package org.kin.scheduler.core.master;

import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

/**
 * @author huangjianqin
 * @date 2020-02-12
 */
public class MasterRunnerTest {
    public static void main(String[] args) {
        Config config = Configs.getCfg();
        String[] masterArgs = new String[]{
                config.getMasterBackendHost(),
                String.valueOf(config.getMasterBackendPort()),
                config.getLogPath(),
                String.valueOf(config.getHeartbeatTime())
        };
        MasterRunner.main(masterArgs);
    }
}
