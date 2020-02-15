package org.kin.scheduler.core.worker.domain;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-02-09
 */
public class ExecutorLaunchInfo implements Serializable {
    private int parallelism;

    public ExecutorLaunchInfo() {
    }

    public ExecutorLaunchInfo(int parallelism) {
        this.parallelism = parallelism;
    }

    //setter && getter

    public int getParallelism() {
        return parallelism;
    }

    public void setParallelism(int parallelism) {
        this.parallelism = parallelism;
    }
}
