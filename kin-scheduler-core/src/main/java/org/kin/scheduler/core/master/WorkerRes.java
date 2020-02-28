package org.kin.scheduler.core.master;

/**
 * @author huangjianqin
 * @date 2020-02-13
 */
public class WorkerRes {
    private String workerId;
    private int parallelism;

    public WorkerRes(String workerId) {
        this.workerId = workerId;
    }

    public void useParallelismRes(int parallelism) {
        this.parallelism -= parallelism;
    }

    public void recoverParallelismRes(int parallelism) {
        this.parallelism += parallelism;
    }

    public void recoverRes(WorkerRes res) {
        this.parallelism += res.getParallelism();
    }

    //getter

    public String getWorkerId() {
        return workerId;
    }

    public int getParallelism() {
        return parallelism;
    }
}
