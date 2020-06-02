package org.kin.scheduler.core.worker.transport;

import org.kin.scheduler.core.transport.RPCResult;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-03-08
 */
public class TaskExecFileContent extends RPCResult implements Serializable {
    private static final long serialVersionUID = 42L;

    public static TaskExecFileContent success(String workerId, String path, int fromLineNum, int toLineNum, String content, boolean isEnd) {
        TaskExecFileContent result = new TaskExecFileContent();
        result.setSuccess(true);
        result.setDesc("");
        result.setWorkerId(workerId);
        result.setPath(path);
        result.setFromLineNum(fromLineNum);
        result.setToLineNum(toLineNum);
        result.setContent(content);
        result.setEnd(isEnd);
        return result;
    }

    public static TaskExecFileContent fail(String workerId, String path, int fromLineNum, String desc) {
        TaskExecFileContent content = new TaskExecFileContent();
        content.setSuccess(false);
        content.setDesc(desc);
        content.setWorkerId(workerId);
        content.setPath(path);
        content.setFromLineNum(fromLineNum);
        content.setContent(desc);
        return content;
    }

    /** 文件所在的worker id */
    private String workerId;
    /** 文件路径 */
    private String path;
    /** 开始行数 */
    private int fromLineNum;
    /** 读到的行数 */
    private int toLineNum;
    /** 内容 */
    private String content;
    /** 是否文件尾 */
    private boolean isEnd;

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

    public int getToLineNum() {
        return toLineNum;
    }

    public void setToLineNum(int toLineNum) {
        this.toLineNum = toLineNum;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public boolean isEnd() {
        return isEnd;
    }

    public void setEnd(boolean end) {
        isEnd = end;
    }
}
