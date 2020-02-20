package org.kin.scheduler.core.utils;

import org.tmatesoft.svn.core.SVNDepth;
import org.tmatesoft.svn.core.SVNException;
import org.tmatesoft.svn.core.SVNURL;
import org.tmatesoft.svn.core.internal.io.dav.DAVRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.fs.FSRepositoryFactory;
import org.tmatesoft.svn.core.internal.io.svn.SVNRepositoryFactoryImpl;
import org.tmatesoft.svn.core.internal.wc.DefaultSVNOptions;
import org.tmatesoft.svn.core.wc.*;

import java.io.File;

/**
 * @author huangjianqin
 * @date 2020-02-20
 */
public class SVNUtils {
    static {
        DAVRepositoryFactory.setup();
        SVNRepositoryFactoryImpl.setup();
        FSRepositoryFactory.setup();
    }
    /**
     * checkout svn repository
     */
    public static boolean checkoutRepository(String remote, String user, String password, String targetDir){
        File targetDirFile = new File(targetDir);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdir();
        }

        try {
            SVNURL svnUrl = SVNURL.parseURIEncoded(remote);
            ISVNOptions options = SVNWCUtil.createDefaultOptions(true);
            SVNClientManager svnClientManager = SVNClientManager.newInstance(
                    (DefaultSVNOptions) options, user, password);
            SVNUpdateClient updateClient = svnClientManager.getUpdateClient();
            updateClient.setIgnoreExternals(false);

            updateClient.doCheckout(svnUrl, targetDirFile, SVNRevision.HEAD, SVNRevision.HEAD, SVNDepth.INFINITY, true);
            return true;
        } catch (SVNException e) {

        }

        return false;
    }
}
