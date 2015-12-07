package eu.leads.processor.common.test;


import eu.leads.processor.infinispan.LeadsMapper;
import org.infinispan.distexec.mapreduce.Collector;
import org.vertx.java.core.json.JsonObject;


public class WCM extends LeadsMapper<String, String, String, Integer> {

  private static final long serialVersionUID = -5943370243108735560L;
  private static int chunks = 0, words = 0;


  public WCM(JsonObject configuration) {
    super(configuration);
  }

  public void map(String key, String value, Collector<String, Integer> c) {
      /*
       * Split on punctuation or whitespace, except for ' and - to catch contractions and hyphenated
       * words
       */
    for (String word : value.split("[\\p{Punct}\\s&&[^'-]]+")) {
      if (word != null) {
        String w = word.trim();
        if (w.length() > 0) {
          c.emit(word.toLowerCase(), 1);
          words++;
        }
      }
    }

    /// if (chunks % 1 == 0)
    //     System.out.printf("Analyzed %s words in %s lines%n", words, chunks);
  }

}
