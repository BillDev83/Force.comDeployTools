/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools.utils;

import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DeployResult;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.ws.ConnectionException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.control.TreeItem;

/**
 *
 * @author Daniel
 */
public class ApexDeployer extends Service<String> {

    private static class Holder {

        static final ApexDeployer instance = new ApexDeployer();
    }

    private static final Logger log = Logger.getLogger(ApexDeployer.class.getName());

    private MetadataConnection sourceMetaConn;
    private MetadataConnection targetMetaConn;

    private List<PackageTypeMembers> typeMembers = new ArrayList<>();

    // one second in milliseconds
    final static long ONE_SECOND = 4000;
    // maximum number of attempts to retrieve the results
    final static int MAX_NUM_POLL_REQUESTS = 50;
    final static String API_VERSION = "31.0";

    public static ApexDeployer getInstance() {
        return Holder.instance;
    }

    public static ApexDeployer getInstance(MetadataConnection sourceMetaConn,
            MetadataConnection targetMetaConn) {
        
        Holder.instance.sourceMetaConn = sourceMetaConn;
        Holder.instance.targetMetaConn = targetMetaConn;
        Holder.instance.typeMembers.clear();
        
        return Holder.instance;
    }

    public void prepare(String parentValue, List<TreeItem<String>> parentChildren) {
        
        List<String> children = new ArrayList<>();
        for (TreeItem<String> child : parentChildren) {
            String v = child.getValue();
            children.add(v);
        }

        PackageTypeMembers ptm = new PackageTypeMembers();
        ptm.setName(parentValue);
        ptm.setMembers(children.toArray(new String[]{}));

        typeMembers.add(ptm);
    }

    @Override
    protected Task<String> createTask() {
        return new Task<String>() {
            @Override
            protected String call() {
                try {
                    updateMessage("Preparing deployment...");

                    com.sforce.soap.metadata.Package p = new com.sforce.soap.metadata.Package();
                    p.setTypes(typeMembers.toArray(new PackageTypeMembers[]{}));
                    p.setVersion(API_VERSION);
                    RetrieveRequest request = new RetrieveRequest();
                    request.setUnpackaged(p);

                    // Start the retrieve operation
                    AsyncResult asyncResult = sourceMetaConn.retrieve(request);
                    log.info("asyncResult " + asyncResult.toString());
                    String asyncResultId = asyncResult.getId();

                    // Wait for the retrieve to complete
                    int poll = 0;
                    long waitTimeMilliSecs = ONE_SECOND;
                    RetrieveResult result = null;

                    while (true) {
                        Thread.sleep(waitTimeMilliSecs);
                        result = sourceMetaConn.checkRetrieveStatus(asyncResultId);

                        StringBuilder buf = new StringBuilder();
                        if (result.getMessages() != null) {
                            for (RetrieveMessage rm : result.getMessages()) {
                                buf.append(rm.getFileName() + " - " + rm.getProblem());
                            }
                        }
                        if (buf.length() > 0) {
                            log.info("Retrieve warnings:\n" + buf);
                        }
                        if (result.getZipFile() != null) {
                            break;
                        }
                    }

                    DeployOptions deployOptions = new DeployOptions();
                    deployOptions.setPerformRetrieve(false);
                    deployOptions.setRollbackOnError(true);
                    //deployOptions.setPurgeOnDelete(true);
                    //deployOptions.setRunAllTests(true);
                    asyncResult = targetMetaConn.deploy(result.getZipFile(), deployOptions);
                    asyncResultId = asyncResult.getId();

                    DeployResult deployResult = null;
                    boolean fetchDetails;
                    waitTimeMilliSecs = ONE_SECOND;
                    poll = 0;

                    do {
                        if(isCancelled()) {
                            log.info("Deployment cancelled!");
                            targetMetaConn.cancelDeploy(asyncResultId);
                            poll = 3;
                        }
                        
                        Thread.sleep(waitTimeMilliSecs);

                        // Fetch in-progress details once for every 3 polls
                        fetchDetails = (poll % 3 == 0);
                        deployResult = targetMetaConn.checkDeployStatus(asyncResultId, fetchDetails);

                        int componentsDeployed = deployResult.getNumberComponentsDeployed();
                        int componentsTotal = deployResult.getNumberComponentsTotal();
                        int testsCompleted = deployResult.getNumberTestsCompleted();
                        int testsTotal = deployResult.getNumberTestsTotal();

                        String status = String.format("%s components: %d/%d - tests: %d/%d",
                                deployResult.getId(), componentsDeployed,
                                componentsTotal,
                                testsCompleted,
                                testsTotal);
                        updateMessage(status);

                        if (componentsDeployed < componentsTotal) {
                            updateProgress(componentsDeployed, componentsTotal);
                        } else {
                            updateProgress(testsCompleted, testsTotal);
                        }

                        for (DeployMessage dm : deployResult.getDetails().getComponentFailures()) {
                            log.severe("Component failure: " + dm.getProblem());
                            updateMessage("Component failure: " + dm.getProblem());
                        }
                        log.info("Status is: " + deployResult.getStatus() + "\n" + status);
                    } while (!deployResult.isDone());

                    String deployDetails = String.format("Deploy details:"
                            + "\nCompleted:%1$Tm/%1$Te/%1$TY %1$TI:%1$TM %1$Tp"
                            + "\nErrors:%2$s"
                            + "\nDeployed:%3$s"
                            + "\nTotal:%4$s"
                            + "\nTest Errors:%5$s"
                            + "\nCompleted Tests:%6$s"
                            + "\nTests Total:%7$s",
                            deployResult.getCompletedDate(),
                            deployResult.getNumberComponentErrors(),
                            deployResult.getNumberComponentsDeployed(),
                            deployResult.getNumberComponentsTotal(),
                            deployResult.getNumberTestErrors(),
                            deployResult.getNumberTestsCompleted(),
                            deployResult.getNumberTestsTotal());
                    log.info(deployDetails);
                    updateMessage("Deploy ended!");

                    if (!deployResult.isSuccess()) {
                        updateMessage("Deploy failed: " + deployResult.getErrorMessage());
                        log.log(Level.SEVERE, "Deploy failed with code: {0} msg: {1}",
                                new Object[]{deployResult.getErrorStatusCode(), deployResult.getErrorMessage()});
                    }

                    updateProgress(0, 0);
                    return deployDetails;
                } catch (InterruptedException | ConnectionException ex) {
                    log.severe(ex.getMessage());
                    return ex.getMessage();
                }
            }
        };
    }
}
