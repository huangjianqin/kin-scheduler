package org.kin.scheduler.core.worker.utils;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;
import org.kin.scheduler.core.worker.WorkerRunner;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * worker启动工具类
 *
 * @author huangjianqin
 * @date 2020-06-27
 */
public class WorkerUtils {
    /**
     * 在同一jvm下, 根据配置文件(默认scheduler-workers)定义的worker id(每行一个), 启动n个worker
     */
    public static void runMoreWorker() {
        //读取scheduler.yml来获取worker起始端口
        Config config = Configs.getCfg();
        int workerPort = config.getWorkerPort();

        ExecutorService executors = Executors.newCachedThreadPool();
        List<Thread> threads = new ArrayList<>();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("scheduler-workers");
        if (Objects.nonNull(is)) {
            try (Scanner scanner = new Scanner(is)) {
                while (scanner.hasNext()) {
                    String executorId = scanner.nextLine();
                    while (NetUtils.isPortInRange(workerPort) && !NetUtils.isValidPort(workerPort)) {
                        workerPort++;
                    }

                    if (NetUtils.isPortInRange(workerPort)) {
                        int finalWorkerPort = workerPort;
                        executors.execute(() -> {
                            Thread curThread = Thread.currentThread();
                            threads.add(curThread);
                            WorkerRunner.main(new String[]{executorId, String.valueOf(finalWorkerPort)});
                        });
                    }
                }
            }
        }

        if (CollectionUtils.isNonEmpty(threads)) {
            JvmCloseCleaner.DEFAULT().add(JvmCloseCleaner.MAX_PRIORITY, () -> {
                for (Thread thread : threads) {
                    thread.interrupt();
                }
            });
        }
    }
}
