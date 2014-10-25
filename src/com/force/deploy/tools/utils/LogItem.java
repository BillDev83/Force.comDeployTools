/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools.utils;

import javafx.beans.property.SimpleStringProperty;
import org.apache.http.client.fluent.Request;

/**
 *
 * @author Daniel
 */
public class LogItem {

    private SimpleStringProperty id;
    private SimpleStringProperty status;
    private SimpleStringProperty location;
    private SimpleStringProperty operation;
    private SimpleStringProperty request;
    
    public LogItem(String id, String status, String location, String operation, String request) {
        this.id = new SimpleStringProperty(id);
        this.status = new SimpleStringProperty(status);
        this.location = new SimpleStringProperty(location);
        this.operation = new SimpleStringProperty(operation);
        this.request = new SimpleStringProperty(request);
    }

    public void setId(SimpleStringProperty id) {
        this.id = id;
    }

    public void setStatus(SimpleStringProperty status) {
        this.status = status;
    }

    public void setLocation(SimpleStringProperty location) {
        this.location = location;
    }

    public void setOperation(SimpleStringProperty operation) {
        this.operation = operation;
    }

    public void setRequest(SimpleStringProperty request) {
        this.request = request;
    }

    public String getId() {
        return id.get();
    }

    public String getStatus() {
        return status.get();
    }

    public String getLocation() {
        return location.get();
    }

    public String getOperation() {
        return operation.get();
    }

    public String getRequest() {
        return request.get();
    }

    public String getRawLog(String serviceEndPoint, String sid) {
        try {
            serviceEndPoint = serviceEndPoint.replace("/Soap/T/", "/data/v");
            serviceEndPoint = serviceEndPoint.substring(0, serviceEndPoint.lastIndexOf("/"));
            serviceEndPoint = serviceEndPoint + "/tooling/sobjects/ApexLog/"+id.get()+"/Body/";
            
            System.out.println("End Point: " + serviceEndPoint);
            String rawLog = Request.Get(serviceEndPoint)
                    .addHeader("Authorization", "Bearer " + sid)
                    .execute().returnContent().asString();
            return rawLog;
        } catch (Exception ex) {
            System.out.println("ex" + ex.getMessage());
            return "";
        }
    }
}
