package eu.leads.processor.core.comp;

/**
 * Created by vagvaz on 7/13/14.
 */
public class ProcessorFactory {

  public ProcessorFactory() {
  }


  public static String getProcessorClassName(String componentType) {
    String result = "eu.leads.processor.core.DefaultProcessor";
    return result;
  }
}
