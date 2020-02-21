package org.kin.scheduler.core.utils;

import org.apache.commons.exec.*;
import org.kin.scheduler.core.task.TaskLoggers;
import org.kin.scheduler.core.task.handler.exception.WorkingDirectoryNotExistsException;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;

/**
 * 1、使用java调用控制台进程方式"Runtime.getRuntime().exec()"来运行脚本(shell或python)
 * 2、因为通过java调用控制台进程方式实现，需要保证目标机器PATH路径正确配置对应编译器
 * 3、脚本打印的日志存储在指定的日志文件上
 * 4、python 异常输出优先级高于标准输出，体现在Log文件中，因此推荐通过logging方式打日志保持和异常信息一致；否则用prinf日志顺序会错乱
 *
 * @author huangjianqin
 * @date 2020-02-19
 */
public class ScriptUtils {

    /**
     * make script file
     */
    public static boolean createScriptFile(String scriptFileName, String content) {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(scriptFileName);
            fileOutputStream.write(content.getBytes("UTF-8"));
            fileOutputStream.close();
            return true;
        } catch (Exception e) {
            TaskLoggers.getLogger().error(e.getMessage(), e);
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    TaskLoggers.getLogger().error(e.getMessage(), e);
                }
            }
        }

        return false;
    }

    /**
     * 异步启动进程
     * 自定义进程标准输出日志文件输出方式
     *
     * @return 返回进程 启动 结果
     */
    public static int execCommand(String command, String logFile, String workingDirectory, String... params) {
        try {
            return execCommand0(command, logFile, workingDirectory, params);
        } catch (Exception e) {
            TaskLoggers.getLogger().error(e.getMessage(), e);
        }
        return -1;
    }

    /**
     * 异步启动进程
     * 自定义进程标准输出日志文件输出方式
     *
     * @return 返回进程 启动 结果
     */
    private static int execCommand0(String command, String logFile, String workingDirectory, String... params) throws Exception {
        File workingDirectoryFile = new File(workingDirectory);
        if (!workingDirectoryFile.exists()) {
            throw new WorkingDirectoryNotExistsException(workingDirectory);
        }
        FileOutputStream fileOutputStream = null;   //
        try {
            fileOutputStream = new FileOutputStream(logFile, true);
            PumpStreamHandler streamHandler = new PumpStreamHandler(fileOutputStream, fileOutputStream, null);

            // command
            CommandLine commandline = new CommandLine(command);
            if (params != null && params.length > 0) {
                commandline.addArguments(params);
            }

            //设置60秒超时，执行超过60秒后会直接终止
//            ExecuteWatchdog watchdog = new ExecuteWatchdog(-1);
//            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            // exec
            DefaultExecutor exec = new DefaultExecutor();
            exec.setExitValues(null);
//            exec.setWatchdog(watchdog);
            exec.setStreamHandler(streamHandler);
            exec.setWorkingDirectory(workingDirectoryFile);
            int exitValue = exec.execute(commandline);  // exit code: 0=success, 1=error
            return exitValue;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    TaskLoggers.getLogger().error(e.getMessage(), e);
                }
            }
        }
    }

    /**
     * 同步启动进程, 等待进程结束并返回结果
     * 自定义进程标准输出日志文件输出方式
     *
     * @return 返回进程 执行 结果
     */
    public static int execCommand(String command, String logFile) {
        try {
            return execCommand0(command, logFile);
        } catch (Exception e) {
            TaskLoggers.getLogger().error(e.getMessage(), e);
        }
        return -1;
    }

    /**
     * 同步启动进程, 等待进程结束并返回结果
     * 自定义进程标准输出日志文件输出方式
     *
     * @return 返回进程 执行 结果
     */
    private static int execCommand0(String command, String logFile) throws Exception {
        FileOutputStream fileOutputStream = null;   //
        try {
            fileOutputStream = new FileOutputStream(logFile, true);
            PumpStreamHandler streamHandler = new PumpStreamHandler(fileOutputStream, fileOutputStream, null);

            // command
            CommandLine commandline = new CommandLine(command);

            //设置60秒超时，执行超过60秒后会直接终止
            ExecuteWatchdog watchdog = new ExecuteWatchdog(-1);
            DefaultExecuteResultHandler resultHandler = new DefaultExecuteResultHandler();
            // exec
            DefaultExecutor exec = new DefaultExecutor();
            exec.setExitValues(null);
            exec.setWatchdog(watchdog);
            exec.setStreamHandler(streamHandler);
            exec.execute(commandline, resultHandler);  // exit code: 0=success, 1=error
            resultHandler.waitFor();
            return resultHandler.getExitValue();
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                    TaskLoggers.getLogger().error(e.getMessage(), e);
                }

            }
        }
    }
}
