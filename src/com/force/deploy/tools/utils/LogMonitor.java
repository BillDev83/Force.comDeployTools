/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools.utils;

import com.force.deploy.tools.MainUIController;
import com.sforce.soap.tooling.ApexLog;
import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.SObject;
import com.sforce.soap.tooling.SoapConnection;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel
 */
public class LogMonitor extends Thread {

    private static final Logger log = Logger.getLogger(ConnectionsManager.class.getName());

    private String query;
    private final SoapConnection toolingConn;

    public LogMonitor(String q, SoapConnection toolingConn) {
        this.query = q;
        this.toolingConn = toolingConn;
    }

    @Override
    public void run() {
        try {
            while (true) {
                if(!MainUIController.logItemIds.isEmpty()) {
                    List<String> ids = new ArrayList<>();
                    for(String id : MainUIController.logItemIds) {
                        ids.add("\'" + id + "\'");
                    }
                    query += "AND Id NOT IN (" + String.join(",", ids) + ")";
                }
                QueryResult qr = toolingConn.query(query);

                for (SObject o : qr.getRecords()) {
                    ApexLog l = (ApexLog) o;
                    LogItem li = new LogItem(l.getId(), l.getStatus(), l.getLocation(), l.getOperation(), l.getRequest());
                    MainUIController.logItems.add(li);
                    MainUIController.logItemIds.add(l.getId());
                }

                Thread.sleep(5000);
            }
        } catch (Exception ex) {
            log.log(Level.INFO, null, ex);
        }
    }
}
