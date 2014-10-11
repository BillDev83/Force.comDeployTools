/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools;

import com.force.deploy.tools.utils.Project;
import com.force.deploy.tools.utils.Serializer;
import java.net.URL;
import java.util.HashMap;
import java.util.ResourceBundle;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextField;
import javafx.stage.Stage;

/**
 * FXML Controller class
 *
 * @author Daniel
 */
public class CredentialsController implements Initializable {

    private TextField name;
    private TextField username;
    private TextField password;
    private TextField securityToken;
    private ComboBox<String> environment;
    private TextField instance;
    private Label lblInstance;

    /**
     * Initializes the controller class.
     */
    @Override
    public void initialize(URL url, ResourceBundle rb) {
        environment.getItems().addAll(
                "Production/Developer Edition",
                "Sandbox",
                "Pre-release",
                "Other (Specify)");

        environment.setOnAction(new EventHandler<ActionEvent>() {

            @Override
            public void handle(ActionEvent event) {
                String selected = environment.getSelectionModel().getSelectedItem();
                if ("Other (Specify)".equals(selected)) {
                    instance.setVisible(true);
                    lblInstance.setVisible(true);
                } else {
                    instance.setVisible(false);
                    lblInstance.setVisible(false);
                    instance.setText("");
                }
            }
        });

        if (MainUIController.selectedProject != null) {
            Project project = MainUIController.selectedProject;
            name.setText(project.name);
            name.setEditable(false);
            username.setText(project.username);
            password.setText(project.password);
            securityToken.setText(project.securityToken);
            environment.setValue(project.environment);
            if ("Other (Specify)".equals(project.environment)) {
                instance.setText(project.instance);
                instance.setVisible(true);
                lblInstance.setVisible(true);
            }
        }
    }

    private void btnSaveAction(ActionEvent event) {
        Project project = new Project(name.getText(), username.getText(), password.getText(),
                securityToken.getText(), environment.getValue(), instance.getText());
        HashMap<String, Project> saved = (HashMap<String, Project>) Serializer.deserialize(Project.PROJECT_REPOSITORY);
        if (saved == null) {
            saved = new HashMap<>();
        }
        saved.put(project.name, project);
        if (!MainUIController.projectItems.contains(project.name)) {
            MainUIController.projectItems.add(project.name);
        }
        Serializer.serialize(saved, Project.PROJECT_REPOSITORY);

        Node node = (Node) event.getSource();
        Stage stage = (Stage) node.getScene().getWindow();
        stage.close();
    }

    private void btnCancelAction(ActionEvent event) {
        Node node = (Node) event.getSource();
        Stage stage = (Stage) node.getScene().getWindow();
        stage.close();
    }
}
