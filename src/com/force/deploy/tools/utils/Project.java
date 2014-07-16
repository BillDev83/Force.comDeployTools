/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools.utils;

import java.io.Serializable;

/**
 *
 * @author Daniel
 */
public class Project implements Serializable {

    public static String PROJECT_REPOSITORY = "Projects";
    public String name;
    public String username;
    public String password;
    public String securityToken;
    public String environment;
    public String instance;

    public Project(String name, String username, String password, String securityToken, String environment, String instance) {
        this.name = name;
        this.username = username;
        this.password = password;
        this.securityToken = securityToken;
        this.environment = environment;
        this.instance = instance;
    }
}
