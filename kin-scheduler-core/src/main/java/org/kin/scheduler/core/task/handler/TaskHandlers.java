package org.kin.scheduler.core.task.handler;

import org.kin.framework.utils.ClassUtils;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.handler.domain.Singleton;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * TaskHandler的工具类
 * 负责寻找合适的TaskHandler
 */
public class TaskHandlers {
    /** 存储TaskHandler单例, 重复使用 */
    private static Map<Class<?>, TaskHandlerInfo> paramType2TaskHandler;

    static {
        init();
    }

    private static void init() {
        synchronized (TaskHandlers.class) {
            Set<Class<? extends TaskHandler>> taskHandlerImplClasses = ClassUtils.getSubClass(TaskHandler.class.getPackage().getName(), TaskHandler.class, false);
            Map<Class<?>, TaskHandlerInfo> paramType2TaskHandler = new HashMap<>(taskHandlerImplClasses.size());
            for (Class<? extends TaskHandler> taskHandlerType : taskHandlerImplClasses) {
                try {
                    Constructor constructor = taskHandlerType.getConstructor();
                    TaskHandler taskHandler;
                    if (taskHandlerType.isAnnotationPresent(Singleton.class)) {
                        taskHandler = (TaskHandler) constructor.newInstance();
                        paramType2TaskHandler.put(taskHandler.getTaskParamType(), new TaskHandlerInfo(taskHandlerType, taskHandler));
                    }
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
            }

            TaskHandlers.paramType2TaskHandler = paramType2TaskHandler;
        }
    }

    public static TaskHandler getTaskHandler(TaskDescription taskDescription) {
        Class<?> paramType = taskDescription.getParam().getClass();
        if (paramType2TaskHandler.containsKey(paramType)) {
            return paramType2TaskHandler.get(paramType).get();
        }

        return null;
    }

    //--------------------------------------------------------------------------------------------------------

    private static class TaskHandlerInfo {
        private Class<? extends TaskHandler> type;
        private TaskHandler singleton;

        public TaskHandlerInfo(Class<? extends TaskHandler> type) {
            this.type = type;
        }

        public TaskHandlerInfo(Class<? extends TaskHandler> type, TaskHandler singleton) {
            this.type = type;
            this.singleton = singleton;
        }

        public TaskHandler get() {
            if (Objects.nonNull(singleton)) {
                //单利模式的TaskHandler
                return singleton;
            } else {
                Constructor constructor;
                try {
                    constructor = type.getConstructor();
                    return (TaskHandler) constructor.newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    e.printStackTrace();
                }
                return null;
            }
        }
    }
}
