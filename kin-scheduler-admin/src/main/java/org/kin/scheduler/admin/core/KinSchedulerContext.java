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
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.driver.Application;
import org.kin.scheduler.core.master.Master;
import org.kin.scheduler.core.master.executor.allocate.AllocateStrategyType;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationListener;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
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
@PropertySource({"classpath:application.yml", "classpath:bootstrap.yml"})
public class KinSchedulerContext implements InitializingBean, ApplicationListener<ContextRefreshedEvent> {
    private static KinSchedulerContext INSTANCE;

    /** 应用名 */
    @Value("${application.name:kin-scheduler}")
    private String appName;
    /** mail用户 */
    @Value("${spring.mail.username}")
    private String emailUserName;
    /** scheduler并发数 */
    @Value("${kin.scheduler.parallism:5}")
    private int parallism;
    /** host */
    @Value("${kin.scheduler.host:0.0.0.0}")
    private String host;
    /** port */
    @Value("${kin.scheduler.port:46668}")
    private int port;
    /** 日志路径 */
    @Value("${kin.scheduler.logPath:/logs}")
    private String logPath;
    /** 通信序列化方式 */
    @Value("${kin.scheduler.serialize:kryo}")
    private String serialize = SerializerType.KRYO.name();
    /** 通信是否支持压缩 */
    @Value("${kin.scheduler.compression:false}")
    private boolean compression;

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
    private RpcEnv rpcEnv;
    private KinDriver driver;

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
            //spring 容器初始化后, 初始化rpc环境和master
            rpcEnv = new RpcEnv(host, port, SysUtils.getSuitableThreadNum(),
                    Serializers.getSerializer(serialize), compression);
            rpcEnv.startServer();

            Config config = new Config();
            config.setMasterHost(host);
            config.setMasterPort(port);
            config.setLogPath(logPath);

            master = new Master(rpcEnv, config);
            master.start();
        }
    }

    @PreDestroy
    public void shutdown() {
        driver.stop();
        master.stop();
        try {
            Thread.sleep(300);
        } catch (InterruptedException e) {

        }
        rpcEnv.stop();
    }

    public KinDriver getDriver() {
        //首次使用driver才初始化driver
        if (Objects.isNull(driver)) {
            synchronized (this) {
                if (Objects.isNull(driver)) {
                    driver = new KinDriver(
                            rpcEnv,
                            Application.build()
                                    .appName(getAppName())
                                    .master(NetUtils.getIpPort(host, port))
                                    .driverPort(port)
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

    //--------------------------------------------------------------------------------------------------------------------------------------------------
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

    public int getParallism() {
        return parallism;
    }

    public void setParallism(int parallism) {
        this.parallism = parallism;
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

    public String getHost() {
        return host;
    }

    public void setHost(String host) {
        this.host = host;
    }

    public int getPort() {
        return port;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public String getLogPath() {
        return logPath;
    }

    public void setLogPath(String logPath) {
        this.logPath = logPath;
    }

    public String getSerialize() {
        return serialize;
    }

    public void setSerialize(String serialize) {
        this.serialize = serialize;
    }

    public boolean isCompression() {
        return compression;
    }

    public void setCompression(boolean compression) {
        this.compression = compression;
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
