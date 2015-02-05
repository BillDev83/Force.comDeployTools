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
import com.sforce.soap.tooling.SaveResult;
import com.sforce.soap.tooling.SoapConnection;
import com.sforce.soap.tooling.TraceFlag;
import com.sforce.ws.ConnectionException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.application.Platform;
import javafx.concurrent.Service;
import javafx.concurrent.Task;

/**
 *
 * @author Daniel
 */
public class LogMonitor extends Service<Void> {

    private static final Logger log = Logger.getLogger(LogMonitor.class.getName());

    private final String query;
    private final String userId;
    private final SoapConnection toolingConn;

    public LogMonitor(String q, SoapConnection tc, String uid) {
        query = q;
        toolingConn = tc;
        userId = uid;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            public Void call() {
                monitorUser(userId);
                
                int count = 0;
                int MAX = 240; // 240 API requests in 20 minutes
                while (true) {
                    try {
                        updateProgress(count, MAX);
                        String additionalFilter = " AND StartTime > " + Instant.now().minusSeconds(5);
                        log.log(Level.INFO, "QUERY: " + query + additionalFilter);
                        QueryResult qr = toolingConn.query(query + additionalFilter);

                        for (SObject o : qr.getRecords()) {
                            ApexLog l = (ApexLog) o;
                            LogItem li = new LogItem(l.getId(), l.getStatus(), l.getLocation(),
                                    l.getOperation(), l.getRequest(), l.getDurationMilliseconds().toString(),
                                    l.getLogLength().toString());

                            Platform.runLater(() -> {
                                MainUIController.logItems.add(li);
                                MainUIController.logItemIds.add(l.getId());
                            });
                        }

                        if (count > MAX) {
                            log.info("Stopping after 20 minutes/240 API requests.");
                            break;
                        }
                        
                        if(isCancelled()) {
                            log.info("Monitor cancelled!");
                            break;
                        }

                        Thread.sleep(5000);
                        count++;
                    } catch (ConnectionException | InterruptedException ex) {
                        log.log(Level.INFO, "Query: " + query, ex);
                    }
                }
                updateProgress(0, 0);
                return null;
            }
        };
    }
    
    private void monitorUser(String uid) {
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
            QueryResult result = toolingConn.query("SELECT Id FROM TraceFlag WHERE TracedEntityId = '" + uid + "'");
            List<String> ids = new ArrayList<>();
            for (SObject o : result.getRecords()) {
                ids.add(o.getId());
            }
            toolingConn.delete(ids.toArray(new String[]{}));

            SaveResult[] results = toolingConn.create(new SObject[]{traceFlag});
            for (SaveResult r : results) {
                log.info("Tracing " + uid + "|" + r.getId());
            }
        } catch (ConnectionException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }
}
