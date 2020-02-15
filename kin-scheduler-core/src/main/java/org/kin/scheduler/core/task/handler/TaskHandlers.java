package org.kin.scheduler.core.task.handler;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.scheduler.core.task.Task;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * TaskHandler的工具类
 * 负责寻找合适的TaskHandler
 */
public class TaskHandlers {
    //存储TaskHandler单例, 重复使用
    private Map<Class<?>, TaskHandler> paramType2TaskHandler;

    public void init() {
        synchronized (this) {
            Map<Class<?>, TaskHandler> paramType2TaskHandler = new HashMap<>();

            Reflections reflections = new Reflections(TaskHandler.class.getPackage().getName(), new SubTypesScanner());
            for (Class<?> taskHandlerType : reflections.getSubTypesOf(TaskHandler.class)) {
                try {
                    Constructor constructor = taskHandlerType.getConstructor();
                    TaskHandler taskHandler = (TaskHandler) constructor.newInstance();
                    paramType2TaskHandler.put(taskHandler.getTaskParamType(), taskHandler);
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    ExceptionUtils.log(e);
                }

            }

            this.paramType2TaskHandler = paramType2TaskHandler;
        }
    }

    public TaskHandler getTaskHandler(Task task) {
        Class<?> paramType = task.getParam().getClass();
        return paramType2TaskHandler.get(paramType);
    }
}
