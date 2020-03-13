package org.kin.scheduler.admin.entity;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public class JobInfo {
    //唯一id
    private int id;
    //appName
    private String appName;
    //标题
    private String title;
    //优先级
    private int order;

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAppName() {
        return appName;
    }

    public void setAppName(String appName) {
        this.appName = appName;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public int getOrder() {
        return order;
    }

    public void setOrder(int order) {
        this.order = order;
    }
}
