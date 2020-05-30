package org.kin.scheduler.core.utils;

/**
 * @author huangjianqin
 * @date 2020-05-30
 */
public class GitUtilsTest {
    public static void main(String[] args) {
        GitUtils.cloneRepository("https://github.com/huangjianqin/kin-java-agent.git",
                "", "", "test");
    }
}
