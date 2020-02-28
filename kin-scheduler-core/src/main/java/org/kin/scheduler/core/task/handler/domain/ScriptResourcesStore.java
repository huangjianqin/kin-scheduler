package org.kin.scheduler.core.task.handler.domain;

import org.kin.framework.utils.NetUtils;
import org.kin.framework.utils.StringUtils;
import org.kin.scheduler.core.utils.GitUtils;
import org.kin.scheduler.core.utils.SVNUtils;
import org.kin.scheduler.core.utils.ScriptUtils;

/**
 * @author huangjianqin
 * @date 2020-02-21
 * <p>
 * 脚本存储位置
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
        public boolean cloneResources(String scriptResources, String user, String password, String path) {
            return GitUtils.cloneRepository(scriptResources, user, password, path);
        }
    },
    /**
     * remote仓库
     */
    SVN("remote仓库") {
        @Override
        public boolean cloneResources(String scriptResources, String user, String password, String path) {
            return SVNUtils.checkoutRepository(scriptResources, user, password, path);
        }
    },
    /**
     * 源代码
     */
    RESOURCE_CODE("源代码") {
        @Override
        public boolean cloneResources(String scriptResources, String user, String password, String path) {
            return ScriptUtils.createScriptFile(path, scriptResources);
        }
    };
    public static ScriptResourcesStore[] STORES = values();
    private String desc;

    ScriptResourcesStore(String desc) {
        this.desc = desc;
    }

    public abstract boolean cloneResources(String scriptResources, String user, String password, String path);

    //----------------------------------------------------------------------------------

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
