package org.kin.scheduler.core.master;

import org.kin.scheduler.core.cfg.Config;
import org.kin.scheduler.core.cfg.Configs;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class MasterRunner {
    public static void main(String[] args) {
        //读取scheduler.yml来获取配置
        Config config = Configs.getCfg();
        Master master = new Master(config.getMasterBackendHost(), config.getMasterBackendPort());
        try {
            master.init();
            master.start();
        } finally {
            master.stop();
        }
    }
}
