package org.kin.scheduler.core.worker;

import org.kin.scheduler.core.worker.utils.WorkerUtils;

/**
 * @author huangjianqin
 * @date 2020-02-08
 */
public class WorkerRunnerTest {
    public static void main(String[] args) {
        WorkerUtils.runMoreWorker();
        synchronized (WorkerRunnerTest.class) {
            try {
                WorkerRunnerTest.class.wait();
            } catch (InterruptedException e) {

            }
        }
    }
}
