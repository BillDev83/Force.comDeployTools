/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.force.deploy.tools.utils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 *
 * @author Daniel
 */
public class Serializer {

    protected static final Logger log = Logger.getLogger(Serializer.class.getName());

    public static void serialize(Object o, String name) {
        ObjectOutputStream out;
        String destination = "./tmp/" + name + ".ser";
        File file = new File(destination);
        try {
            if (!file.getParentFile().exists() && !file.getParentFile().mkdirs()) {
                file.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(destination);
            out = new ObjectOutputStream(fos);
            out.writeObject(o);
            out.close();
            log.log(Level.INFO, "Serialized data is saved in {0}", destination);
        } catch (IOException ex) {
            log.log(Level.SEVERE, null, ex);
        }
    }

    public static Object deserialize(String name) {
        Object o = null;
        String destination = "./tmp/" + name + ".ser";
        File file = new File(destination);
        if (file.exists()) {
            try (FileInputStream fis = new FileInputStream(destination); ObjectInputStream in = new ObjectInputStream(fis)) {
                o = in.readObject();
            } catch (IOException | ClassNotFoundException ex) {
                log.log(Level.SEVERE, null, ex);
            }
        }
        return o;
    }
}
