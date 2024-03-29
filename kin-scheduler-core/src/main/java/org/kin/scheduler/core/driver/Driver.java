package org.kin.scheduler.core.driver;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.framework.utils.ExtensionLoader;
import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEndpointRef;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.serialization.Serialization;
import org.kin.scheduler.core.driver.exception.RegisterApplicationFailureException;
import org.kin.scheduler.core.driver.scheduler.TaskContext;
import org.kin.scheduler.core.driver.scheduler.TaskExecFuture;
import org.kin.scheduler.core.driver.scheduler.TaskScheduler;
import org.kin.scheduler.core.driver.scheduler.impl.DefaultTaskScheduler;
import org.kin.scheduler.core.driver.transport.ApplicationEnd;
import org.kin.scheduler.core.driver.transport.ReadFile;
import org.kin.scheduler.core.driver.transport.RegisterApplication;
import org.kin.scheduler.core.master.Master;
import org.kin.scheduler.core.master.transport.RegisterApplicationResp;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.worker.transport.TaskExecFileContent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.Serializable;
import java.util.Objects;
import java.util.concurrent.ExecutionException;

/**
 * application driver 负责创建taskscheduler, 对外提供调用接口
 *
 * @author huangjianqin
 * @date 2020-02-09
 */
public class Driver {
    private static final Logger log = LoggerFactory.getLogger(Driver.class);
    /** rpc环境 */
    protected final RpcEnv rpcEnv;
    /** master client */
    protected final RpcEndpointRef masterRef;
    /** application配置 */
    protected Application app;
    /** TaskScheduler实现 */
    protected TaskScheduler taskScheduler;
    protected volatile boolean isStopped;

    //---------------------------------------------------------------------------------------------------------------------
    public static Driver common(Application app) {
        RpcEnv rpcEnv = new RpcEnv("0.0.0.0", app.getDriverPort(), SysUtils.CPU_NUM,
                ExtensionLoader.getExtension(Serialization.class, app.getSerializationCode()), app.getCompressionType());
        rpcEnv.startServer();
        return new Driver(rpcEnv, app, new DefaultTaskScheduler(rpcEnv, app));
    }

    //---------------------------------------------------------------------------------------------------------------------
    public Driver(RpcEnv rpcEnv, Application app, TaskScheduler taskScheduler) {
        this.rpcEnv = rpcEnv;
        //master client ref
        Object[] masterHostPort = NetUtils.parseIpPort(app.getMasterAddress());
        this.masterRef = rpcEnv.createEndpointRef(masterHostPort[0].toString(), (Integer) masterHostPort[1], Master.DEFAULT_NAME);
        this.app = app;
        this.taskScheduler = taskScheduler;
    }

    public void start() {
        if (isStopped) {
            return;
        }

        //启动taskscheduler
        taskScheduler.start();

        JvmCloseCleaner.instance().add(JvmCloseCleaner.MAX_PRIORITY, this::stop);

        //注册application
        try {
            ApplicationDescription appDesc = new ApplicationDescription();
            appDesc.setAppName(app.getAppName());
            appDesc.setAllocateStrategyType(app.getAllocateStrategyType());
            appDesc.setCpuCoreNum(app.getCpuCoreNum());
            appDesc.setMinCoresPerExecutor(app.getMinCoresPerExecutor());
            appDesc.setOneExecutorPerWorker(app.isOneExecutorPerWorker());

            RegisterApplicationResp registerApplicationResp = (RegisterApplicationResp) masterRef.requestResponse(RegisterApplication.of(appDesc, taskScheduler.ref())).get();
            if (!registerApplicationResp.isSuccess()) {
                throw new RegisterApplicationFailureException(registerApplicationResp.getDesc());
            }
        } catch (Exception e) {
            throw new RegisterApplicationFailureException(e.getMessage());
        }
        log.info("driver(appName={}, master={}) started on {}", app.getAppName(), app.getMasterAddress(), rpcEnv.address());
    }

    public void stop() {
        if (isStopped) {
            return;
        }

        //先通知master 应用stop
        masterRef.fireAndForget(ApplicationEnd.of(app.getAppName()));
        isStopped = true;
        //再shutdown executor
        //防止无用executor分配, 如果先shutdown executor再通知master 应用stop, master存在再次为该应用分配资源的可能
        if (Objects.nonNull(taskScheduler)) {
            taskScheduler.stop();
        }

        TaskExecFuture.CALLBACK_EXECUTORS.shutdown();
        log.info("driver(appName={}, master={}) closed", app.getAppName(), app.getMasterAddress());
    }

    //----------------------------------------------------------------------------------------------------------------------------

    /**
     * 等待task完成
     */
    public final void awaitTermination() {
        taskScheduler.awaitTermination();
    }

    /**
     * call线程
     * 向master请求某worker上的log文件
     *
     * @param taskId task id
     * @return log info
     */
    public final TaskExecFileContent readLog(String taskId, int fromLineNum) {
        TaskContext taskContext = taskScheduler.getTaskInfo(taskId);
        if (Objects.isNull(taskContext)) {
            throw new IllegalStateException(String.format("unknown task(taskid='%s')", taskId));
        }

        return readFile(taskContext.getWorkerId(), taskContext.getLogPath(), fromLineNum);
    }

    /**
     * call线程
     * 向master请求某worker上的output文件
     *
     * @param taskId task id
     * @return output info
     */
    public final TaskExecFileContent readOutput(String taskId, int fromLineNum) {
        TaskContext taskContext = taskScheduler.getTaskInfo(taskId);
        if (Objects.isNull(taskContext)) {
            throw new IllegalStateException(String.format("unknown task(taskid='%s')", taskId));
        }

        return readFile(taskContext.getWorkerId(), taskContext.getOutputPath(), fromLineNum);
    }

    /**
     * call线程
     * 从worker上读取文件
     *
     * @param workerId    worker id
     * @param path        log path
     * @param fromLineNum 开始的行数
     */
    protected final TaskExecFileContent readFile(String workerId, String path, int fromLineNum) {
        try {
            return (TaskExecFileContent) masterRef.requestResponse(ReadFile.of(workerId, path, fromLineNum)).get();
        } catch (InterruptedException e) {
            return TaskExecFileContent.fail(workerId, path, fromLineNum, "thread interrupted");
        } catch (ExecutionException e) {
            return TaskExecFileContent.fail(workerId, path, fromLineNum, ExceptionUtils.getExceptionDesc(e));
        }
    }

    /**
     * 提交task
     * call线程
     */
    public final <R extends Serializable, PARAM extends Serializable> TaskExecFuture<R> submitTask(TaskDescription<PARAM> taskDescription) {
        return taskScheduler.submitTask(taskDescription);
    }

    /**
     * 取消task
     * call线程
     */
    public final boolean cancelTask(String taskId) {
        return taskScheduler.cancelTask(taskId);
    }

    //----------------------------------------------------------------------------------------------------------------------------

    public RpcEnv rpcEnv() {
        return rpcEnv;
    }
}
