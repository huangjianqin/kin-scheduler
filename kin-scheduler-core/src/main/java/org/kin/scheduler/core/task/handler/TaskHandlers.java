package org.kin.scheduler.core.task.handler;

import org.kin.framework.utils.ExceptionUtils;
import org.kin.scheduler.core.task.Task;
import org.kin.scheduler.core.task.handler.domain.Singleton;
import org.reflections.Reflections;
import org.reflections.scanners.SubTypesScanner;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * TaskHandler的工具类
 * 负责寻找合适的TaskHandler
 */
public class TaskHandlers {
    //存储TaskHandler单例, 重复使用
    private static Map<Class<?>, TaskHandlerInfo> paramType2TaskHandler;

    static {
        init();
    }

    private static void init() {
        synchronized (TaskHandlers.class) {
            Map<Class<?>, TaskHandlerInfo> paramType2TaskHandler = new HashMap<>();

            Reflections reflections = new Reflections(TaskHandler.class.getPackage().getName(), new SubTypesScanner());
            for (Class<? extends TaskHandler> taskHandlerType : reflections.getSubTypesOf(TaskHandler.class)) {
                try {
                    Constructor constructor = taskHandlerType.getConstructor();
                    TaskHandler taskHandler = null;
                    if(taskHandlerType.isAnnotationPresent(Singleton.class)){
                        taskHandler = (TaskHandler) constructor.newInstance();
                    }
                    paramType2TaskHandler.put(taskHandler.getTaskParamType(), new TaskHandlerInfo(taskHandlerType, taskHandler));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    ExceptionUtils.log(e);
                }
            }

            TaskHandlers.paramType2TaskHandler = paramType2TaskHandler;
        }
    }

    public static TaskHandler getTaskHandler(Task task) {
        Class<?> paramType = task.getParam().getClass();
        if(paramType2TaskHandler.containsKey(paramType)){
            return paramType2TaskHandler.get(paramType).get();
        }

        return null;
    }

    //--------------------------------------------------------------------------------------------------------
    private static class TaskHandlerInfo{
        private Class<? extends TaskHandler> type;
        private TaskHandler singleton;

        public TaskHandlerInfo(Class<? extends TaskHandler> type) {
            this.type = type;
        }

        public TaskHandlerInfo(Class<? extends TaskHandler> type, TaskHandler singleton) {
            this.type = type;
            this.singleton = singleton;
        }

        public TaskHandler get(){
            if(Objects.nonNull(singleton)){
                //单利模式的TaskHandler
                return singleton;
            }
            else{
                Constructor constructor;
                try {
                    constructor = type.getConstructor();
                    return (TaskHandler) constructor.newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    ExceptionUtils.log(e);
                }
                return null;
            }
        }
    }
}
