/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools;

import com.force.deploy.tools.utils.ConnectionsManager;
import com.force.deploy.tools.utils.LogItem;
import com.force.deploy.tools.utils.LogMonitor;
import com.force.deploy.tools.utils.Project;
import com.force.deploy.tools.utils.ResultInfo;
import com.force.deploy.tools.utils.Serializer;
import com.sforce.soap.metadata.AsyncResult;
import com.sforce.soap.metadata.DeleteResult;
import com.sforce.soap.metadata.DeployMessage;
import com.sforce.soap.metadata.DeployOptions;
import com.sforce.soap.metadata.DescribeMetadataObject;
import com.sforce.soap.metadata.DescribeMetadataResult;
import com.sforce.soap.metadata.FileProperties;
import com.sforce.soap.metadata.ListMetadataQuery;
import com.sforce.soap.metadata.Metadata;
import com.sforce.soap.metadata.MetadataConnection;
import com.sforce.soap.metadata.Package;
import com.sforce.soap.metadata.PackageTypeMembers;
import com.sforce.soap.metadata.ReadResult;
import com.sforce.soap.metadata.RetrieveMessage;
import com.sforce.soap.metadata.RetrieveRequest;
import com.sforce.soap.metadata.RetrieveResult;
import com.sforce.soap.metadata.UpsertResult;
import com.sforce.soap.partner.LoginResult;
import com.sforce.soap.partner.PartnerConnection;
import com.sforce.soap.partner.SearchRecord;
import com.sforce.soap.partner.SearchResult;
import com.sforce.soap.tooling.QueryResult;
import com.sforce.soap.tooling.SObject;
import com.sforce.soap.tooling.SoapConnection;
import com.sforce.ws.ConnectionException;
import com.sforce.ws.ConnectorConfig;
import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.ComboBox;
import javafx.scene.control.ContextMenu;
import javafx.scene.control.Label;
import javafx.scene.control.ListView;
import javafx.scene.control.MenuButton;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.TreeItem;
import javafx.scene.control.TreeView;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.input.Clipboard;
import javafx.scene.input.ClipboardContent;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.input.MouseButton;
import javafx.scene.input.MouseEvent;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.StageStyle;

/**
 *
 * @author Daniel
 */
public class MainUIController implements Initializable {

    @FXML
    private ListView<String> projects;
    @FXML
    private TreeView<String> metaSource;
    @FXML
    private TreeView<String> metaTarget;
    @FXML
    private ComboBox<String> target;
    @FXML
    private Label source;
    @FXML
    private Label details;
    @FXML
    private Label statusLabel;
    @FXML
    private ProgressBar progress;
    @FXML
    private TableView<ResultInfo> results;
    @FXML
    private MenuButton loadMeta;
    @FXML
    private TextField userSearch;
    @FXML
    private ListView<String> usersList;
    @FXML
    public TableView<LogItem> debugLogs;

    private static final Logger log = Logger.getLogger(MainUIController.class.getName());

    public static ObservableList<String> projectItems = FXCollections.observableArrayList();
    public static Project selectedProject;

    public static ObservableList<ResultInfo> deployResults = FXCollections.observableArrayList();

    private PartnerConnection part;
    private MetadataConnection meta;
    
    public static ObservableList<String> userItems = FXCollections.observableArrayList();
    public static Map<String, String> userItemsRef = new HashMap<>();
    public static ObservableList<LogItem> logItems = FXCollections.observableArrayList();
    public static Set<String> logItemIds = new TreeSet<>();
    
    public static String rawLog = "";

    @Override
    public void initialize(URL url, ResourceBundle rb) {
        progress.setProgress(0);
        
        initProjects();
        initMetaSource();
        initMetaTarget();
        initResults();
        
        initDebug();
    }
    
    @FXML
    private void btnStopMonitorAction(ActionEvent event) {
        LogMonitor lm = LogMonitor.getInstance();
        lm.shouldStop = true;
        logItems.clear();
    }
    
    @FXML
    private void btnCreateAction(ActionEvent event) {
        deployResults.clear();

        HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);

        MetadataConnection sourceMetaConn = ConnectionsManager.getMetaConnection(saved.get(source.getText()));
        MetadataConnection targetMetaConn = ConnectionsManager.getMetaConnection(saved.get(target.getSelectionModel().getSelectedItem()));

