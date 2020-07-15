package org.kin.scheduler.core.task.handler.domain;

import org.kin.framework.utils.*;

/**
 * 脚本文件存储位置类型
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
public enum ScriptResourcesStore {
    /**
     * 远程服务器
     */
    REMOTE("远程服务器") {
        @Override
        public boolean cloneResources(String scriptResources, String user, String password, String path) {
            return NetUtils.copyRemoteFile(scriptResources, path);
        }
    },
    /**
     * git仓库
     */
    GIT("git仓库") {
        @Override
        public boolean cloneResources(String scriptResources, String user, String password, String path) throws Exception {
            return GitUtils.cloneRepository(scriptResources, user, password, path);
        }
    },
    /**
     * remote仓库
     */
    SVN("remote仓库") {
        @Override
        public boolean cloneResources(String scriptResources, String user, String password, String path) throws Exception {
            return SVNUtils.checkoutRepository(scriptResources, user, password, path);
        }
    },
    /**
     * 源代码
     */
    SOURCE_CODE("源代码") {
        @Override
        public boolean cloneResources(String scriptResources, String user, String password, String path) throws Exception {
            return FileUtils.createFile(path, scriptResources);
        }
    };
    public static ScriptResourcesStore[] STORES = values();
    private String desc;

    ScriptResourcesStore(String desc) {
        this.desc = desc;
    }

    public abstract boolean cloneResources(String scriptResources, String user, String password, String path) throws Exception;

    //----------------------------------------------------------------------------------

    /**
     * 根据名称获取脚本文件存储位置类型
     */
    public static ScriptResourcesStore getByName(String name) {
        if (StringUtils.isNotBlank(name)) {
            for (ScriptResourcesStore item : STORES) {
                if (item.name().toLowerCase().equals(name.toLowerCase())) {
                    return item;
                }
            }
        }

        return null;
    }
}
