package org.kin.scheduler.admin.core;

import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.SysUtils;
import org.kin.kinrpc.message.core.RpcEnv;
import org.kin.kinrpc.transport.serializer.SerializerType;
import org.kin.kinrpc.transport.serializer.Serializers;
import org.kin.scheduler.admin.dao.JobInfoDao;
import org.kin.scheduler.admin.dao.TaskInfoDao;
import org.kin.scheduler.admin.dao.TaskLogDao;
import org.kin.scheduler.admin.service.JobService;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.master.Master;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.mail.javamail.JavaMailSender;

import javax.annotation.PreDestroy;
import javax.sql.DataSource;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
@Configuration
public class KinSchedulerContext implements InitializingBean, ApplicationListener<ContextRefreshedEvent> {
    private static KinSchedulerContext INSTANCE;

    /** 应用名 */
    @Value("${application.name}")
    private String appName;
    /** mail用户 */
    @Value("${spring.mail.username}")
    private String emailUserName;
    /** scheduler并发 */
    @Value("${kin.scheduler.parallism}")
    private int schedulerParallism;
    /** master host */
    @Value("${kin.scheduler.masterHost}")
    private String masterHost;
    /** master port */
    @Value("${kin.scheduler.masterPort}")
    private int masterPort;
    /** 日志路径 */
    @Value("${kin.scheduler.logPath}")
    private String logPath;
    /** driver绑定端口 */
    @Value("${kin.scheduler.driverPort}")
    private int driverPort;

    @Autowired
    private TaskInfoDao taskInfoDao;
    @Autowired
    private JobService jobService;
    @Autowired
    private DataSource dataSource;
    @Autowired
    private TaskLogDao taskLogDao;
    @Autowired
    private JavaMailSender mailSender;
    @Autowired
    private JobInfoDao jobInfoDao;

    private Master master;
    private RpcEnv masterRpcEnv;
    private KinDriver driver;
    private RpcEnv driverRpcEnv;

    public static KinSchedulerContext instance() {
        return INSTANCE;
    }

    @Override
    public void afterPropertiesSet() {
        INSTANCE = this;
    }

    @Override
    public void onApplicationEvent(ContextRefreshedEvent refreshedEvent) {
        if (refreshedEvent.getApplicationContext().getParent() == null) {
            //spring 容器初始化后, 初始化master
            Thread masterThread = new Thread(() -> {
                String masterHost = KinSchedulerContext.instance().getMasterHost();
                int masterPort = KinSchedulerContext.instance().getMasterPort();
                String logPath = KinSchedulerContext.instance().getLogPath();

                masterRpcEnv = new RpcEnv(masterHost, masterPort, SysUtils.getSuitableThreadNum(),
                        Serializers.getSerializer(SerializerType.KRYO), false);
                masterRpcEnv.startServer();

                //TODO 定心跳间隔
                master = new Master(masterRpcEnv, logPath, 3);
                try {
                    master.start();
                } finally {
                    master.stop();
                    masterRpcEnv.stop();
                }
            }, "Master-Thread");
            masterThread.start();
        }
    }

    @PreDestroy
    public void shutdown() {
        driver.stop();
        driverRpcEnv.stop();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }
        master.stop();
        masterRpcEnv.stop();
    }

    public KinDriver getDriver() {
        //首次使用driver才初始化driver
        if (Objects.isNull(driver)) {
            synchronized (this) {
                if (Objects.isNull(driver)) {
                    driverRpcEnv = new RpcEnv(NetUtils.getIp(), driverPort, SysUtils.getSuitableThreadNum(),
                            Serializers.getSerializer(SerializerType.KRYO), false);
                    driverRpcEnv.startServer();
                    driver = new KinDriver(
                            driverRpcEnv,
                            Application.build()
                                    .appName(getAppName())
                                    .master(NetUtils.getIpPort(masterHost, masterPort))
                                    .driverPort(driverPort)
                                    .cpuCore(Integer.MAX_VALUE)
                                    .allocateStrategy(AllocateStrategyType.All)
                                    .oneExecutorPerWorker()
                                    .dropResult()
                    );
                    driver.start();
                }
            }
        }
        return driver;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getEmailUserName() {
        return emailUserName;
    }

    public void setEmailUserName(String emailUserName) {
        this.emailUserName = emailUserName;
    }

    public int getSchedulerParallism() {
        return schedulerParallism;
    }

    public void setSchedulerParallism(int schedulerParallism) {
        this.schedulerParallism = schedulerParallism;
    }

    public TaskInfoDao getTaskInfoDao() {
        return taskInfoDao;
    }

    public void setTaskInfoDao(TaskInfoDao taskInfoDao) {
        this.taskInfoDao = taskInfoDao;
    }

    public TaskLogDao getTaskLogDao() {
        return taskLogDao;
    }

    public void setTaskLogDao(TaskLogDao taskLogDao) {
        this.taskLogDao = taskLogDao;
    }

    public JobService getJobService() {
        return jobService;
    }

    public void setJobService(JobService jobService) {
        this.jobService = jobService;
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public String getMasterHost() {
        return masterHost;
    }

    public void setMasterHost(String masterHost) {
        this.masterHost = masterHost;
    }

    public int getMasterPort() {
        return masterPort;
    }

    public void setMasterPort(int masterPort) {
        this.masterPort = masterPort;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public JavaMailSender getMailSender() {
        return mailSender;
    }

    public void setMailSender(JavaMailSender mailSender) {
        this.mailSender = mailSender;
    }

    public JobInfoDao getJobInfoDao() {
        return jobInfoDao;
    }

    public void setJobInfoDao(JobInfoDao jobInfoDao) {
        this.jobInfoDao = jobInfoDao;
    }
}
