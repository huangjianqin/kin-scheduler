package org.kin.scheduler.admin.core;

import org.kin.framework.concurrent.TimeRing;
import org.kin.framework.concurrent.keeper.Keeper;
import org.kin.framework.concurrent.partition.PartitionTaskExecutor;
import org.kin.framework.concurrent.partition.partitioner.impl.EfficientHashPartitioner;
import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.admin.dao.TaskInfoDao;
import org.kin.scheduler.admin.domain.TimeType;
import org.kin.scheduler.admin.entity.TaskInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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
    private TimeRing<Integer> timeRing;
    private Map<Integer, List<Integer>> ringData = new ConcurrentHashMap<>();
    private Connection conn = null;

    private void init() {
        triggerThreads =
                new PartitionTaskExecutor(
                        KinSchedulerContext.instance().getSchedulerParallism(),
                        EfficientHashPartitioner.INSTANCE,
                        "admin-scheduler-thread-");
        scheduleKeeper = Keeper.keep(this::schedule);
        timeRing = TimeRing.second(2, this::trigger);
        timeRing.start();
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
        // 2、push time ring
        timeRing.push(taskInfo.getTriggerNextTime(), taskInfo.getId());

        // 3、fresh next
        taskInfo.setTriggerLastTime(taskInfo.getTriggerNextTime());
        try {
            taskInfo.setTriggerNextTime(TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr()));
        } catch (Exception e) {
            taskInfo.end();
        }
    }

    public void stop() {
        scheduleKeeper.stop();
        timeRing.stop();
        triggerThreads.shutdown();
    }
}
