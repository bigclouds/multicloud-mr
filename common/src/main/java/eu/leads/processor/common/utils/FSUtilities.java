package eu.leads.processor.common.utils;

import com.google.common.base.Strings;
import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;

/**
 * Created by vagvaz on 6/3/14.
 */
public class FSUtilities {
    public static void flushPluginToDisk(String filename, byte[] data) {
        flushToDisk(System.getProperty("java.io.tmpdir") + "/leads/plugins/" + filename, data);
    }

    public static void flushToDisk(String path, byte[] data) {
        File f = new File(path);
        f.getParentFile().mkdirs();
      try {
        f.createNewFile();
      } catch (IOException e) {
        e.printStackTrace();
      }
      try {
            FileOutputStream os = new FileOutputStream(f);
            OutputStreamWriter writer = new OutputStreamWriter(os);
            os.write(data);
            writer.close();
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static byte[] loadBytesFromFile(String filename) {
        byte[] result = null;
        if (!Strings.isNullOrEmpty(filename)) {
            File file = new File(filename);
            try {
                result = Files.readAllBytes(Paths.get(filename));
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return result;
    }

    public static void flushToTmpDisk(String path, byte[] filebytes) {
        FSUtilities.flushToDisk(path, filebytes);
    }

    public static byte[] getBytesFromConfiguration(XMLConfiguration config) {
        byte[] result = null;
        ByteArrayOutputStream stream = new ByteArrayOutputStream();
        try {
            config.save(stream);
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }
        result = stream.toByteArray();
        return result;
    }
}
