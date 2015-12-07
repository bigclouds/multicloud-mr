package eu.leads.processor.common.utils.storage;

import org.apache.hadoop.security.UserGroupInformation;

import java.security.PrivilegedExceptionAction;
import java.util.Properties;

/**
 * Created by angelos on 20/08/15.
 */
public class PredeployedTest {

    public static void main(String [] args){
        final String user="vagvaz";
        final String jarnewPath = "/tmp/localfile_destination/eu.leads.processor.AdidasProcessingPlugin";
        final String jarnewPath2 = "/tmp/localfile_destination/eu.leads.processor.plugins.sentiment.SentimentAnalysisPlugin";
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
                    * Download
                    * */
                    String hdfsPathPredeployed = "plugins/eu.leads.processor.AdidasProcessingPlugin";
                    String hdfsPath = "plugins/eu.leads.processor.plugins.sentiment.SentimentAnalysisPlugin";
                    if(hdfss.exists(hdfsPath)) {
                        System.out.println("...Downloading");
                        hdfss.download(hdfsPathPredeployed, jarnewPath);
                        hdfss.download(hdfsPath, jarnewPath2);
                    } else{
                        System.out.println("Error occured!");
                    }

                    /*
                    * Checksum MD5
                    * */
                    /*FileInputStream fileInputStream=new FileInputStream(jarnewPath);
                    MD5Hash key = MD5Hash.digest(fileInputStream);
                    System.out.println("MD5 key : "+key);
                    FileInputStream fileInputStream2=new FileInputStream(jarPath);
                    MD5Hash key2 = MD5Hash.digest(fileInputStream2);
                    System.out.println("MD5 key : "+key2);*/
                    return null;
                }

            });

        }catch(Exception e){
            e.printStackTrace();
        }
    }

}
