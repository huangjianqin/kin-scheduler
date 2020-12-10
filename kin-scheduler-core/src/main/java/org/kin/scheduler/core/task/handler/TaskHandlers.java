package org.kin.scheduler.core.task.handler;

import org.kin.framework.utils.ClassUtils;
import org.kin.framework.utils.ExceptionUtils;
import org.kin.scheduler.core.task.TaskDescription;
import org.kin.scheduler.core.task.handler.domain.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * TaskHandler的工具类
 * 负责寻找合适的TaskHandler
 *
 * @author huangjianqin
 * @date 2020-02-06
 */
public class TaskHandlers {
    private static final Logger log = LoggerFactory.getLogger(TaskHandlers.class);
    /**
     * 存储TaskHandler单例, 重复使用
     * key -> class, value -> {@link TaskHandlerInfo}
     */
    private static Map<Class<?>, TaskHandlerInfo> paramType2TaskHandler;

    static {
        init();
    }

    private static void init() {
        synchronized (TaskHandlers.class) {
            //加载taskhandler信息
            Set<Class<? extends TaskHandler>> taskHandlerImplClasses = ClassUtils.getSubClass(TaskHandler.class.getPackage().getName(), TaskHandler.class, false);
            Map<Class<?>, TaskHandlerInfo> paramType2TaskHandler = new HashMap<>(taskHandlerImplClasses.size());
            for (Class<? extends TaskHandler> taskHandlerType : taskHandlerImplClasses) {
                try {
                    //获取空构造方法
                    Constructor constructor = taskHandlerType.getConstructor();
                    TaskHandler taskHandler = (TaskHandler) constructor.newInstance();

                    paramType2TaskHandler.put(taskHandler.getTaskParamType(),
                            new TaskHandlerInfo(taskHandlerType, taskHandlerType.isAnnotationPresent(Singleton.class) ? taskHandler : null));
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    ExceptionUtils.throwExt(e);
                }
            }

            TaskHandlers.paramType2TaskHandler = paramType2TaskHandler;
        }
    }

    /**
     * 获取taskhandler实例
     */
    public static TaskHandler getTaskHandler(TaskDescription taskDescription) {
        Class<?> paramType = taskDescription.getParam().getClass();
        if (paramType2TaskHandler.containsKey(paramType)) {
            return paramType2TaskHandler.get(paramType).get();
        }

        return null;
    }

    //--------------------------------------------------------------------------------------------------------

    /**
     * task handler信息
     */
    private static class TaskHandlerInfo {
        /** task handler class实例 */
        private Class<? extends TaskHandler> type;
        /** 支持单例模式的task handler才会创建实例 */
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
                //单例模式的TaskHandler
                return singleton;
            } else {
                //每次都new一个
                Constructor constructor;
                try {
                    constructor = type.getConstructor();
                    return (TaskHandler) constructor.newInstance();
                } catch (NoSuchMethodException | InstantiationException | IllegalAccessException | InvocationTargetException e) {
                    ExceptionUtils.throwExt(e);
                }
                return null;
            }
        }
    }
}
