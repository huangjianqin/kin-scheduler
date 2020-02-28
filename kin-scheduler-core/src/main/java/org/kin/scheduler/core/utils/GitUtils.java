package org.kin.scheduler.core.utils;

import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;

import java.io.File;
import java.util.Collections;

/**
 * @author huangjianqin
 * @date 2020-02-20
 */
public class GitUtils {
    /**
     * clone git repository brance 'master'
     */
    public static boolean cloneRepository(String remote, String user, String password, String targetDir) {
        return cloneRepository(remote, "master", user, password, targetDir);
    }

    /**
     * clone git repository
     */
    public static boolean cloneRepository(String remote, String branch, String user, String password, String targetDir) {
        File targetDirFile = new File(targetDir);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdir();
        }

        CloneCommand clone = Git.cloneRepository().setURI(remote).setDirectory(targetDirFile).setBranchesToClone(Collections.singletonList(branch));
//        if (REMOTE_URL.contains("ssh")) {
//            MySShSessionFactory myFactory = new MySShSessionFactory();
//            myFactory.setSshKeyFilePath("C:/id_rsa");
//            SshSessionFactory.setInstance(myFactory);
//        }
        if (remote.contains("http") || remote.contains("https")) {
            UsernamePasswordCredentialsProvider provider = new UsernamePasswordCredentialsProvider(user, password);
            clone.setCredentialsProvider(provider);
        }

        Git clonedRepository;
        try {
            clonedRepository = clone.call();
            clonedRepository.close();
            // now open the created repository
//            FileRepositoryBuilder builder = new FileRepositoryBuilder();
//            Repository repository = builder.setGitDir(new File(localPath + "/.git")).readEnvironment()
//                    // scan environment GIT_DIR
//                    // GIT_WORK_TREE
//                    // variables
//                    .findGitDir() // scan up the file system tree
//                    .build();
            return true;
        } catch (GitAPIException e) {

        }

        return false;
    }
}
