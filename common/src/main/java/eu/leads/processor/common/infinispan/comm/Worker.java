package eu.leads.processor.common.infinispan.comm;


/**
 *
 * @author vagvaz
 * @author otrack
 *
 * Created by vagvaz on 7/5/14.
 * The worker node tracks updates on a stream
 * and maintains a local sum of the updates
 * when an update violates the its constrain,
 * the worker informs the coodinator.
 */
public class Worker extends InfinispanComNode {

    private String id;     //node id
    private int localValue; //local sum

    private int latestReceivedGlobalSum;

    private int prevReported;

    // private Constrain constrain; //worker constrain
//    private ComChannel channel;  //communication channel.

    public Worker(String ID, int initialValue/*, Constrain c*/, ComChannel com) {
        super(ID,com);
        this.id = ID;
        localValue = initialValue;
        prevReported = 0;
        // constrain =  c;
//        this.channel = com;

        latestReceivedGlobalSum = 0;
    }


    public boolean update(int newvalue){
      return false;
    }

    public int getLatestReceivedGlobalSum() {
        return latestReceivedGlobalSum;
    }

    public void setLatestReceivedGlobalSum(int latestReceivedGlobalSum) {
        this.latestReceivedGlobalSum = latestReceivedGlobalSum;
    }

//    public ComChannel getChannel() {
//        return channel;
//    }
//
//    public void setChannel(ComChannel channel) {
//        this.channel = channel;
//    }

    public int getPrevReported() {
        return prevReported;
    }

    public void setPrevReported(int prevReported) {
        this.prevReported = prevReported;
    }
    /**
     * Getter for property 'localValue'.
     *
     * @return Value for property 'localValue'.
     */
    public int getLocalValue() {
        return localValue;
    }

    /**
     * Setter for property 'localValue'.
     *
     * @param localValue Value to set for property 'localValue'.
     */
    public void setLocalValue(int localValue) {
        this.localValue = localValue;
    }

    /**
     * Getter for property 'id'.
     *
     * @return Value for property 'id'.
     */
    public String getId() {
        return id;
    }

    /**
     * Setter for property 'id'.
     *
     * @param id Value to set for property 'id'.
     */
    public void setId(String id) {
        this.id = id;
    }

    /**
     * Getter for property 'constrain'.
     *
     * @return Value for property 'constrain'.
     */
    /*public Constrain getConstrain() {
        return constrain;
    }*/

//    /**
//     * Setter for property 'constrain'.
//     *
//     * @param constrain Value to set for property 'constrain'.
//     */
    /*public void setConstrain(Constrain constrain) {
        this.constrain = constrain;
    }*/

    @Override
    public void receiveMessage(InfinispanComMessage msg) {

        if(msg.getType().equals("receiveGlobalSum")){
            latestReceivedGlobalSum = (Integer) msg.getBody();
        }
        else
            assert(1>2);
        /*Message reply = new Message(id,"reply");
        if(msg.getType().equals("get")){

            msg.
        }
        else
            assert(1>2);*/


        //If the message is a get local values then set the local sum as body to the reply
        /*if(msg.getType().equals("get")){
            reply.setBody(localValue);
            channel.sentTo(COORDINATOR,reply);
        }
        //if the message is a new constrain just update the local constrain.
        else if (msg.getType().equals("constrain")){
            this.constrain = (Constrain) msg.getBody();
        }*/
    }
}
