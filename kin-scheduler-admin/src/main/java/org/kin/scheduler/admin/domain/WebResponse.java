package org.kin.scheduler.admin.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public class WebResponse<T> implements Serializable {
    public static final long serialVersionUID = 42L;

    private int code;
    private String msg;
    private T content;

    public WebResponse() {
    }

    public WebResponse(int code, String msg, T content) {
        this.code = code;
        this.msg = msg;
        this.content = content;
    }

    public static <T> WebResponse<T> success(T content) {
        return success("", content);
    }

    public static <T> WebResponse<T> success() {
        return success("", null);
    }

    public static <T> WebResponse<T> success(String msg, T content) {
        return new WebResponse<>(Constants.SUCCESS_CODE, msg, content);
    }

    public static <T> WebResponse<T> fail(String msg) {
        return new WebResponse<>(Constants.FAIL_CODE, msg, null);
    }

    //------------------------------------------------------------------------------------------------------

    public int getCode() {
        return code;
    }

    public void setCode(int code) {
        this.code = code;
    }

    public String getMsg() {
        return msg;
    }

    public void setMsg(String msg) {
        this.msg = msg;
    }

    public T getContent() {
        return content;
    }

    public void setContent(T content) {
        this.content = content;
    }

    @Override
    public String toString() {
        return "WebResult [code=" + code + ", msg=" + msg + ", content=" + content + "]";
    }

}