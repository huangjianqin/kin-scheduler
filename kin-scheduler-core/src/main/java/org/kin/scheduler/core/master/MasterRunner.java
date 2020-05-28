package org.kin.scheduler.core.master;

/**
 * @author huangjianqin
 * @date 2020-02-06
 */
public class MasterRunner {
    public static void main(String[] args) {
        if (args.length == 4) {
            String masterBackendHost = args[0];
            int masterBackendPort = Integer.parseInt(args[1]);
            String logPath = args[2];
            int heartbeat = Integer.parseInt(args[3]);

            Master master = new Master(masterBackendHost, masterBackendPort, logPath, heartbeat);
            try {
                master.init();
                master.start();
                synchronized (master) {
                    try {
                        master.wait();
                    } catch (InterruptedException e) {

                    }
                }
            } finally {
                master.stop();
            }
        }
    }
}
