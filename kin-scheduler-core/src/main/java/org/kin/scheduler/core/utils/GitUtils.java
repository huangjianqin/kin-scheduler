package org.kin.scheduler.core.utils;

import ch.qos.logback.classic.Logger;
import org.eclipse.jgit.api.CloneCommand;
import org.eclipse.jgit.api.Git;
import org.eclipse.jgit.api.errors.GitAPIException;
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider;
import org.kin.framework.utils.CollectionUtils;
import org.kin.scheduler.core.log.Loggers;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.util.Collection;
import java.util.Collections;
import java.util.Objects;

/**
 * @author huangjianqin
 * @date 2020-02-20
 */
public class GitUtils {
    private static final org.slf4j.Logger log = LoggerFactory.getLogger(GitUtils.class);

    /**
     * clone git repository latest branch'
     */
    public static boolean cloneRepository(String remote, String user, String password, String targetDir) {
        return cloneRepository(remote, user, password, targetDir, Collections.emptyList());
    }

    /**
     * clone git repository
     */
    public static boolean cloneRepository(String remote, String user, String password, String targetDir, String... branches) {
        return cloneRepository(remote, user, password, targetDir, CollectionUtils.toList(branches));
    }

    /**
     * clone git repository
     */
    public static boolean cloneRepository(String remote, String user, String password, String targetDir, Collection<String> branches) {
        File targetDirFile = new File(targetDir);
        if (!targetDirFile.exists()) {
            targetDirFile.mkdir();
        }

        CloneCommand clone = Git.cloneRepository().setURI(remote).setDirectory(targetDirFile);
        if (CollectionUtils.isNonEmpty(branches)) {
            clone.setBranchesToClone(branches);
        }
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
            Logger logger = Loggers.logger();
            if (Objects.nonNull(logger)) {
                logger.error("", e);
            } else {
                log.error("", e);
            }
        }

        return false;
    }
}
