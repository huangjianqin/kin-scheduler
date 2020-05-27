package org.kin.scheduler.core.driver.exception;

/**
 * @author huangjianqin
 * @date 2020-02-15
 */
public class RegisterApplicationFailureException extends RuntimeException {
    public RegisterApplicationFailureException() {
    }

    public RegisterApplicationFailureException(String message) {
        super(message);
    }

    public RegisterApplicationFailureException(Throwable cause) {
        super(cause);
    }
}
