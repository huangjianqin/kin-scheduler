package org.kin.scheduler.core.utils;

import org.apache.commons.exec.CommandLine;
import org.apache.commons.exec.DefaultExecutor;
import org.apache.commons.exec.PumpStreamHandler;

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
    public static void markScriptFile(String scriptFileName, String content) throws IOException {
        FileOutputStream fileOutputStream = null;
        try {
            fileOutputStream = new FileOutputStream(scriptFileName);
            fileOutputStream.write(content.getBytes("UTF-8"));
            fileOutputStream.close();
        } catch (Exception e) {
            throw e;
        }finally{
            if(fileOutputStream != null){
                fileOutputStream.close();
            }
        }
    }

    /**
     * 日志文件输出方式
     */
    public static int execCommand(String command, String logFile, String... params) throws IOException {
        FileOutputStream fileOutputStream = null;   //
        try {
            fileOutputStream = new FileOutputStream(logFile, true);
            PumpStreamHandler streamHandler = new PumpStreamHandler(fileOutputStream, fileOutputStream, null);

            // command
            CommandLine commandline = new CommandLine(command);
            if (params!=null && params.length>0) {
                commandline.addArguments(params);
            }

            // exec
            DefaultExecutor exec = new DefaultExecutor();
            exec.setExitValues(null);
            exec.setStreamHandler(streamHandler);
            //TODO
//            exec.setWorkingDirectory();
            int exitValue = exec.execute(commandline);  // exit code: 0=success, 1=error
            return exitValue;
        } catch (Exception e) {

            return -1;
        } finally {
            if (fileOutputStream != null) {
                try {
                    fileOutputStream.close();
                } catch (IOException e) {
                }

            }
        }
    }
}
