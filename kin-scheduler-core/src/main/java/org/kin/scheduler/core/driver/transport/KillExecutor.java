package org.kin.scheduler.core.driver.transport;

import java.io.Serializable;

/**
 * @author huangjianqin
 * @date 2020-06-18
 */
public class KillExecutor implements Serializable {
    private static final long serialVersionUID = 4632046239973112546L;
    public static final KillExecutor INSTANCE = new KillExecutor();
}
