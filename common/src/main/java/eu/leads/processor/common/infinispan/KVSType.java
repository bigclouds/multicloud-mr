package eu.leads.processor.common.infinispan;

/**
 * Created by vagvaz on 5/18/14.
 */
public enum KVSType {
    LOCAL, CLUSTER, ENSEMBLE;

    public static KVSType stringToKVSType(String type) {
        if (type.toUpperCase().equals(LOCAL.toString())) {
            return LOCAL;
        } else if (type.toUpperCase().equals(CLUSTER.toString())) {
            return CLUSTER;
        } else if (type.toUpperCase().equals(ENSEMBLE.toString()))
            return ENSEMBLE;
        else
            return LOCAL;
    }
}


