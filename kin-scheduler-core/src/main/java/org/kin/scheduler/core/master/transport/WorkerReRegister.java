package org.kin.scheduler.core.master.transport;

import java.io.Serializable;

/**
 * 通知worker重新注册消息
 *
 * @author huangjianqin
 * @date 2020-06-18
 */
public class WorkerReRegister implements Serializable {
    private static final long serialVersionUID = 1437965955374496787L;
    public static WorkerReRegister INSTANCE = new WorkerReRegister();
}
