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
import javafx.scene.control.TreeView;

/**
 *
 * @author Daniel
 */
public class DebugLogController implements Initializable {

    @FXML
    private TextArea rawLog;
    @FXML
    private TextArea debugOutput;
    @FXML
    private TreeView<?> logTree;
    @FXML
    private TextArea logItem;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rawLog.setText(MainUIController.rawLog);
        debugOutput.setText(stripDebugOutput(MainUIController.rawLog));
    }

    private String stripDebugOutput(String rawLog) {
        String output = "";

        String[] lines = rawLog.split("\n");
        String[] copy = lines;

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];

            if (line.contains("|DEBUG|")) {
                output += line.substring(line.indexOf("|DEBUG|") + 7, line.length()) + "\r\n";
                for (int j = i + 1; j < lines.length; j++) {
                    String line2 = copy[j];
                    if (line2.contains("|SYSTEM_METHOD_EXIT|")) {
                        i = j + 1;
                        break;
                    } else {
                        output += line2 + "\r\n";
                    }
                }
            }
        }

        return output;
    }

}
