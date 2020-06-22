package org.kin.scheduler.core.task.handler.params;

/**
 * 脚本task参数
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
public class ScriptParam extends GlueParam {
    private String scriptResources;
    private String scriptResourcesStore;
    private String user;
    private String password;

    public static ScriptParam of(String scriptResources, String scriptResourcesStore,
                                 String user, String password) {
        ScriptParam scriptParam = new ScriptParam();
        scriptParam.scriptResources = scriptResources;
        scriptParam.scriptResourcesStore = scriptResourcesStore;
        scriptParam.user = user;
        scriptParam.password = password;
        return scriptParam;
    }

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

    @Override
    public String toString() {
        return "ScriptParam{" +
                "scriptResources='" + scriptResources + '\'' +
                ", scriptResourcesStore='" + scriptResourcesStore + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", command='" + command + '\'' +
                ", type='" + type + '\'' +
                '}';
    }
}
