package eu.leads.processor.core;

import org.bson.BSONObject;
import org.bson.BasicBSONDecoder;
import org.bson.BasicBSONEncoder;
import org.infinispan.commons.io.ByteBuffer;
import org.infinispan.commons.io.ByteBufferImpl;
import org.infinispan.commons.marshall.BufferSizePredictor;
import org.infinispan.commons.marshall.Marshaller;

import java.io.*;
import java.util.Arrays;

/**
 * Created by tr on 22/4/2015.
 */
public class TupleMarshaller implements Marshaller {
  @Override public byte[] objectToByteBuffer(Object obj, int estimatedSize) throws IOException, InterruptedException {
    return objectToByteBuffer(obj);
  }

  @Override public byte[] objectToByteBuffer(Object obj) throws IOException, InterruptedException {
    ByteArrayOutputStream baos = new ByteArrayOutputStream();
    ObjectOutputStream oos = new ObjectOutputStream(baos);
    if (obj instanceof Tuple) {

      Tuple t = (Tuple) obj;
      BasicBSONEncoder encoder = new BasicBSONEncoder();
      byte[] array1 = encoder.encode(t.asBsonObject());
      //                byte[] array = Base64.encode(array1);
      oos.writeObject(array1);
    } else {
      oos.writeObject(obj);
    }
    return baos.toByteArray();
  }

  @Override public Object objectFromByteBuffer(byte[] buf) throws IOException, ClassNotFoundException {
    Object oo = null;
    try {
      byte[] b = Arrays.copyOfRange(buf, 3, buf.length);
      ByteArrayInputStream bais = new ByteArrayInputStream(b);

      ObjectInputStream ois = new ObjectInputStream(bais);
      oo = ois.readObject();
    } catch (Exception e) {
      if (e instanceof StreamCorruptedException) {
        String probKey = new String(buf);
        return probKey;
      }
    }
    try {

      if (oo instanceof byte[]) {
        byte[] array = (byte[]) oo;
        //            String tmpBytes =  (String) oo;
        //            byte[] array = Base64.decode(tmpBytes);
        BasicBSONDecoder decoder = new BasicBSONDecoder();
        BSONObject data = decoder.readObject(array);
        Tuple result = new Tuple(data.toString());
        return result;
      } else {
        return oo;
      }
    } catch (Exception e) {
      e.printStackTrace();
      System.out.println("Returning.... for " + oo.toString());
      return oo;

    }
  }
  //        throw new IOException("cannot unmarshall");
  //    }

  @Override public Object objectFromByteBuffer(byte[] buf, int offset, int length)
      throws IOException, ClassNotFoundException {
    if (offset != 0 || length != buf.length) {
      return objectFromByteBuffer(Arrays.copyOfRange(buf, offset, length));
    }
    return objectFromByteBuffer(buf);
  }

  @Override public ByteBuffer objectToBuffer(Object o) throws IOException, InterruptedException {
    byte[] buf = objectToByteBuffer(o);
    return new ByteBufferImpl(buf, 0, buf.length);
  }

  @Override public boolean isMarshallable(Object o) throws Exception {
    return (o instanceof Tuple) || (o instanceof Serializable);
  }

  @Override public BufferSizePredictor getBufferSizePredictor(Object o) {
    return null;
  }
}
