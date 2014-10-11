/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */

package com.force.deploy.tools;

import java.net.URL;
import java.util.ResourceBundle;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.TextArea;

/**
 *
 * @author Daniel
 */
public class DebugLogController implements Initializable {
    @FXML
    private TextArea rawLog;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rawLog.setText(MainUIController.rawLog);
    }
    
}
