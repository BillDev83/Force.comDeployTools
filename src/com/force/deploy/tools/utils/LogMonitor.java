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
import com.sforce.soap.tooling.TraceFlag;
import com.sforce.ws.ConnectionException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;

/**
 *
 * @author Daniel
 */
public class LogMonitor extends Thread {

    private static class Holder {
        static final LogMonitor instance = new LogMonitor();
    }

    private static final Logger log = Logger.getLogger(LogMonitor.class.getName());

    private String query;
    private SoapConnection toolingConn;

    public static LogMonitor getInstance(String q, SoapConnection toolingConn) {
        Holder.instance.query = q;
        Holder.instance.toolingConn = toolingConn;
        return Holder.instance;
    }

    public void monitorUser(String uid) {
        TraceFlag traceFlag = new TraceFlag();
        traceFlag.setApexCode("Debug");
        traceFlag.setApexProfiling("Info");
        traceFlag.setCallout("Info");
        traceFlag.setDatabase("Info");
        traceFlag.setSystem("Debug");
        traceFlag.setValidation("Info");
        traceFlag.setVisualforce("Info");
        traceFlag.setWorkflow("Info");
        //set an expiration date
        Calendar expirationDate = Calendar.getInstance();
        expirationDate.add(Calendar.HOUR, 1);
        traceFlag.setExpirationDate(expirationDate);
        //set the ID of the user to monitor
        traceFlag.setTracedEntityId(uid);
        
        try {
            toolingConn.create(new SObject[] {traceFlag});
        } catch (ConnectionException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    @Override
    public void run() {
        try {
            while (true) {
                if (!MainUIController.logItemIds.isEmpty()) {
                    List<String> ids = new ArrayList<>();
                    for (String id : MainUIController.logItemIds) {
                        ids.add("\'" + id + "\'");
                    }
                    query += " AND Id NOT IN (" + String.join(",", ids) + ")";
                }
                QueryResult qr = toolingConn.query(query);

                for (SObject o : qr.getRecords()) {
                    ApexLog l = (ApexLog) o;
                    LogItem li = new LogItem(l.getId(), l.getStatus(), l.getLocation(), 
                            l.getOperation(), l.getRequest(), l.getDurationMilliseconds().toString(), 
                            l.getLogLength().toString());
                    
                    Platform.runLater(new Runnable() {
                        @Override
                        public void run() {
                           // Update/Query the FX classes here
                            MainUIController.logItems.add(li);
                            MainUIController.logItemIds.add(l.getId());
                        }
                     });
                }

                Thread.sleep(5000);
            }
        } catch (Exception ex) {
            log.log(Level.INFO, null, ex);
        }
    }
}
