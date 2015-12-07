package eu.leads.processor.common.infinispan.comm;

import java.util.HashMap;
import java.util.Map;

/**
 *
 * @author vagvaz
 * @author otrack
 *
 * Created by vagvaz on 7/5/14.
 * The coordinator that maintains the global sum.
 * And computes the localConstrains given to the worker nodes
 *
 */
public class Coordinator extends InfinispanComNode {

    Map<String,Integer> localValues;   //the local values of all worker nodes
    //Map<String,Constrain> constrains;  //the local constrains of all worker nodes
    int globalSum;                     //global sum
    ComChannel channel;                //communication medium

    public Coordinator(ComChannel com) {
        super(InfinispanComNode.COORDINATOR,com);
        localValues = new HashMap<String, Integer>();
        //constrains = new HashMap<String, Constrain>();
        this.channel = com;
    }

    /**
     * Getter for property 'localValues'.
     *
     * @return Value for property 'localValues'.
     */
    public Map<String, Integer> getLocalValues() {
        return localValues;
    }

    /**
     * Setter for property 'localValues'.
     *
     * @param localValues Value to set for property 'localValues'.
     */
    public void setLocalValues(Map<String, Integer> localValues) {
        this.localValues = localValues;
        recomputeValue();
    }

    /**
     * Getter for property 'constrains'.
     *
     * @return Value for property 'constrains'.
     */
    /*public Map<String, Constrain> getConstrains() {
        return constrains;
    }*/

    /**
     * Setter for property 'constrains'.
     *
     * @param constrains Value to set for property 'constrains'.
     */
    /*public void setConstrains(Map<String, Constrain> constrains) {
        this.constrains = constrains;
    }*/

    /**
     * Getter for property 'globalSum'.
     *
     * @return Value for property 'globalSum'.
     */
    public int getGlobalSum() {
        return globalSum;
    }

    /**
     * Setter for property 'globalSum'.
     *
     * @param globalSum Value to set for property 'globalSum'.
     */
    public void setGlobalSum(int globalSum) {
        this.globalSum = globalSum;
    }

    //Receive message from worker. This is called only when there is a violation on a worker node
    @Override
    public void receiveMessage(InfinispanComMessage msg) {
        if (msg.getType().equals("update")) {
            localValues.put(msg.getFrom(), (Integer) msg.getBody());
            //Compute new global sum
            recomputeValue();
           /* //Compute constrains
            computeConstrains();
            //Send Constrains to workers
            sendConstrains();*/
        } else if (msg.getType().equals("requestGlobalSum")) {
            channel.broadCast(new InfinispanComMessage(InfinispanComNode.COORDINATOR, "receiveGlobalSum", globalSum));
            //System.out.println("GLOBAL SUM: "+globalSum);
        }
        else {
            throw new RuntimeException("Invalid message");
        }
    }

    //recompute global sum
    private void recomputeValue() {
        this.globalSum = 0;
        //Iterate over local values and sum to compute the global sum globalSum
        for(Map.Entry<String,Integer> entry : localValues.entrySet()){
            this.globalSum += entry.getValue();
        }
    }

    //compute constrains
    /*private void computeConstrains() {
        //Compute the drift each constrain will be equal to
        // localValue - drift...localValue + drift
        int drift = (int) Math.ceil(0.1*this.globalSum)/localValues.size();
        for(Map.Entry<String,Integer> entry : localValues.entrySet()){

            //get current local globalSum;
            int localValue = entry.getValue();
            //put new constrain to map
            constrains.put(entry.getKey(),new Constrain(localValue-drift,localValue+drift));
        }
    }*/

    //Send constrains back to workers
    /*private void sendConstrains() {
        //Send to each worker node the new constrain
        for ( Map.Entry<String, Constrain> entry : constrains.entrySet() ) {
            channel.sentTo(entry.getKey(), new Message(Node.COORDINATOR, "constrain", entry.getValue()));
        }
    }*/


}
