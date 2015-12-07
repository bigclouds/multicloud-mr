package eu.leads.processor.common.infinispan.comm;

import org.infinispan.Cache;
import org.infinispan.metadata.Metadata;
import org.infinispan.notifications.cachelistener.filter.CacheEventFilter;
import org.infinispan.notifications.cachelistener.filter.EventType;

/**
 *
 * @author vagvaz
 * @author otrack
 *
 * Created by vagvaz on 7/5/14.
 *
 * Communication channel just just a map of String,Node to send messages
 */
public class ComChannel {
  
  private Cache<String,InfinispanComMessage> nodes;

  public ComChannel(Cache<String,InfinispanComMessage> c) {
      nodes =  c;
  }

  // Add a node to the map
  public void register(final String id, InfinispanComNode infinispanComNode){
      CacheEventFilter<String,InfinispanComMessage> filter= new CacheEventFilter<String,InfinispanComMessage>(){

          @Override
          public boolean accept(String key, InfinispanComMessage oldValue, Metadata oldMetadata, InfinispanComMessage newValue, Metadata newMetadata, EventType eventType) {
              return id.equals(key);
          }
      };
      nodes.put(id, InfinispanComMessage.EMPTYMSG); // to create the entry
      nodes.addListener(infinispanComNode, filter, null);

  }

  //Send messsage to node id
  public void sentTo(String id, InfinispanComMessage infinispanComMessage){
    nodes.put(id, infinispanComMessage);
  }

  // Broadcast a message to all nodes, but coordinator
  // The coordinator takes as a result the replies of the nodes
  public void broadCast(InfinispanComMessage infinispanComMessage){
    for(String node: nodes.keySet()){
      if(!node.equals(InfinispanComNode.COORDINATOR)){
          nodes.put(node, infinispanComMessage);
      }
    }
  }

}
