package org.kin.scheduler.core.task.handler.params;

import org.kin.scheduler.core.task.handler.domain.ScriptResourcesStore;

import java.util.Arrays;

/**
 * 脚本task参数
 *
 * @author huangjianqin
 * @date 2020-02-21
 */
public class ScriptParam extends GlueParam {
    /** 脚本源url/源代码 */
    private String scriptResources;
    /** 脚本源方式(git,svn,源代码==) */
    private String scriptResourcesStore;
    private String user;
    private String password;
    /** 分片下标 */
    private int shardingIndex;
    /** 分片总数 */
    private int shardingTotal;

    /**
     * 源码脚本执行
     */
    public static ScriptParam sourceCode(String scriptResources) {
        return sourceCodeSharding(scriptResources, 0, 0);
    }

    /**
     * 源码脚本分片执行
     */
    public static ScriptParam sourceCodeSharding(String scriptResources, int shardingIndex, int shardingTotal) {
        return ScriptParam.of(scriptResources, ScriptResourcesStore.SOURCE_CODE.name(), "", "", 0, 0);
    }

    public static ScriptParam of(String scriptResources, String scriptResourcesStore,
                                 String user, String password) {
        return ScriptParam.of(scriptResources, scriptResourcesStore, user, password, 0, 0);
    }

    public static ScriptParam of(String scriptResources, String scriptResourcesStore,
                                 String user, String password, int shardingIndex, int shardingTotal) {
        ScriptParam scriptParam = new ScriptParam();
        scriptParam.scriptResources = scriptResources;
        scriptParam.scriptResourcesStore = scriptResourcesStore;
        scriptParam.user = user;
        scriptParam.password = password;
        scriptParam.shardingIndex = shardingIndex;
        scriptParam.shardingTotal = shardingTotal;
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

    public int getShardingIndex() {
        return shardingIndex;
    }

    public void setShardingIndex(int shardingIndex) {
        this.shardingIndex = shardingIndex;
    }

    public int getShardingTotal() {
        return shardingTotal;
    }

    public void setShardingTotal(int shardingTotal) {
        this.shardingTotal = shardingTotal;
    }

    @Override
    public String toString() {
        return "ScriptParam{" +
                "scriptResources='" + scriptResources + '\'' +
                ", scriptResourcesStore='" + scriptResourcesStore + '\'' +
                ", user='" + user + '\'' +
                ", password='" + password + '\'' +
                ", shardingIndex=" + shardingIndex +
                ", shardingTotal=" + shardingTotal +
                ", command='" + command + '\'' +
                ", type='" + type + '\'' +
                ", params=" + Arrays.toString(params) +
                '}';
    }
}
