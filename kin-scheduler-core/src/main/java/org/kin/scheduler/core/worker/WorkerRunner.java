package org.kin.scheduler.core.worker;

import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

/**
 * @author huangjianqin
 * @date 2020-02-06
 * <p>
 * bash
 * 1. 读取scheduler-workers文件获取workerId(一行一个)
 * 2. 读取scheduler.yml来获取配置
 * 3. 批量启动worker
 */
public class WorkerRunner {
    /**
     * args包含一个workerId和worker rpc端口
     */
    public static void main(String[] args) {
        if (args.length == 2) {
            Config config = Configs.getCfg();
            String workerId = args[0];
            config.setWorkerBackendPort(Integer.valueOf(args[1]));

            Worker worker = new Worker(workerId, config);
            try {
                worker.init();
                worker.start();
                synchronized (worker) {
                    try {
                        worker.wait();
                    } catch (InterruptedException e) {

                    }
                }
            } finally {
                worker.stop();
            }

        }
    }
}
