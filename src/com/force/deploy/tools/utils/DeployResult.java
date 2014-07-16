/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools.utils;

import com.sforce.soap.metadata.DeleteResult;
import com.sforce.soap.metadata.SaveResult;
import com.sforce.soap.metadata.UpsertResult;
import javafx.beans.property.SimpleStringProperty;

/**
 *
 * @author Daniel
 */
public class DeployResult {

    private final SimpleStringProperty component;
    private final SimpleStringProperty statusCode;
    private final SimpleStringProperty message;
    private final SimpleStringProperty success;

    public DeployResult(DeleteResult result) {
        component = new SimpleStringProperty(result.getFullName());
        if (!result.isSuccess()) {
            com.sforce.soap.metadata.Error e = result.getErrors()[0];
            statusCode = new SimpleStringProperty(e.getStatusCode().toString());
            message = new SimpleStringProperty(e.getMessage());
        } else {
            statusCode = new SimpleStringProperty("OK");
            message = new SimpleStringProperty("Success!");
        }
        success = new SimpleStringProperty(String.valueOf(result.isSuccess()));
    }
    
    public DeployResult(UpsertResult result) {
        component = new SimpleStringProperty(result.getFullName());
        if (!result.isSuccess()) {
            com.sforce.soap.metadata.Error e = result.getErrors()[0];
            statusCode = new SimpleStringProperty(e.getStatusCode().toString());
            message = new SimpleStringProperty(e.getMessage());
        } else {
            statusCode = new SimpleStringProperty("OK");
            message = new SimpleStringProperty("Success!");
        }
        success = new SimpleStringProperty(String.valueOf(result.isSuccess()));
    }
    
    public DeployResult(SaveResult result) {
        component = new SimpleStringProperty(result.getFullName());
        if (!result.isSuccess()) {
            com.sforce.soap.metadata.Error e = result.getErrors()[0];
            statusCode = new SimpleStringProperty(e.getStatusCode().toString());
            message = new SimpleStringProperty(e.getMessage());
        } else {
            statusCode = new SimpleStringProperty("OK");
            message = new SimpleStringProperty("Success!");
        }
        success = new SimpleStringProperty(String.valueOf(result.isSuccess()));
    }
    
    public DeployResult(String component, String message) {
        this.component = new SimpleStringProperty(component);
        statusCode = new SimpleStringProperty("UNKNOWN");
        this.message = new SimpleStringProperty(message);
        success = new SimpleStringProperty("false");
    }

    public String getComponent() {
        return component.get();
    }

    public String getStatusCode() {
        return statusCode.get();
    }

    public String getMessage() {
        return message.get();
    }

    public String getSuccess() {
        return success.get();
    }

    public void setComponent(String component) {
        this.component.set(component);
    }

    public void setStatusCode(String statusCode) {
        this.statusCode.set(statusCode);
    }

    public void setMessage(String message) {
        this.message.set(message);
    }

    public void setSuccess(String success) {
        this.success.set(success);
    }
}
