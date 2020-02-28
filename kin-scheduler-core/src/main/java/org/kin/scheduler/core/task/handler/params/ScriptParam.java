package org.kin.scheduler.core.task.handler.params;

/**
 * @author huangjianqin
 * @date 2020-02-21
 */
public class ScriptParam extends GlueParam {
    private String ScriptResources;
    private String ScriptResourcesStore;
    private String user;
    private String password;

    //setter && getter

    public String getScriptResources() {
        return ScriptResources;
    }

    public void setScriptResources(String scriptResources) {
        ScriptResources = scriptResources;
    }

    public String getScriptResourcesStore() {
        return ScriptResourcesStore;
    }

    public void setScriptResourcesStore(String scriptResourcesStore) {
        ScriptResourcesStore = scriptResourcesStore;
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
