package eu.leads.processor.common.utils;

import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.infinispan.InfinispanClusterSingleton;
import eu.leads.processor.common.infinispan.InfinispanManager;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import org.infinispan.Cache;
import org.infinispan.commons.marshall.Marshaller;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.marshall.core.MarshalledEntryImpl;
import org.infinispan.persistence.leveldb.LevelDBStore;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.persistence.manager.PersistenceManagerImpl;
import org.infinispan.persistence.spi.InitializationContext;
import org.iq80.leveldb.DB;
import org.iq80.leveldb.DBIterator;

import java.io.IOException;
import java.lang.reflect.Field;
import java.util.Map;

public class InfinispanClusterTest  {
    public static void main(String[] args) throws IOException {
        LQPConfiguration.initialize();
       LQPConfiguration.getInstance().getConfiguration().setProperty("node.current.component", "catalog-worker");
       InfinispanManager manager = InfinispanClusterSingleton.getInstance().getManager();
       Cache map = (Cache) manager.getPersisentCache("clustered");
       Cache map2 = (Cache) manager.getPersisentCache("clustered");
//      DB overrideCache = ((LevelDBStore)((PersistenceManagerImpl)((ClusteredCacheLoaderInterceptor)map.getAdvancedCache().getInterceptorChain().get(8)).).getAllLoaders().get(0)).db

      map2.put("1","11");
//               map2.put("1","11");
//               map.put("1","11");
//               map.put("22", "222");
       LQPConfiguration.getInstance().getConfiguration().setProperty("node.current.component", "planner");
       InfinispanManager manager2 = manager;// CacheManagerFactory.createCacheManager();
      Tuple t = new Tuple();
      t.setAttribute("aaaa","dfdsfadfafd");
      t.setAttribute("aaaeeqa","dfdsfadfafd");
      t.setAttribute("aaawerwa","dfdsfadfafd");
      t.setAttribute("aaaewa","dfdsfadfafd");
      t.setAttribute("aaeaa","dfdsfadfafd");
      t.setAttribute("aeaa2a","dfdsfadfafd");
      t.setAttribute("aeaa432a","dfdsfadfafd");
      t.setAttribute("ae343aaa","dfdsfadfafd");
      t.setAttribute("c",t.asJsonObject().copy().toString());
      t.setAttribute("c2",t.asJsonObject().copy().toString());
      t.setAttribute("c4",t.asJsonObject().copy().toString());

      for(int i = 0; i < 10;i++){
        map.put(Integer.toString(i), new Tuple(t.asJsonObject().copy().toString()));
        map2.put(Integer.toString(i), new Tuple(t.asJsonObject().copy().toString()));
        if(i % 50000 == 0 || i < 1024)
        {
          System.out.println("map " + map.getAdvancedCache().getDataContainer().size());
          System.out.println("map2 " + map2.getAdvancedCache().getDataContainer().size());
        }
      }
      System.out.println("Fin ");
      System.in.read();
      System.out.println("map " + map.getAdvancedCache().getDataContainer().size());
      System.out.println("map2 " + map2.getAdvancedCache().getDataContainer().size());

      ComponentRegistry registry = map.getAdvancedCache().getComponentRegistry();
      PersistenceManagerImpl persistenceManager =
          (PersistenceManagerImpl) registry.getComponent(PersistenceManager.class);
      LevelDBStore dbStore = (LevelDBStore) persistenceManager.getAllLoaders().get(0);

      try {
        Field db  = dbStore.getClass().getDeclaredField("db");
        db.setAccessible(true);
        DB realDb = (DB)db.get(dbStore);
        DBIterator it = realDb.iterator();
        it.seekToFirst();
        Field ctxField  = dbStore.getClass().getDeclaredField("ctx");
        ctxField.setAccessible(true);
        InitializationContext ctx = (InitializationContext) ctxField.get(dbStore);
        Marshaller m = ctx.getMarshaller();
        System.out.println("------------------------------------------------------");
        while(it.hasNext()){
          Map.Entry<byte[],byte[]> entry = it.next();
          String key  = (String) m.objectFromByteBuffer(entry.getKey());
          org.infinispan.marshall.core.MarshalledEntryImpl  value =
              (MarshalledEntryImpl) m.objectFromByteBuffer(entry.getValue());

          Tuple tuple =  new Tuple();
          tuple = (Tuple) m.objectFromByteBuffer(value.getValueBytes().getBuf());
          System.out.println("key: " + key + " " + (value != null ? tuple.asJsonObject().encodePrettily() : "null") );
        }
      } catch (NoSuchFieldException e) {
        e.printStackTrace();
      } catch (IllegalAccessException e) {
        e.printStackTrace();
      } catch (ClassNotFoundException e) {
        e.printStackTrace();
      }
      System.out.println("=======================================================");
//       map2 = manager2.getPersisentCache("default.webpages");
//       map = manager2.getPersisentCache(StringConstants.QUERIESCACHE);
//
//               map2.put("2333","223");
//               map.put("d2","22");
//               map.put("2f", "22");
//        PrintUtilities.printMap(map);
//        PrintUtilities.printMap(map2);
      System.in.read();
      manager2.stopManager();
      if(manager != manager2) {
        manager.stopManager();
      }

        System.out.println("Restarting....");
        LQPConfiguration.getInstance().getConfiguration().setProperty("node.current.component",
            "catalog-worker");
        manager.startManager("conf/infinispan-clustered.xml");
        LQPConfiguration.getInstance().getConfiguration().setProperty("node.current.component",
            "planner");

        manager2.startManager("conf/infinispan-clustered.xml");
        map2 = (Cache) manager.getPersisentCache("default.webpages");

        PrintUtilities.printMap(map2);
        map2 = (Cache) manager2.getPersisentCache("default.webpages");
        PrintUtilities.printMap(map2);
        manager.stopManager();
        manager2.stopManager();
        //        InfinispanCluster cluster = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        ////        cluster.initialize();
        //        InfinispanManager man = cluster.getManager();
        //
        //        ConcurrentMap map = man.getPersisentCache("testCache");
        //        map.put("1","11");
        //        map.put("2","22");
        //        InfinispanCluster cluster2 = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        //        PrintUtilities.printMap(map);
        //        ConcurrentMap map2 = cluster2.getManager().getPersisentCache("testCache");
        //        map2.put("3","33");
        //        PrintUtilities.printMap(map2);
        //        System.out.println("cl");
        //        PrintUtilities.printList(cluster.getManager().getMembers());
        //        System.out.println("cl2");
        //        PrintUtilities.printList(cluster2.getManager().getMembers());
    }

    public void testGetManager() throws Exception {
        //        LQPConfiguration.initialize();
        //        InfinispanCluster cluster = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        ////        cluster.initialize();
        //        InfinispanManager man = cluster.getManager();
        //
        //        ConcurrentMap map = man.getPersisentCache("testCache");
        //        map.put("1","11");
        //        map.put("2","22");
        //        InfinispanCluster cluster2 = WeldContext.INSTANCE.getBean(InfinispanCluster.class);
        //
        //        PrintUtilities.printMap(map);
        //        ConcurrentMap map2 = cluster2.getManager().getPersisentCache("testCache");
        //        map2.put("3","33");
        //        PrintUtilities.printMap(map2);
        //        System.out.println("cl");
        //        PrintUtilities.printList(cluster.getManager().getMembers());
        //        System.out.println("cl2");
        //        PrintUtilities.printList(cluster2.getManager().getMembers());
    }
}
