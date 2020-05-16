package org.kin.scheduler.core.worker;

import org.kin.framework.JvmCloseCleaner;
import org.kin.framework.utils.CollectionUtils;
import org.kin.framework.utils.NetUtils;
import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Scanner;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class WorkerRunnerTest {
    public static void main(String[] args) {
        //读取scheduler.yml来获取worker起始端口
        Config config = Configs.getCfg();
        int workerBackendPort = config.getWorkerBackendPort();

        ExecutorService executors = Executors.newCachedThreadPool();
        List<Thread> threads = new ArrayList<>();

        InputStream is = Thread.currentThread().getContextClassLoader().getResourceAsStream("scheduler-workers");
        if (Objects.nonNull(is)) {
            try (Scanner scanner = new Scanner(is)) {
                while (scanner.hasNext()) {
                    String executorId = scanner.nextLine();
                    while (NetUtils.isPortInRange(workerBackendPort) && !NetUtils.isValidPort(workerBackendPort)) {
                        workerBackendPort++;
                    }

                    if (NetUtils.isPortInRange(workerBackendPort)) {
                        int finalWorkerBackendPort = workerBackendPort;
                        executors.execute(() -> {
                            Thread curThread = Thread.currentThread();
                            threads.add(curThread);
                            WorkerRunner.main(new String[]{executorId, String.valueOf(finalWorkerBackendPort)});
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
            synchronized (WorkerRunnerTest.class) {
                try {
                    WorkerRunnerTest.class.wait();
                } catch (InterruptedException e) {

                }
            }
        }
    }
}
