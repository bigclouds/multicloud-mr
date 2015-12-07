package eu.leads.processor.common.infinispan;

import java.io.Serializable;

/**
 * Created by vagvaz on 9/29/14.
 */
public class ProcessorEntry implements Serializable{
    protected String key;
    protected String value;
    public ProcessorEntry(Object key, Object value) {
        this.key = (String)key;
        this.value = (String)value;
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    public String getValue() {
        return value;
    }

    public void setValue(String value) {
        this.value = value;
    }
}
