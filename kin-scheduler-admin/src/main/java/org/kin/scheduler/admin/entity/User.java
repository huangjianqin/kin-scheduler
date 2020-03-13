package org.kin.scheduler.admin.entity;

/**
 * @author huangjianqin
 * @date 2020-03-07
 */
public class User {
    public static final int USER = 0;
    public static final int ADMIN = 1;

    //唯一id
    private int id;
    //账号
    private String account;
    //密码
    private String password;
    //0-普通用户、1-管理员
    private int role;
    //名称
    private String name;

    public boolean isAdmin() {
        return role == ADMIN;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public int getRole() {
        return role;
    }

    public void setRole(int role) {
        this.role = role;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
