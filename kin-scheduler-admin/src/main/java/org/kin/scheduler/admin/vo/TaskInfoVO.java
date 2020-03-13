package org.kin.scheduler.admin.vo;

import org.kin.scheduler.admin.entity.TaskInfo;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public class TaskInfoVO extends TaskInfo {
    private String userName;

    public String getUserName() {
        return userName;
    }

    public void setUserName(String userName) {
        this.userName = userName;
    }
}
