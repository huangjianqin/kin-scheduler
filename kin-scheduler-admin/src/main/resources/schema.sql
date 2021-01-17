DROP TABLE IF EXISTS `user`;
DROP TABLE IF EXISTS `job_info`;
DROP TABLE IF EXISTS `task_info`;
DROP TABLE IF EXISTS `task_log`;
DROP TABLE IF EXISTS `task_lock`;

CREATE TABLE `user` (
    `id`  int(11) NOT NULL auto_increment COMMENT 'id',
    `account` VARCHAR(100) NOT NULL COMMENT '账号',
    `password`    VARCHAR(100) NOT NULL COMMENT '密码',
    `role`    tinyint NOT NULL COMMENT '0-普通用户、1-管理员',
    `name`    VARCHAR(150) NOT NULL COMMENT '用户名',
    PRIMARY KEY (`id`)
)   ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE `job_info` (
    `id`  int(11) NOT NULL auto_increment COMMENT 'id',
    `app_name`    VARCHAR(200) NOT NULL COMMENT 'job应用名',
    `title`   VARCHAR(200) NOT NULL COMMENT 'job标题',
    `order`   tinyint COMMENT 'job优先级',
    PRIMARY KEY (`id`)
)   ENGINE = InnoDB   DEFAULT CHARSET = utf8;

CREATE TABLE `task_info` (
    `id`  int(11) NOT NULL auto_increment COMMENT 'id',
    `job_id`  int(11) NOT NULL COMMENT '所属作业',
    `time_type`   VARCHAR(50) NOT NULL COMMENT '时间类型',
    `time_str`    VARCHAR(50) NOT NULL COMMENT '时间表达式',
    `desc`    text NOT NULL COMMENT '任务描述',
    `add_time`  datetime    COMMENT '任务添加时间',
    `update_time` datetime  COMMENT '任务更新时间',
    `user_id`    int(11) NOT NULL COMMENT '所属用户id',
    `route_strategy`    VARCHAR(50) NOT NULL COMMENT 'executor路由策略',
    `type`    VARCHAR(50) NOT NULL COMMENT '任务类型',
    `param`  tinytext NOT NULL COMMENT '任务参数(json)',
    `exec_strategy` VARCHAR(50) NOT NULL COMMENT '任务执行策略',
    `exec_timeout`    tinyint NOT NULL DEFAULT '0' COMMENT '任务执行超时',
    `retry_times` tinyint NOT NULL DEFAULT '0'  COMMENT '任务执行重试次数',
    `alarm_email` VARCHAR(200) DEFAULT '' COMMENT '警告邮件(可多个, 逗号分隔)',
    `child_task_ids`  VARCHAR(300)  DEFAULT '' COMMENT '子任务id(可多个, 逗号分隔)',
    `trigger_status`  tinyint NOT NULL DEFAULT '0' COMMENT '调度状态',
    `trigger_last_time`  bigint(13)  NOT NULL DEFAULT '0' COMMENT '上次调度时间',
    `trigger_next_time`   bigint(13) NOT NULL DEFAULT '0' COMMENT '下次调度时间',
    PRIMARY KEY (`id`)
)   ENGINE = InnoDB DEFAULT CHARSET = utf8;

CREATE TABLE `task_log`
(
    `id`                int(11)      NOT NULL auto_increment COMMENT 'id',
    `task_id`           int(11)      NOT NULL COMMENT '所属任务',
    `job_id`            int(11)      NOT NULL COMMENT '所属作业',
    `desc`              text         NOT NULL COMMENT '任务描述',
    `executor_address`  VARCHAR(200)          DEFAULT '' COMMENT 'executor地址',
    `route_strategy`    VARCHAR(50)  NOT NULL COMMENT 'executor路由策略',
    `type`              VARCHAR(50)  NOT NULL COMMENT '任务类型',
    `param`             tinytext     NOT NULL COMMENT '任务参数(json)',
    `exec_strategy`     VARCHAR(50)  NOT NULL COMMENT '任务执行策略',
    `exec_timeout`      tinyint      NOT NULL DEFAULT '0' COMMENT '任务执行超时',
    `retry_times`       tinyint      NOT NULL DEFAULT '0' COMMENT '任务当前执行重试次数',
    `retry_times_limit` tinyint      NOT NULL DEFAULT '0' COMMENT '任务执行重试次数上限',
    `trigger_time`      datetime COMMENT '任务调度时间',
    `triggr_code`       int          NULL COMMENT '任务调度结果',
    `handle_time`       datetime COMMENT '任务处理时间',
    `handle_code`       int          NULL COMMENT '任务处理结果',
    `log_path`          VARCHAR(400) NULL COMMENT '任务日志路径',
    `output_path`       VARCHAR(400) NULL COMMENT '任务输出文件',
    PRIMARY KEY (`id`)
) ENGINE = InnoDB
  DEFAULT CHARSET = utf8;

CREATE TABLE `task_lock` (
    `lock_name` VARCHAR(50) NOT NULL COMMENT '锁名称',
    PRIMARY KEY(`lock_name`)
)   ENGINE = InnoDB DEFAULT CHARSET = utf8;