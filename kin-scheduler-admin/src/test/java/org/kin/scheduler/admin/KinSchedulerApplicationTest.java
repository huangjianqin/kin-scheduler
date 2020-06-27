package org.kin.scheduler.admin;

import org.kin.scheduler.core.worker.utils.WorkerUtils;

/**
 * @author huangjianqin
 * @date 2020-06-27
 */
public class KinSchedulerApplicationTest {
    public static void main(String[] args) {
        WorkerUtils.runMoreWorker();
        KinSchedulerApplication.main(args);
    }
}
