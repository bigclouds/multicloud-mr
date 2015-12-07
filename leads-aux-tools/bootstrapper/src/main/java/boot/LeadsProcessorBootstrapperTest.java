package boot;

/**
 * Created by vagvaz on 8/21/14.
 */
public class LeadsProcessorBootstrapperTest {
  public static void main(String[] args) {
    String xmlConfiguration = "/tmp/default-conf/boot-configuration.xml"; //boot-configuration.xml
    Boot2.main(new String[] {xmlConfiguration});
  }
}
