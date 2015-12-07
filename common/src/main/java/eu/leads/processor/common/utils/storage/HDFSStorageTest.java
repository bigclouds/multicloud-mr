package eu.leads.processor.common.utils.storage;

import org.apache.hadoop.io.MD5Hash;
import org.apache.hadoop.security.UserGroupInformation;

import java.io.FileInputStream;
import java.security.PrivilegedExceptionAction;
import java.util.Properties;

/**
 * Created by angelos on 22/03/15.
 */

public class HDFSStorageTest {

    public static void main(String [] args){
        final String user="vagvaz";
        final String jarPath = "/tmp/test.jar";
        final String jarnewPath = "/tmp/test2.jar";
        try {
            UserGroupInformation ugi = UserGroupInformation.createRemoteUser(user);
            ugi.doAs(new PrivilegedExceptionAction<Void>() {

                public Void run() throws Exception {
                    Properties c = new Properties();
                    c.setProperty("hdfs.url", "hdfs://snf-618466.vm.okeanos.grnet.gr:8020");
                    c.setProperty("fs.defaultFS", "hdfs://snf-618466.vm.okeanos.grnet.gr:8020");
                    c.setProperty("fs.hdfs.impl", org.apache.hadoop.hdfs.DistributedFileSystem.class.getName());
                    c.setProperty("fs.file.impl", org.apache.hadoop.fs.LocalFileSystem.class.getName());
                    c.setProperty("prefix", "/user/vagvaz/");
                    c.setProperty("hdfs.user",user);

                    c.setProperty("postfix","0");

                    HDFSStorage hdfss = new HDFSStorage();
                    boolean init = hdfss.initialize(c);
                    hdfss.setConfiguration(c);
                    System.out.println("hdfss.initialize : "+init);
                    System.out.println("hdfss.getConfiguration() : "+hdfss.getConfiguration());
                    System.out.println("hdfss.getStorageType() : "+hdfss.getStorageType());


                    /*
                    upload my.jar and break into pieces
                     */

                    String hdfsPath = "jars1/my.jar";
//                    try {
//                        BufferedInputStream input = new BufferedInputStream(new FileInputStream(jarPath));
//                        byte[] buffer;
//                        int size = input.available();
//                        int counter = -1;
//                        while( size > 0){
//                            counter++;
//                            if(size > 1024*1024*10)
//                            {
//                                buffer = new byte[1024*1024*10];
//                            }
//                            else{
//                                buffer = new byte[size];
//                            }
//                            input.read(buffer);
//
//
//                            if(!hdfss.writeData(hdfsPath + "/" + counter,buffer)) {
//                                System.out.println("Data could not be writed!");
//                                return null;
//                            }
//                            System.out.println(hdfsPath+"/"+counter);
//                            size = input.available();
//                        }
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    } catch (IOException e) {
//                        e.printStackTrace();
//                    }


                    /*
                    download
                     */
                    if(hdfss.exists(hdfsPath)) {
                        System.out.println("...Downloading");
                        hdfss.download("/" + hdfsPath, jarnewPath);
                    } else{
                        System.out.println("Error occured!");
                    }

                    /*
                    * Checksum MD5*/

                    FileInputStream fileInputStream=new FileInputStream(jarnewPath);
                    MD5Hash key = MD5Hash.digest(fileInputStream);
                    System.out.println("MD5 key : "+key);

                    FileInputStream fileInputStream2=new FileInputStream(jarPath);
                    MD5Hash key2 = MD5Hash.digest(fileInputStream2);
                    System.out.println("MD5 key : "+key2);

                    // exists
                    return null;
                }

            });

        }catch(Exception e){
            e.printStackTrace();
        }
    }
}