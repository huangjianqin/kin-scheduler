package org.kin.scheduler.core.driver.exception;

/**
 * @author huangjianqin
 * @date 2020-02-15
 */
public class SubmitJobFailureException extends RuntimeException {
    public SubmitJobFailureException() {
    }

    public SubmitJobFailureException(String message) {
        super(message);
    }

    public SubmitJobFailureException(Throwable cause) {
        super(cause);
    }
}