        for (TreeItem<String> parent : metaTarget.getRoot().getChildren()) {
            String parentValue = parent.getValue();
            if (parentValue.startsWith("Apex")) {
                deployApex(parentValue, parent.getChildren(), sourceMetaConn, targetMetaConn);
            } else {
                deployNormal(parentValue, parent.getChildren(), sourceMetaConn, targetMetaConn);
            }
        }
    }
    
    @FXML
    private void btnClearLogsAction(ActionEvent event) {
        logItems.clear();
    }

    @FXML
    private void btnDeleteAction(ActionEvent event) {
        deployResults.clear();
        HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);

        MetadataConnection targetMetaConn = ConnectionsManager.getMetaConnection(saved.get(target.getSelectionModel().getSelectedItem()));

        for (TreeItem<String> parent : metaTarget.getRoot().getChildren()) {
            String parentValue = parent.getValue();
            if (parentValue.startsWith("Apex")) {
                deleteApex(parentValue, parent.getChildren(), targetMetaConn);
            } else {
                deleteNormal(parentValue, parent.getChildren(), targetMetaConn);
            }
        }
    }

    @FXML
    private void btnAddAction(ActionEvent event) {
        try {
            selectedProject = null;
            Parent root = FXMLLoader.load(getClass().getResource("/fxml/Credentials.fxml"));
            Scene scene = new Scene(root);
            final Stage dialog = new Stage();
            dialog.setTitle("Force.com Project");
            dialog.initModality(Modality.APPLICATION_MODAL);
            dialog.initStyle(StageStyle.UTILITY);
            dialog.setScene(scene);
            dialog.showAndWait();
        } catch (IOException ex) {
            log.log(Level.INFO, null, ex);
        }
    }

    private void initResults() {
        results.setEditable(true);
        TableColumn<ResultInfo, String> c1 = new TableColumn<>("Metadata Component");
        c1.setCellValueFactory(new PropertyValueFactory<>("component"));
        c1.setMinWidth(200);
        TableColumn<ResultInfo, String> c2 = new TableColumn<>("Status Code");
        c2.setCellValueFactory(new PropertyValueFactory<>("statusCode"));
        c2.setMinWidth(200);
        TableColumn<ResultInfo, String> c3 = new TableColumn<>("Message");
        c3.setCellValueFactory(new PropertyValueFactory<>("message"));
        c3.setMinWidth(500);
        TableColumn<ResultInfo, String> c4 = new TableColumn<>("Success");
        c4.setCellValueFactory(new PropertyValueFactory<>("success"));
        c4.setMinWidth(73);
        results.getColumns().addAll(c1, c2, c3, c4);
        results.setItems(deployResults);
    }

    private void initLoadMeta() {
        loadMeta.getItems().clear();
        prepareLoad();

        Set<String> thingsToLoad = new TreeSet<>();
        DescribeMetadataResult desc;
        try {
            desc = meta.describeMetadata(30.0);
            for (DescribeMetadataObject obj : desc.getMetadataObjects()) {
                thingsToLoad.add(obj.getXmlName());
                String[] children = obj.getChildXmlNames();
                if (children != null && children.length > 0) {
                    for (String child : Arrays.asList(children)) {
                        if (child != null) {
                            thingsToLoad.add(child);
                        }
                    }
                }
            }
        } catch (ConnectionException ex) {
            log.log(Level.INFO, null, ex);
        }

        List<MenuItem> btnLoadItems = new ArrayList<>();
        final Map<String, String> subFolderItems = new TreeMap<>();
        for (String f : "Dashboard,Document,Report".split(",")) {
            subFolderItems.put(f, f + "Folder");
        }
        subFolderItems.put("EmailTemplate", "EmailFolder");

        for (String folder : thingsToLoad) {
            MenuItem item = new MenuItem(folder);
            item.setOnAction(new EventHandler<ActionEvent>() {

                @Override
                public void handle(ActionEvent event) {
                    try {

                        HashMap<String, TreeSet<String>> props = new HashMap<>();

                        String localFolder = folder;
                        if (subFolderItems.containsKey(folder)) {
                            localFolder = subFolderItems.get(folder);
                        }

                        ListMetadataQuery query = new ListMetadataQuery();
                        query.setType(localFolder);

                        FileProperties[] fp = meta.listMetadata(new ListMetadataQuery[]{query}, 29.0);
                        props.putAll(buildComponents(fp));

                        TreeItem<String> rootItem = new TreeItem<>(localFolder);
                        List<TreeItem<String>> toAdd = new ArrayList<>();
                        for (String type : props.keySet()) {
                            if (subFolderItems.containsKey(folder)) {
                                for (String component : props.get(type)) {
                                    ListMetadataQuery q = new ListMetadataQuery();
                                    q.setFolder(component);
                                    q.setType(folder);
                                    fp = meta.listMetadata(new ListMetadataQuery[]{q}, 29.0);
                                    if (fp.length > 0) {
                                        TreeItem<String> item = new TreeItem<>(component);
                                        List<TreeItem<String>> subItems = new ArrayList<>();
                                        for (String subComponent : buildSubComponents(fp).get(component)) {
                                            TreeItem<String> treeSubItem = new TreeItem<>(subComponent);
                                            subItems.add(treeSubItem);
                                        }
                                        item.getChildren().addAll(subItems);
                                        toAdd.add(item);
                                    }
                                }
                            } else {
                                for (String component : props.get(type)) {
                                    toAdd.add(new TreeItem<>(component));
                                }
                            }
                        }
                        metaSource.getRoot().getChildren().clear();
                        if (!toAdd.isEmpty()) {
                            rootItem.setExpanded(true);
                            rootItem.getChildren().addAll(toAdd);
                            metaSource.getRoot().getChildren().add(rootItem);
                        }
                    } catch (ConnectionException ex) {
                        log.log(Level.INFO, null, ex);
                    }
                }
            });
            btnLoadItems.add(item);
        }
        loadMeta.getItems().addAll(btnLoadItems);
    }

    private void initMetaTarget() {
        metaTarget.setRoot(new TreeItem<>("Target Metadata"));
        metaTarget.setShowRoot(false);

        metaTarget.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                    TreeItem<String> item = metaTarget.getSelectionModel().getSelectedItem();
                    if (item.isLeaf()) {
                        TreeItem<String> parent = item.getParent();
                        parent.getChildren().remove(item);
                        if (parent.getChildren().isEmpty()) {
                            metaTarget.getRoot().getChildren().remove(parent);
                        }
                    }
                }
            }
        });
    }

    private void initMetaSource() {
        metaSource.setRoot(new TreeItem<>("Source Metadata"));
        metaSource.setShowRoot(false);

        metaSource.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (event.getButton().equals(MouseButton.PRIMARY) && event.getClickCount() == 2) {
                    TreeItem<String> item = metaSource.getSelectionModel().getSelectedItem();
                    String itemVal = item.getValue();
                    if (item.isLeaf()) {
                        TreeItem<String> parent = item.getParent();
                        String parentVal = parent.getValue();

                        TreeItem<String> targetRoot = metaTarget.getRoot();
                        boolean parentFound = false;
                        for (TreeItem<String> targetRootChild : targetRoot.getChildren()) {
                            if (targetRootChild.getValue().equals(parentVal)) {
                                parentFound = true;
                                boolean itemFound = false;
                                for (TreeItem<String> targetChildCell : targetRootChild.getChildren()) {
                                    if (targetChildCell.getValue().equals(itemVal)) {
                                        itemFound = true;
                                        break;
                                    }
                                }
                                if (!itemFound) {
                                    targetRootChild.getChildren().add(new TreeItem<>(itemVal));
                                }
                                break;
                            }
                        }
                        if (!parentFound) {
                            TreeItem<String> targetParent = new TreeItem<>(parentVal);
                            targetParent.setExpanded(true);
                            targetParent.getChildren().add(new TreeItem<>(itemVal));
                            targetRoot.getChildren().add(targetParent);
                        }
                    }
                }
            }
        });
    }

    private void initProjects() {
        projects.setItems(projectItems);
        target.setItems(projectItems);
        HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);
        if (saved != null) {
            projectItems.addAll(saved.keySet());
        }
        projects.setOnMouseClicked(new EventHandler<MouseEvent>() {
            @Override
            public void handle(MouseEvent event) {
                try {
                    String name = projects.getSelectionModel().getSelectedItem();
                    HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);
                    selectedProject = saved.get(name);
                    source.setText(name);
                    if (event.getButton().equals(MouseButton.PRIMARY)) {
                        if (event.getClickCount() == 1) {
                            initLoadMeta();
                        }
                    }
                } catch (Exception ex) {
                    log.log(Level.INFO, null, ex);
                }
            }
        });

        ContextMenu contextMenu = new ContextMenu();
        MenuItem openWebLink = new MenuItem("Open Web Link");
        openWebLink.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                try {
                    Project project = selectedProject;

                    String endPoint = ConnectionsManager.getEndpoint(project.environment, project.instance);

                    ConnectorConfig partConfig = new ConnectorConfig();
                    partConfig.setAuthEndpoint(endPoint);
                    partConfig.setServiceEndpoint(endPoint);
                    partConfig.setManualLogin(true);

                    part = new PartnerConnection(partConfig);
                    LoginResult result = part.login(project.username, project.password + project.securityToken);
                    String loginLink = result.getServerUrl();
                    loginLink = loginLink.substring(0, loginLink.indexOf("/", 8)) + "/secur/frontdoor.jsp?sid=" + result.getSessionId();

                    Desktop.getDesktop().browse(new URI(loginLink));
                } catch (ConnectionException | IOException | URISyntaxException ex) {
                    log.log(Level.INFO, null, ex);
                }
            }
        });
        MenuItem copyWebLink = new MenuItem("Copy Web Link");
        copyWebLink.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                try {
                    Project project = selectedProject;

                    String endPoint = ConnectionsManager.getEndpoint(project.environment, project.instance);

                    ConnectorConfig partConfig = new ConnectorConfig();
                    partConfig.setAuthEndpoint(endPoint);
                    partConfig.setServiceEndpoint(endPoint);
                    partConfig.setManualLogin(true);

                    part = new PartnerConnection(partConfig);
                    LoginResult result = part.login(project.username, project.password + project.securityToken);
                    Clipboard clipboard = Clipboard.getSystemClipboard();
                    ClipboardContent content = new ClipboardContent();
                    String loginLink = result.getServerUrl();
                    loginLink = loginLink.substring(0, loginLink.indexOf("/", 8)) + "/secur/frontdoor.jsp?sid=" + result.getSessionId();
                    content.putString(loginLink);
                    clipboard.setContent(content);
                } catch (ConnectionException ex) {
                    log.log(Level.INFO, null, ex);
                }
            }
        });
        MenuItem logMonitor = new MenuItem("Log Monitor");
        logMonitor.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                try {
                    Project project = selectedProject;

                    SoapConnection toolingConn = ConnectionsManager.getToolingConnection(project);
                    String q = "SELECT Id, Application, DurationMilliseconds, Location, LogLength, "
                            + "LogUserId, Operation, Request, StartTime, Status FROM ApexLog";

                    QueryResult result = toolingConn.query(q);

                    deployResults.clear();

                    for (SObject o : result.getRecords()) {
                        deployResults.add(new ResultInfo(o.getId(), o.toString()));
                    }

                } catch (ConnectionException ex) {
                    log.log(Level.INFO, null, ex);
                }
            }
        });
        MenuItem edit = new MenuItem("Edit");
        edit.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                try {
                    Parent root = FXMLLoader.load(getClass().getResource("/fxml/Credentials.fxml"));
                    Scene scene = new Scene(root);
                    final Stage dialog = new Stage();
                    dialog.setTitle("Force.com Project");
                    dialog.initModality(Modality.APPLICATION_MODAL);
                    dialog.initStyle(StageStyle.UTILITY);
                    dialog.setScene(scene);
                    dialog.showAndWait();
                } catch(IOException e) {
                    log.log(Level.SEVERE, null, e);
                }
            }
        });
        MenuItem delete = new MenuItem("Delete");
        delete.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                projectItems.remove(selectedProject.name);
                HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);
                if (saved != null) {
                    saved.remove(selectedProject.name);
                }
                Serializer.serialize(saved, Project.PROJECT_REPOSITORY);
            }
        });
        contextMenu.getItems().addAll(openWebLink, copyWebLink, logMonitor, edit, delete);

        projects.setContextMenu(contextMenu);
    }

    private void prepareLoad() {
        try {
            progress.setProgress(ProgressBar.INDETERMINATE_PROGRESS);
            Project project = selectedProject;

            String endPoint = ConnectionsManager.getEndpoint(project.environment, project.instance);

            ConnectorConfig config = new ConnectorConfig();
            config.setAuthEndpoint(endPoint);
            config.setServiceEndpoint(endPoint);
            config.setManualLogin(true);

            part = new PartnerConnection(config);
            LoginResult result = part.login(project.username, project.password + project.securityToken);
            config.setServiceEndpoint(result.getServerUrl());
            config.setSessionId(result.getSessionId());
            part = new PartnerConnection(config);

            config.setServiceEndpoint(result.getMetadataServerUrl());
            config.setSessionId(result.getSessionId());
            meta = new MetadataConnection(config);
            statusLabel.setText("Login successful!");
        } catch (ConnectionException ex) {
            log.log(Level.INFO, null, ex);
            statusLabel.setText(ex.getMessage());
        }
        progress.setProgress(0);
    }

    public HashMap<String, TreeSet<String>> buildComponents(FileProperties[] props) {
        HashMap<String, TreeSet<String>> ret = new HashMap<>();
        for (FileProperties prop : props) {
            String type = prop.getType();

            if (!ret.containsKey(type)) {
                ret.put(type, new TreeSet<>());
            }

            if (prop.getNamespacePrefix() != null) {
                log.info(prop.getFullName() + " (NOT SUPPORTED)");
            } else {
                ret.get(type).add(prop.getFullName());
            }
        }
        return ret;
    }

    public HashMap<String, TreeSet<String>> buildSubComponents(FileProperties[] props) {
        HashMap<String, TreeSet<String>> ret = new HashMap<>();
        for (FileProperties prop : props) {
            String[] parts = prop.getFullName().split("/");
            String type = parts[0];
            String value = parts[1];

            if (!ret.containsKey(type)) {
                ret.put(type, new TreeSet<>());
            }

            if (prop.getNamespacePrefix() != null) {
                log.info(value + " (NOT SUPPORTED)");
            } else {
                ret.get(type).add(value);
            }
        }
        return ret;
    }

    

    private void deployNormal(String parentValue, List<TreeItem<String>> parentChildren, MetadataConnection sourceMetaConn, MetadataConnection targetMetaConn) {
        try {
            List<String> children = new ArrayList<>();
            for (TreeItem<String> child : parentChildren) {
                String v = child.getValue();
                children.add(v);
            }

            ReadResult readResult = sourceMetaConn.readMetadata(parentValue, children.toArray(new String[]{}));
            List<Metadata> metadata = new ArrayList<>();
            for (Metadata m : readResult.getRecords()) {
                if (m != null) {
                    metadata.add(m);
                }
            }
            if (!metadata.isEmpty()) {
                UpsertResult[] saveResult = targetMetaConn.upsertMetadata(metadata.toArray(new Metadata[]{}));
                for (UpsertResult sr : saveResult) {
                    deployResults.add(ResultInfo.fromUpsertResult(sr));
                }
            } else {
                deployResults.add(new ResultInfo(parentValue, "Components not read from source!"));
            }
        } catch (ConnectionException ex) {
            deployResults.add(new ResultInfo(parentValue, ex.getMessage()));
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void deployApex(String parentValue, List<TreeItem<String>> parentChildren, MetadataConnection sourceMetaConn, MetadataConnection targetMetaConn) {
        try {
            // one second in milliseconds
            final long ONE_SECOND = 4000;
            // maximum number of attempts to retrieve the results
            final int MAX_NUM_POLL_REQUESTS = 50;
            final double API_VERSION = 31.0;

            List<String> children = new ArrayList<>();
            for (TreeItem<String> child : parentChildren) {
                String v = child.getValue();
                children.add(v);
            }

            PackageTypeMembers ptm = new PackageTypeMembers();
            ptm.setName(parentValue);
            ptm.setMembers(children.toArray(new String[]{}));

            Package p = new Package();
            p.setTypes(new PackageTypeMembers[]{ptm});
            p.setVersion(API_VERSION + "");
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

            do {
                Thread.sleep(waitTimeMilliSecs);
                // Double the wait time for the next iteration
                //waitTimeMilliSecs *= 2;
                if (poll++ > MAX_NUM_POLL_REQUESTS) {
                    throw new Exception("Request timed out. If this is a large set "
                            + "of metadata components, check that the time allowed "
                            + "by MAX_NUM_POLL_REQUESTS is sufficient.");
                }
                result = sourceMetaConn.checkRetrieveStatus(asyncResultId);

                StringBuilder buf = new StringBuilder();
                if (result.getMessages() != null) {
                    for (RetrieveMessage rm : result.getMessages()) {
                        buf.append(rm.getFileName() + " - " + rm.getProblem());
                    }
                }
                if (buf.length() > 0) {
                    System.out.println("Retrieve warnings:\n" + buf);
                    deployResults.add(new ResultInfo(parentValue, buf.toString()));
                }
                if (result.getZipFile() != null) {
                    break;
                }
            } while (true);

            DeployOptions deployOptions = new DeployOptions();
            deployOptions.setPerformRetrieve(false);
            deployOptions.setRollbackOnError(true);
            //deployOptions.setPurgeOnDelete(true);
            //deployOptions.setRunAllTests(true);
            asyncResult = targetMetaConn.deploy(result.getZipFile(), deployOptions);
            asyncResultId = asyncResult.getId();

            com.sforce.soap.metadata.DeployResult deployResult = null;
            boolean fetchDetails;
            waitTimeMilliSecs = ONE_SECOND;
            poll = 0;
            do {
                Thread.sleep(waitTimeMilliSecs);
                // double the wait time for the next iteration
                //waitTimeMilliSecs *= 2;

                // Fetch in-progress details once for every 3 polls
                fetchDetails = (poll % 3 == 0);
                deployResult = targetMetaConn.checkDeployStatus(asyncResultId, fetchDetails);
                for (DeployMessage dm : deployResult.getDetails().getComponentFailures()) {
                    deployResults.add(new ResultInfo(parentValue, "Status: " + dm.getProblem()));
                }
                log.info("Status is: " + deployResult.getStatus());

                if (poll++ > MAX_NUM_POLL_REQUESTS) {

                    throw new Exception("Request timed out. If this is a large set "
                            + "of metadata components, check that the time allowed by "
                            + "MAX_NUM_POLL_REQUESTS is sufficient.");
                }
            } while (!deployResult.isDone());
            details.setText(String.format("Details:\nCompleted:%s\nErrors:%s\nDeployed:%s\nTotal:%s\nTest Errors:%s\nCompleted Tests:%s\nTests Total:%s",
                    deployResult.getCompletedDate(), deployResult.getNumberComponentErrors(),
                    deployResult.getNumberComponentsDeployed(),
                    deployResult.getNumberComponentsTotal(),
                    deployResult.getNumberTestErrors(),
                    deployResult.getNumberTestsCompleted(),
                    deployResult.getNumberTestsTotal()));

            if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
                throw new Exception(deployResult.getErrorStatusCode() + " msg: "
                        + deployResult.getErrorMessage());
            }
            if (!deployResult.isSuccess()) {
                throw new Exception("The files were not successfully deployed. " + deployResult.getErrorStatusCode() + " msg: "
                        + deployResult.getErrorMessage());
            }
        } catch (Exception ex) {
            deployResults.add(new ResultInfo(parentValue, ex.getMessage()));
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void deleteApex(String parentValue, List<TreeItem<String>> parentChildren, MetadataConnection targetMetaConn) {
        // one second in milliseconds
        final long ONE_SECOND = 4000;
        // maximum number of attempts to retrieve the results
        final int MAX_NUM_POLL_REQUESTS = 50;
        final double API_VERSION = 31.0;

        File f = new File("./tmp/delete.zip");
        try {
            if (!f.getParentFile().exists() && !f.getParentFile().mkdirs()) {
                f.createNewFile();
            }
            StringBuilder sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n"
                    + "<types>\n");
            for (TreeItem<String> item : parentChildren) {
                sb.append("<members>").append(item.getValue()).append("</members>\n");
            }
            sb.append("<name>").append(parentValue).append("</name>\n"
                    + "</types>\n"
                    + "<version>30.0</version>\n"
                    + "</Package>");

            byte[] data = sb.toString().getBytes();
            final ZipOutputStream out = new ZipOutputStream(new FileOutputStream(f));
            ZipEntry e = new ZipEntry("unpackaged/destructiveChanges.xml");
            out.putNextEntry(e);

            out.write(data, 0, data.length);
            out.closeEntry();

            e = new ZipEntry("unpackaged/package.xml");

            sb = new StringBuilder();
            sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>\n"
                    + "<Package xmlns=\"http://soap.sforce.com/2006/04/metadata\">\n"
                    + "<version>30.0</version>\n"
                    + "</Package>");

            data = sb.toString().getBytes();
            out.putNextEntry(e);
            out.write(data, 0, data.length);
            out.closeEntry();

            out.close();

            int len = (int) f.length();
            FileInputStream fis = new FileInputStream(f);
            byte[] buffer = new byte[len];
            fis.read(buffer);

            DeployOptions deployOptions = new DeployOptions();
            deployOptions.setPerformRetrieve(false);
            deployOptions.setIgnoreWarnings(false);
            deployOptions.setRollbackOnError(true);
            deployOptions.setRunAllTests(true);
            AsyncResult asyncResult = targetMetaConn.deploy(buffer, deployOptions);
            String asyncResultId = asyncResult.getId();

            com.sforce.soap.metadata.DeployResult deployResult = null;
            boolean fetchDetails;
            long waitTimeMilliSecs = ONE_SECOND;
            int poll = 0;
            do {
                Thread.sleep(waitTimeMilliSecs);
                // double the wait time for the next iteration
                //waitTimeMilliSecs *= 2;

                // Fetch in-progress details once for every 3 polls
                fetchDetails = (poll % 3 == 0);

                deployResult = targetMetaConn.checkDeployStatus(asyncResultId, fetchDetails);
                deployResults.add(new ResultInfo(parentValue, "Status: " + deployResult.getStatus()));
                log.info("Status is: " + deployResult.getStatus());

                if (poll++ > MAX_NUM_POLL_REQUESTS) {

                    throw new Exception("Request timed out. If this is a large set "
                            + "of metadata components, check that the time allowed by "
                            + "MAX_NUM_POLL_REQUESTS is sufficient.");
                }
            } while (!deployResult.isDone());

            SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            details.setText(String.format("Details:\nCompleted: %s\nErrors: %s\nDeployed: %s\nTotal: %s\nTest Errors: %s\nCompleted Tests: %s\nTests Total: %s",
                    format.format(deployResult.getCompletedDate().getTime()),
                    deployResult.getNumberComponentErrors(),
                    deployResult.getNumberComponentsDeployed(),
                    deployResult.getNumberComponentsTotal(),
                    deployResult.getNumberTestErrors(),
                    deployResult.getNumberTestsCompleted(),
                    deployResult.getNumberTestsTotal()));

            if (!deployResult.isSuccess() && deployResult.getErrorStatusCode() != null) {
                throw new Exception(deployResult.getErrorStatusCode() + " msg: "
                        + deployResult.getErrorMessage());
            }
            if (!deployResult.isSuccess()) {
                throw new Exception("The files were not successfully deployed. " + deployResult.getErrorStatusCode() + " msg: "
                        + deployResult.getErrorMessage());
            }
        } catch (Exception ex) {
            deployResults.add(new ResultInfo(parentValue, ex.getMessage()));
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void deleteNormal(String parentValue, List<TreeItem<String>> children, MetadataConnection targetMetaConn) {
        try {
            Set<String> items = new TreeSet<>();
            for (TreeItem<String> child : children) {
                items.add(child.getValue());
            }

            DeleteResult[] saveResult = targetMetaConn.deleteMetadata(parentValue, items.toArray(new String[]{}));

            List<ResultInfo> resultsList = new ArrayList<>();
            for (DeleteResult sr : saveResult) {
                resultsList.add(ResultInfo.fromDeleteResult(sr));
            }

            deployResults.addAll(resultsList);
        } catch (ConnectionException ex) {
            deployResults.add(new ResultInfo(parentValue, ex.getMessage()));
            log.log(Level.SEVERE, null, ex);
        }
    }

    private void initDebug() {
        
        userSearch.setOnKeyPressed(new EventHandler<KeyEvent>() {

            @Override
            public void handle(KeyEvent event) {
                if(event.getCode().equals(KeyCode.ENTER)) {
                    userItems.clear();
                    userItemsRef.clear();
                    
                    HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);
                    PartnerConnection partnerConn = ConnectionsManager.getPartnerConnection(saved.get(source.getText()));
                    
                    try {
                        SearchResult sr = partnerConn.search("FIND {"+userSearch.getText()+"*} IN ALL FIELDS RETURNING User (Id, Name)");
                        
                        for(SearchRecord r : sr.getSearchRecords()) {
                            com.sforce.soap.partner.sobject.SObject o = r.getRecord();
                            
                            userItems.add((String) o.getField("Name"));
                            userItemsRef.put((String) o.getField("Name"), (String) o.getField("Id"));
                        }
                    } catch (Exception ex) {
                        log.log(Level.SEVERE, null, ex);
                    }
                }
            }
        });
        
        usersList.setItems(userItems);
        
        debugLogs.setEditable(true);
        TableColumn<LogItem, String> c1 = new TableColumn<>("Duration");
        c1.setCellValueFactory(new PropertyValueFactory<>("duration"));
        c1.setMinWidth(60);
        TableColumn<LogItem, String> c2 = new TableColumn<>("Log Size");
        c2.setCellValueFactory(new PropertyValueFactory<>("logSize"));
        c2.setMinWidth(60);
        TableColumn<LogItem, String> c3 = new TableColumn<>("Location");
        c3.setCellValueFactory(new PropertyValueFactory<>("location"));
        c3.setMinWidth(75);
        TableColumn<LogItem, String> c4 = new TableColumn<>("Request");
        c4.setCellValueFactory(new PropertyValueFactory<>("request"));
        c4.setMinWidth(75);
        TableColumn<LogItem, String> c5 = new TableColumn<>("Operation");
        c5.setCellValueFactory(new PropertyValueFactory<>("operation"));
        c5.setMinWidth(150);
        TableColumn<LogItem, String> c6 = new TableColumn<>("Status");
        c6.setCellValueFactory(new PropertyValueFactory<>("status"));
        c6.setMinWidth(150);
        debugLogs.getColumns().addAll(c1, c2, c3, c4, c5, c6);
        debugLogs.setItems(logItems);
        
        debugLogs.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                //throw new UnsupportedOperationException("Not supported yet."); //To change body of generated methods, choose Tools | Templates.
                if(event.getButton().equals(MouseButton.PRIMARY)) {
                    if(event.getClickCount() == 2) {
                        try {
                            HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);
                            SoapConnection toolingConn = ConnectionsManager.getToolingConnection(saved.get(source.getText()));
                            
                            LogItem item = (LogItem) debugLogs.getSelectionModel().getSelectedItem();
                            rawLog = item.getRawLog(toolingConn.getConfig().getServiceEndpoint(), toolingConn.getConfig().getSessionId());
                            
                            Parent root = FXMLLoader.load(getClass().getResource("/fxml/DebugLog.fxml"));
                            Scene scene = new Scene(root);
                            final Stage dialog = new Stage();
                            dialog.setTitle("Force.com Project");
                            dialog.initModality(Modality.NONE);
                            dialog.initStyle(StageStyle.UTILITY);
                            dialog.setScene(scene);
                            dialog.showAndWait();
                        } catch (Exception ex) {
                            log.log(Level.SEVERE, null, ex);
                        }
                    }
                }
            }
        });
        
        usersList.setOnMouseClicked(new EventHandler<MouseEvent>() {

            @Override
            public void handle(MouseEvent event) {
                if (event.getButton().equals(MouseButton.PRIMARY)) {
                    if(event.getClickCount() == 2) {
                        String userName = usersList.getSelectionModel().getSelectedItem();
                        String uid = userItemsRef.get(userName);
                        
                        HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);
                    
                        SoapConnection toolingConn = ConnectionsManager.getToolingConnection(saved.get(source.getText()));
                        
                        String q = "SELECT Id, Application, Status, Location, Operation, "
                                + "Request, DurationMilliseconds, LogLength FROM ApexLog "
                                + "WHERE LogUserId = '"+uid+"'";
                        
                        logItems.clear();
                        
                        LogMonitor lm = LogMonitor.getInstance(q, toolingConn);
                        lm.monitorUser(uid);
                        if(!lm.isRunning) {
                            lm.start();
                        }
                    }
                }
            }
        });
    }
}
