package eu.leads.processor.common.continuous;

import eu.leads.processor.plugins.EventType;

import java.io.IOException;
import java.io.ObjectStreamException;
import java.io.Serializable;

/**
 * Created by vagvaz on 10/5/15.
 */
public class EventTriplet implements Serializable {
  EventType type;
  Object key;
  Object value;

  public EventTriplet(EventType type, Object key, Object value){
    this.type = type;
    this.key = key;
    this.value = value;
  }
  public EventType getType() {
    return type;
  }

  public void setType(EventType type) {
    this.type = type;
  }

  public Object getKey() {
    return key;
  }

  public void setKey(Object key) {
    this.key = key;
  }

  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

   private void writeObject(java.io.ObjectOutputStream out)   throws IOException {
     out.writeInt(type.getValue());
     out.writeObject(key);
     out.writeObject(value);
   }
   private void readObject(java.io.ObjectInputStream in)
       throws IOException, ClassNotFoundException{
     int typeInt = in.readInt();
     switch (typeInt){
       case 1:
         type = EventType.CREATED;
         break;
       case 2:
         type = EventType.MODIFIED;
         break;
       case 3:
         type = EventType.REMOVED;
         break;
       default:
         type = EventType.CREATED;
         break;
     }
     key = in.readObject();
     value = in.readObject();
   }
   private void readObjectNoData()
       throws ObjectStreamException {
     type = null;
     key =null;
     value = null;
   }
}
