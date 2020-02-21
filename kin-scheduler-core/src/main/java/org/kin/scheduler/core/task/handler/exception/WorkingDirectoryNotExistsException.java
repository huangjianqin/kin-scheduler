package org.kin.scheduler.core.task.handler.exception;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class WorkingDirectoryNotExistsException extends RuntimeException{
    public WorkingDirectoryNotExistsException(String message) {
        super(message);
    }
}
