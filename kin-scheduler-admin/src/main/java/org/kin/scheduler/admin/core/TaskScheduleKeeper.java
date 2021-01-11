package org.kin.scheduler.admin.core;

import org.kin.framework.concurrent.TimeRing;
import org.kin.framework.concurrent.keeper.Keeper;
import org.kin.framework.concurrent.partition.EfficientHashPartitioner;
import org.kin.framework.concurrent.partition.PartitionTaskExecutor;
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
import java.util.Objects;
import java.util.concurrent.TimeUnit;

/**
 * 调度task工具类
 *
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

    /** 分区线程池, 用于按task id分区提交task */
    private PartitionTaskExecutor<Integer> triggerThreads;
    private Keeper.KeeperStopper scheduleKeeper;
    /** 2s时间间隔的时间环, 模拟时间行走, 不太实时, 但是性能相对较好 */
    private TimeRing<Integer> timeRing;
    /** 数据库连接 */
    private Connection conn = null;

    /**
     * 初始化
     */
    private void init() {
        triggerThreads =
                new PartitionTaskExecutor<>(
                        KinSchedulerContext.instance().getParallism(),
                        EfficientHashPartitioner.INSTANCE,
                        "admin-schedule-");
        scheduleKeeper = Keeper.keep(this::schedule);
        timeRing = TimeRing.second(2, this::trigger);
        timeRing.start();
    }

    /**
     * 提交分区线程池提交task
     *
     * @param taskId task id
     */
    private void trigger(int taskId) {
        triggerThreads.execute(taskId, () -> TaskTrigger.instance().trigger(taskId));
    }

    /**
     * 提交分区线程池提交task
     *
     * @param taskInfo task信息
     */
    private void trigger(TaskInfo taskInfo) {
        triggerThreads.execute(taskInfo.getId(), () -> TaskTrigger.instance().trigger(taskInfo));
    }

    /**
     * 间隔调度扫描未来 PRE_READ_MS 毫秒内(恰好一个调度间隔)的task, 并push进时间环内, 调度提交task
     */
    private void schedule() {
        try {
            TimeUnit.MILLISECONDS.sleep(PRE_READ_MS - System.currentTimeMillis() % 1000);
        } catch (InterruptedException e) {
            return;
        }

        // 扫描任务
        long start = System.currentTimeMillis();
        PreparedStatement preparedStatement = null;
        try {
            if (conn != null) {
                conn.close();
            }
            conn = KinSchedulerContext.instance().getDataSource().getConnection();
            conn.setAutoCommit(false);

            preparedStatement = conn.prepareStatement("select * from task_lock where lock_name = 'schedule_lock' for update");
            preparedStatement.execute();

            //事务开始, 所有其他操作必须等事务commit or rollback
            //预读 PRE_READ_MS 毫秒内调度任务
            TaskInfoDao taskInfoDao = KinSchedulerContext.instance().getTaskInfoDao();
            long nowTime = System.currentTimeMillis();
            List<TaskInfo> waittingTaskInfos = taskInfoDao.mapper().scheduleTaskQuery(nowTime + PRE_READ_MS);
            if (CollectionUtils.isNonEmpty(waittingTaskInfos)) {
                for (TaskInfo taskInfo : waittingTaskInfos) {
                    if (nowTime > taskInfo.getTriggerNextTime() + PRE_READ_MS) {
                        // 过期超5s：本地忽略，当前时间开始计算下次触发时间

                        // 刷新下次触发时间
                        taskInfo.setTriggerLastTime(taskInfo.getTriggerNextTime());
                        try {
                            taskInfo.setTriggerNextTime(TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr()));
                        } catch (Exception e) {
                            log.error("schedule task(jobId={}, taskId={}) fail, due to >>>>>", taskInfo.getJobId(), taskInfo.getId(), e);
                            taskInfo.end();
                        }
                    } else if (nowTime > taskInfo.getTriggerNextTime()) {
                        // 过期5s内 ：立即触发一次，当前时间开始计算下次触发时间；

                        long nextTime;
                        try {
                            nextTime = TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr());
                        } catch (Exception e) {
                            log.error("schedule task(jobId={}, taskId={}) fail, due to >>>>>", taskInfo.getJobId(), taskInfo.getId(), e);
                            taskInfo.end();
                            continue;
                        }

                        // 提交task
                        trigger(taskInfo);

                        // fresh next, 刷新下次触发时间
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

                // 更新trigger信息
                for (TaskInfo taskInfo : waittingTaskInfos) {
                    taskInfoDao.mapper().scheduleUpdate(taskInfo);
                }

            }

            //事务结束
            conn.commit();
        } catch (Exception e) {
            log.error("调度异常 >>>>", e);
            try {
                conn.rollback();
            } catch (SQLException ex) {
                log.error("调度时回滚异常 >>>>", e);
            }
        } finally {
            if (null != preparedStatement) {
                try {
                    preparedStatement.close();
                } catch (SQLException e) {
                    log.error("数据库连接关闭异常 >>>>", e);
                }
            }
        }
        long cost = System.currentTimeMillis() - start;

        // next second, align second
        int oneSecondMillis = 1000;
        try {
            if (cost < oneSecondMillis) {
                TimeUnit.MILLISECONDS.sleep(oneSecondMillis - System.currentTimeMillis() % oneSecondMillis);
            }
        } catch (InterruptedException e) {

        }

        if (conn != null) {
            try {
                conn.close();
            } catch (SQLException e) {
                log.error("数据库连接关闭异常 >>>>", e);
            }
        }
    }

    /**
     * 把符合条件的task推进时间环, 等待时间行走到触发时间, 进而触发任务, 提交task
     *
     * @param taskInfo task信息
     */
    private void pushRing(TaskInfo taskInfo) {
        // 推进时间环
        timeRing.push(taskInfo.getTriggerNextTime(), taskInfo.getId());

        // 刷新下次触发时间
        taskInfo.setTriggerLastTime(taskInfo.getTriggerNextTime());
        try {
            taskInfo.setTriggerNextTime(TimeType.getByName(taskInfo.getTimeType()).parseTime(taskInfo.getTimeStr()));
        } catch (Exception e) {
            log.error("schedule task(jobId={}, taskId={}) fail, due to >>>>>", taskInfo.getJobId(), taskInfo.getId(), e);
            taskInfo.end();
        }
    }

    /**
     * 关闭并释放资源
     */
    public void stop() {
        scheduleKeeper.stop();
        timeRing.stop();
        triggerThreads.shutdown();
    }
}
