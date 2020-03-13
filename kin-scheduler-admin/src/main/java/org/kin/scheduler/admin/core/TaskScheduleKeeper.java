package org.kin.scheduler.admin.core;

import org.kin.framework.concurrent.Keeper;
import org.kin.framework.concurrent.PartitionTaskExecutor;
import org.kin.framework.concurrent.SimpleThreadFactory;
import org.kin.framework.concurrent.impl.EfficientHashPartitioner;
import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.admin.dao.TaskInfoDao;
import org.kin.scheduler.admin.domain.TimeType;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
public class TaskScheduleKeeper {
    private static Logger log = LoggerFactory.getLogger(TaskScheduleKeeper.class);
    private static TaskScheduleKeeper INSTANCE;
    public static final long PRE_READ_MS = 5000;

    public static TaskScheduleKeeper instance() {
        if (Objects.isNull(INSTANCE)) {
            synchronized (TaskScheduleKeeper.class) {
                if (Objects.isNull(INSTANCE)) {
                    INSTANCE = new TaskScheduleKeeper();
                    INSTANCE.init();
                }
            }
        }

        return INSTANCE;
    }

    private PartitionTaskExecutor<Integer> triggerThreads;
    private Keeper.KeeperStopper scheduleKeeper;
    private Keeper.KeeperStopper ringScheduleKeeper;
    private Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();
    private Connection conn = null;

    private void init() {
        triggerThreads =
                new PartitionTaskExecutor(
                        KinSchedulerContext.instance().getSchedulerParallism(),
                        EfficientHashPartitioner.INSTANCE,
                        new SimpleThreadFactory("admin-scheduler-thread-"));
        scheduleKeeper = Keeper.keep(this::schedule);
        ringScheduleKeeper = Keeper.keep(this::ringSchedule);
    }

    public void trigger(int taskId) {
        triggerThreads.execute(taskId, () -> {
            TaskTrigger.instance().trigger(taskId);
        });
    }

    public void trigger(TaskInfo taskInfo) {
        triggerThreads.execute(taskInfo.getId(), () -> {
            TaskTrigger.instance().trigger(taskInfo);
        });
    }

    public void schedule() {
        try {
            TimeUnit.MILLISECONDS.sleep(PRE_READ_MS - System.currentTimeMillis() % 1000);
        } catch (InterruptedException e) {
        }

        // 扫描任务
        long start = System.currentTimeMillis();
        PreparedStatement preparedStatement = null;
        try {
            if (conn != null) {
                conn.isClosed();
            }
            conn = KinSchedulerContext.instance().getDataSource().getConnection();
            conn.setAutoCommit(false);

            preparedStatement = conn.prepareStatement("select * from task_lock where lock_name = 'schedule_lock' for update");
            preparedStatement.execute();

            //事务开始, 所有其他操作必须等事务commit or rollback
            // 1、预读5s内调度任务
            TaskInfoDao taskInfoDao = KinSchedulerContext.instance().getTaskInfoDao();
            long nowTime = System.currentTimeMillis();
            List<TaskInfo> waittingTaskInfos = taskInfoDao.scheduleTaskQuery(nowTime + PRE_READ_MS);
            if (CollectionUtils.isNonEmpty(waittingTaskInfos)) {
                // 2、推送时间轮
                for (TaskInfo taskInfo : waittingTaskInfos) {

                    // 时间轮刻度计算
                    if (nowTime > taskInfo.getTriggerNextTime() + PRE_READ_MS) {
                        // 过期超5s：本地忽略，当前时间开始计算下次触发时间

                        // fresh next
                        taskInfo.setTriggerLastTime(taskInfo.getTriggerNextTime());
                        try {
                            taskInfo.setTriggerNextTime(TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr()));
                        } catch (Exception e) {
                            taskInfo.end();
                        }
                    } else if (nowTime > taskInfo.getTriggerNextTime()) {
                        // 过期5s内 ：立即触发一次，当前时间开始计算下次触发时间；

                        long nextTime;
                        try {
                            nextTime = TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr());
                        } catch (Exception e) {
                            taskInfo.end();
                            continue;
                        }

                        // 1、trigger
                        trigger(taskInfo);

                        // 2、fresh next
                        taskInfo.setTriggerLastTime(taskInfo.getTriggerNextTime());
                        taskInfo.setTriggerNextTime(nextTime);


                        // 下次5s内：预读一次；
                        if (taskInfo.getTriggerNextTime() - nowTime < PRE_READ_MS) {
                            pushRing(taskInfo);
                        }

                    } else {
                        // 未过期：正常触发，递增计算下次触发时间
                        pushRing(taskInfo);
                    }

                }

                // 3、更新trigger信息
                for (TaskInfo taskInfo : waittingTaskInfos) {
                    taskInfoDao.scheduleUpdate(taskInfo);
                }

            }

            //事务结束
            conn.commit();
        } catch (Exception e) {
            log.error("调度时出现异常 >>>>", e);
        } finally {
            if (null != preparedStatement) {
                try {
                    preparedStatement.close();
                } catch (SQLException ignore) {
                }
            }
        }
        long cost = System.currentTimeMillis() - start;

        // next second, align second
        try {
            if (cost < 1000) {
                TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
            }
        } catch (InterruptedException e) {

        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
            }
        }
    }

    private void pushRing(TaskInfo taskInfo) {
        // 1、make ring second
        int ringSecond = (int) ((taskInfo.getTriggerNextTime() / 1000) % 60);

        // 2、push time ring
        pushTimeRing(ringSecond, taskInfo.getId());

        // 3、fresh next
        taskInfo.setTriggerLastTime(taskInfo.getTriggerNextTime());
        try {
            taskInfo.setTriggerNextTime(TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr()));
        } catch (Exception e) {
            taskInfo.end();
        }
    }

    private void pushTimeRing(int ringSecond, int jobId) {
        // push async ring
        List<Integer> ringItemData = ringData.computeIfAbsent(ringSecond, k -> new ArrayList<>());
        ringItemData.add(jobId);
    }

    public void ringSchedule() {
        // align second
        try {
            TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
        } catch (InterruptedException e) {

        }

        try {
            // second data
            List<Integer> ringItemData = new ArrayList<>();
            int nowSecond = Calendar.getInstance().get(Calendar.SECOND);   // 避免处理耗时太长，跨过刻度，向前校验一个刻度；
            for (int i = 0; i < 2; i++) {
                List<Integer> tmpData = ringData.remove((nowSecond + 60 - i) % 60);
                if (tmpData != null) {
                    ringItemData.addAll(tmpData);
                }
            }

            // ring trigger
            if (CollectionUtils.isNonEmpty(ringItemData)) {
                // do trigger
                for (int jobId : ringItemData) {
                    // do trigger
                    trigger(jobId);
                }
                // clear
                ringItemData.clear();
            }
        } catch (Exception e) {
            log.error("时钟调度遇到异常 >>>> ", e);
        }

        // next second, align second
        try {
            TimeUnit.MILLISECONDS.sleep(1000 - System.currentTimeMillis() % 1000);
        } catch (InterruptedException e) {
        }

    }

    public void stop() {
        scheduleKeeper.stop();
        ringScheduleKeeper.stop();
        triggerThreads.shutdown();
    }
}
