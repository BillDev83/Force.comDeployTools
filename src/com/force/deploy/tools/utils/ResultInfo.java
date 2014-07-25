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
import com.sforce.soap.metadata.Error;

/**
 *
 * @author Daniel
 */
public class ResultInfo {

    private final SimpleStringProperty component;
    private final SimpleStringProperty statusCode;
    private final SimpleStringProperty message;
    private final SimpleStringProperty success;

    public ResultInfo(String component, String statusCode, String message, boolean success) {
        this.component = new SimpleStringProperty(component);
        this.statusCode = new SimpleStringProperty(statusCode);
        this.message = new SimpleStringProperty(message);
        this.success = new SimpleStringProperty(String.valueOf(success));
    }
    
    public static ResultInfo fromDeleteResult(DeleteResult result) {
        String statusCode = "OK", message = "Success!";
        if (!result.isSuccess()) {
            Error e = result.getErrors()[0];
            statusCode = e.getStatusCode().toString();
            message = e.getMessage();
        }
        return new ResultInfo(result.getFullName(), statusCode, message, result.isSuccess());
    }
    
    public static ResultInfo fromUpsertResult(UpsertResult result) {
        String statusCode = "OK", message = "Success!";
        if (!result.isSuccess()) {
            Error e = result.getErrors()[0];
            statusCode = e.getStatusCode().toString();
            message = e.getMessage();
        }
        return new ResultInfo(result.getFullName(), statusCode, message, result.isSuccess());
    }
    
    public static ResultInfo fromSaveResult(SaveResult result) {
        String statusCode = "OK", message = "Success!";
        if (!result.isSuccess()) {
            Error e = result.getErrors()[0];
            statusCode = e.getStatusCode().toString();
            message = e.getMessage();
        }
        return new ResultInfo(result.getFullName(), statusCode, message, result.isSuccess());
    }
    
    public ResultInfo(String component, String message) {
        this.component = new SimpleStringProperty(component);
        statusCode = new SimpleStringProperty("UNKNOWN");
        this.message = new SimpleStringProperty(message);
        success = new SimpleStringProperty(String.valueOf(message.toLowerCase().contains("succeeded")));
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
