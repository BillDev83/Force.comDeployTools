/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.force.deploy.tools.utils;

import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.tooling.SoapConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel
 */
public class ConnectionsManager {
    
    private static final Logger log = Logger.getLogger(ConnectionsManager.class.getName());
    
    public static String getEndpoint(String environment, String instance) {
        if (null != environment) {
            switch (environment) {
                case "Production/Developer Edition":
                    return "https://login.salesforce.com/services/Soap/u/29.0";
                case "Sandbox":
                    return "https://test.salesforce.com/services/Soap/u/29.0";
                case "Pre-release":
                    return "";
                case "Other (Specify)":
                    return instance;
            }
        }
        return "";
    }
    
    public static PartnerConnection getPartnerConnection(Project project) {
        try {
            String endPoint = getEndpoint(project.environment, project.instance);

            ConnectorConfig config = new ConnectorConfig();
            config.setAuthEndpoint(endPoint);
            config.setServiceEndpoint(endPoint);
            config.setManualLogin(true);

            PartnerConnection pc = new PartnerConnection(config);
            LoginResult result = pc.login(project.username, project.password + project.securityToken);
            
            config.setServiceEndpoint(result.getServerUrl());
            config.setSessionId(result.getSessionId());
            return new PartnerConnection(config);
        } catch (ConnectionException ex) {
            log.log(Level.INFO, null, ex);
        }
        return null;
    }
    
    public static MetadataConnection getMetaConnection(Project project) {
        try {
            String endPoint = getEndpoint(project.environment, project.instance);

            ConnectorConfig config = new ConnectorConfig();
            config.setAuthEndpoint(endPoint);
            config.setServiceEndpoint(endPoint);
            config.setManualLogin(true);

            PartnerConnection pc = new PartnerConnection(config);
            LoginResult result = pc.login(project.username, project.password + project.securityToken);

            config.setServiceEndpoint(result.getMetadataServerUrl());
            config.setSessionId(result.getSessionId());
            return new MetadataConnection(config);
        } catch (ConnectionException ex) {
            log.log(Level.INFO, null, ex);
        }
        return null;
    }

    public static SoapConnection getToolingConnection(Project project) {
        try {
            String endPoint = getEndpoint(project.environment, project.instance);

            ConnectorConfig config = new ConnectorConfig();
            config.setAuthEndpoint(endPoint);
            config.setServiceEndpoint(endPoint);
            config.setManualLogin(true);

            PartnerConnection pc = new PartnerConnection(config);
            LoginResult result = pc.login(project.username, project.password + project.securityToken);

            config.setServiceEndpoint(result.getServerUrl().replace("/Soap/u", "/Soap/T"));
            config.setSessionId(result.getSessionId());
            return new SoapConnection(config);
        } catch (ConnectionException ex) {
            log.log(Level.INFO, null, ex);
        }
        return null;
    }
}
