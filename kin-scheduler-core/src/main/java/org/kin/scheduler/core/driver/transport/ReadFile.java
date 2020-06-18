package org.kin.scheduler.core.driver.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-16
 */
public class ReadFile implements Serializable {
    private String workerId;
    private String path;
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
