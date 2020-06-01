package org.kin.scheduler.core.task.handler.params;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class ScriptParam extends GlueParam {
    private String scriptResources;
    private String scriptResourcesStore;
    private String user;
    private String password;

    //setter && getter

    public String getScriptResources() {
        return scriptResources;
    }

    public void setScriptResources(String scriptResources) {
        this.scriptResources = scriptResources;
    }

    public String getScriptResourcesStore() {
        return scriptResourcesStore;
    }

    public void setScriptResourcesStore(String scriptResourcesStore) {
        this.scriptResourcesStore = scriptResourcesStore;
    }

    public String getUser() {
        return user;
    }

    public void setUser(String user) {
        this.user = user;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }
}
