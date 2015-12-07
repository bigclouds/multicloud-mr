package eu.leads.processor.core;

import eu.leads.processor.conf.LQPConfiguration;
import org.bson.BSONDecoder;
import org.bson.BSONEncoder;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;

import java.util.concurrent.ConcurrentLinkedDeque;

/**
 * Created by vagvaz on 9/24/14.
 */
public class TupleUtils {
  static ConcurrentLinkedDeque<BSONDecoder> decoders;
  static ConcurrentLinkedDeque<BSONEncoder> encoders;
  static Object encCV = new Object();
  static Object decCV = new Object();

  public static void initialize() {
    decoders = new ConcurrentLinkedDeque<>();
    encoders = new ConcurrentLinkedDeque<>();
    int size = LQPConfiguration.getInstance().getConfiguration().getInt("processor.encoders.size", 200);
    for (int i = 0; i < size; i++) {
      decoders.add(new BasicBSONDecoder());
      encoders.add(new BasicBSONEncoder());
    }
  }

  public static BSONDecoder getDecoder() {
//    while (decoders.isEmpty()) {
//      synchronized (decCV) {
//        try {
//          decCV.wait();
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
//      }
//    }
    return decoders.poll();
  }

  public static BSONEncoder getEncoder() {
//    while (encoders.isEmpty()) {
//      synchronized (encCV) {
//        try {
//          encCV.wait();
//        } catch (InterruptedException e) {
//          e.printStackTrace();
//        }
//      }
//    }
    return encoders.poll();
  }

  public static void addDecoder(BSONDecoder decoder) {
    decoders.add(decoder);
//    synchronized (decCV) {
//      decCV.notify();
//    }
  }

  public static void addEncoder(BSONEncoder encoder) {
    encoders.add(encoder);
//    synchronized (encCV) {
//      encCV.notify();
//    }
  }

  public static int compareValues(Object o1, Object o2, String type) {
    int result = 0;
    if (type.startsWith("TEXT")) {
      String v1 = (String) o1;
      String v2 = (String) o2;
      result = v1.compareTo(v2);
    } else if (type.startsWith("BOOLEAN")) {
      Boolean v1 = (Boolean) o1;
      Boolean v2 = (Boolean) o2;
      result = v1.compareTo(v2);
    } else if (type.startsWith("INT")) {
      Number v1 = (Number) o1;
      Number v2 = (Number) o2;
      Long l1 = v1.longValue();
      Long l2 = v2.longValue();
      result = l1.compareTo(l2);
    } else if (type.startsWith("FLOAT") || type.startsWith("DOUBLE")) {
      Number v1 = (Number) o1;
      Number v2 = (Number) o2;
      Double d1 = v1.doubleValue();
      Double d2 = v2.doubleValue();
      result = d1.compareTo(d2);
    }

    return result;
  }
}
