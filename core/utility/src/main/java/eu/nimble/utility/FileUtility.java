/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package eu.nimble.utility;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.codec.binary.Base64;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author yildiray
 */
public class FileUtility {

    public static Logger log = LoggerFactory.getLogger(FileUtility.class);

    public static void writeToFile(File file, byte[] content) {
        FileOutputStream fos = null;
        try {
            fos = new FileOutputStream(file);
            fos.write(content);
        } catch (FileNotFoundException ex) {
            log.error("", ex);
        } catch (IOException ex) {
            log.error("", ex);
        } finally {
            try {
                fos.close();
            } catch (IOException ex) {
                log.error("", ex);
            }
        }
    }

    public static String readFile(String filePath) {
        try {
            File file = new File(filePath);
            byte[] content = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(content);
            return new String(content);
        } catch (Exception ex) {
            log.error("", ex);
        }
        return null;
    }

    public static String readFile(File file) {
        try {
            byte[] content = new byte[(int) file.length()];
            FileInputStream fis = new FileInputStream(file);
            fis.read(content);
            return new String(content);
        } catch (Exception ex) {
            log.error("", ex);
        }
        return null;
    }

    public static String base64Encode(byte[] compress) {
        byte[] encodedBytes = Base64.encodeBase64(compress);
        return new String(encodedBytes);
    }

    public static byte[] base64Decode(String base64Encoded) {
        byte[] decodedBytes = Base64.decodeBase64(base64Encoded.getBytes());
        return decodedBytes;
    }

}
