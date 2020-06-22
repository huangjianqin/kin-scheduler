package org.kin.scheduler.core.driver.transport;

import java.io.Serializable;

/**
 * 读取worker上文件的消息
 *
 * @author huangjianqin
 * @date 2020-06-16
 */
public class ReadFile implements Serializable {
    /** worker id */
    private String workerId;
    /** 路径 */
    private String path;
    /** 文件开始行数 */
    private int fromLineNum;

    public static ReadFile of(String workerId, String path, int fromLineNum) {
        ReadFile message = new ReadFile();
        message.workerId = workerId;
        message.path = path;
        message.fromLineNum = fromLineNum;
        return message;
    }

    public String getWorkerId() {
        return workerId;
    }

    public void setWorkerId(String workerId) {
        this.workerId = workerId;
    }

    public String getPath() {
        return path;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public int getFromLineNum() {
        return fromLineNum;
    }

    public void setFromLineNum(int fromLineNum) {
        this.fromLineNum = fromLineNum;
    }
}
