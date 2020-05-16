package org.kin.scheduler.admin.core;

import org.kin.scheduler.admin.dao.JobInfoDao;
import org.kin.scheduler.admin.dao.TaskInfoDao;
import org.kin.scheduler.admin.dao.TaskLogDao;
import org.kin.scheduler.admin.service.JobService;
import org.kin.scheduler.core.driver.schedule.impl.SchedulerContextImpl;
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

    @Value("${application.name}")
    private String appName;
    @Value("${spring.mail.username}")
    private String emailUserName;
    @Value("${kin.scheduler.parallism}")
    private int schedulerParallism;
    @Value("${kin.scheduler.masterBackendHost}")
    private String masterBackendHost;
    @Value("${kin.scheduler.masterBackendPort}")
    private int masterBackendPort;
    /** 日志路径 */
    @Value("${kin.scheduler.logPath}")
    private String logPath;

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
            //spring 容器初始化后, 初始化master
            Thread masterThread = new Thread(() -> {
                String masterBackendHost = KinSchedulerContext.instance().getMasterBackendHost();
                int masterBackendPort = KinSchedulerContext.instance().getMasterBackendPort();
                String logPath = KinSchedulerContext.instance().getLogPath();

                master = new Master(masterBackendHost, masterBackendPort, logPath);
                try {
                    master.init();
                    master.start();
                } finally {
                    master.stop();
                }
            }, "Master-Thread");
            masterThread.start();
        }
    }

    @PreDestroy
    public void shutdown() {
        driver.close();
        try {
            Thread.sleep(500);
        } catch (InterruptedException e) {

        }
        master.stop();
    }

    public KinDriver getDriver() {
        //首次使用driver才初始化driver
        if (Objects.isNull(driver)) {
            synchronized (this) {
                if (Objects.isNull(driver)) {
                    driver = new KinDriver(SchedulerContextImpl.build().appName(KinSchedulerContext.instance().getAppName()).allocateStrategy(AllocateStrategyType.All), master);
                    driver.init();
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

    public String getMasterBackendHost() {
        return masterBackendHost;
    }

    public void setMasterBackendHost(String masterBackendHost) {
        this.masterBackendHost = masterBackendHost;
    }

    public int getMasterBackendPort() {
        return masterBackendPort;
    }

    public void setMasterBackendPort(int masterBackendPort) {
        this.masterBackendPort = masterBackendPort;
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
