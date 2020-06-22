package org.kin.scheduler.core.master.transport;

import java.io.Serializable;

/**
 * master定时检查心跳消息
 *
 * @author huangjianqin
 * @date 2020-06-16
 */
public class CheckHeartbeatTimeout implements Serializable {
    private static final long serialVersionUID = 4921926173731490306L;
    public static final CheckHeartbeatTimeout INSTANCE = new CheckHeartbeatTimeout();
}
