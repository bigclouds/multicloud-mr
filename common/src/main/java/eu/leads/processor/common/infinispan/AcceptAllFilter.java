package eu.leads.processor.common.infinispan;

import org.infinispan.filter.KeyValueFilter;
import org.infinispan.filter.KeyValueFilterConverter;
import org.infinispan.metadata.Metadata;

import java.io.Serializable;

/**
 * Created by vagvaz on 9/29/14.
 */
public class AcceptAllFilter implements KeyValueFilterConverter, Serializable {

    private long threshold =-1;
    private long counter = -1;
    public AcceptAllFilter(){
        threshold = -1;
        counter = -1;
    }
    public AcceptAllFilter(long threshold){
        this.threshold = threshold;
        counter = 0;
    }
    @Override public boolean accept(Object key, Object value, Metadata metadata) {
        if (threshold < 0) {
            return true;
        }else{
            if(counter < threshold){
                counter++;
                return true;
            }
            else{
                return false;
            }
        }
    }

    @Override public Object filterAndConvert(Object key, Object value, Metadata metadata) {
        if (threshold < 0) {
            return value;
        }else{
            if(counter < threshold){
                counter++;
                return value;
            }
            else{
                return null;
            }
        }
    }

    @Override public Object convert(Object key, Object value, Metadata metadata) {
        return value;
    }
}
