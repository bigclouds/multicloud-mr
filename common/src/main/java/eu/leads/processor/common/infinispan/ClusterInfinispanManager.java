package eu.leads.processor.common.infinispan;

import eu.leads.processor.common.LeadsListener;
import eu.leads.processor.common.StringConstants;
import eu.leads.processor.common.utils.PrintUtilities;
import eu.leads.processor.conf.LQPConfiguration;
import eu.leads.processor.core.Tuple;
import org.infinispan.Cache;
import org.infinispan.commands.RemoveCacheCommand;
import org.infinispan.configuration.cache.CacheMode;
import org.infinispan.configuration.cache.Configuration;
import org.infinispan.configuration.cache.ConfigurationBuilder;
import org.infinispan.configuration.cache.Index;
import org.infinispan.configuration.global.GlobalConfiguration;
import org.infinispan.configuration.parsing.ConfigurationBuilderHolder;
import org.infinispan.configuration.parsing.ParserRegistry;
import org.infinispan.context.Flag;
import org.infinispan.distexec.DefaultExecutorService;
import org.infinispan.distexec.DistributedExecutorService;
import org.infinispan.distexec.DistributedTask;
import org.infinispan.distexec.DistributedTaskBuilder;
import org.infinispan.eviction.EvictionStrategy;
import org.infinispan.eviction.EvictionThreadPolicy;
import org.infinispan.factories.ComponentRegistry;
import org.infinispan.factories.GlobalComponentRegistry;
import org.infinispan.jmx.CacheJmxRegistration;
import org.infinispan.lifecycle.ComponentStatus;
import org.infinispan.manager.DefaultCacheManager;
import org.infinispan.manager.EmbeddedCacheManager;
import org.infinispan.persistence.leveldb.configuration.CompressionType;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfiguration;
import org.infinispan.persistence.leveldb.configuration.LevelDBStoreConfigurationBuilder;
import org.infinispan.persistence.manager.PersistenceManager;
import org.infinispan.remoting.transport.Address;
import org.infinispan.server.hotrod.HotRodServer;
import org.infinispan.server.hotrod.configuration.HotRodServerConfigurationBuilder;
import org.infinispan.test.fwk.TestCacheManagerFactory;
import org.infinispan.transaction.TransactionMode;
import org.infinispan.transaction.lookup.JBossStandaloneJTAManagerLookup;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FilenameFilter;
import java.io.IOException;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.infinispan.test.AbstractCacheTest.getDefaultClusteredCacheConfig;



/**
 * Created by vagvaz on 5/23/14.
 */


/**
 * Implementation of InfinispanManager interface This class uses the Distributed Execution of
 * infinispan in order to perform the operations for caches and listeners.
 */
public class ClusterInfinispanManager implements InfinispanManager {


  private Logger log = LoggerFactory.getLogger(this.getClass());
  private EmbeddedCacheManager manager;
  private String configurationFile;
  private HotRodServer server;
  private int serverPort;
  private String host;
  private Configuration defaultConfig = null;
  private Configuration defaultIndexConfig = null;
  private ConfigurationBuilderHolder holder = null;
  private static String uniquePath;
  private String currentComponent;
  private String externalIP =null;
  private int maxEntries;
  private int blockSize = 1;
  private int cacheSize = 64;
  private int indexwriter_ram_buffer_size =64;
  private CompressionType compressionType = CompressionType.NONE;
  private int segments = 54;
  //  private static final EquivalentConcurrentHashMapV8<String, TestResources> testResources = new EquivalentConcurrentHashMapV8<>(AnyEquivalence.getInstance(), AnyEquivalence.getInstance());

  /**
   * Constructs a new ClusterInfinispanManager.
   */
  public ClusterInfinispanManager() {
    host = "0.0.0.0";
    maxEntries = LQPConfiguration.getInstance().getConfiguration().getInt("node.infinispan.maxentries",5000);
    indexwriter_ram_buffer_size =  LQPConfiguration.getInstance().getConfiguration().getInt("hibernate.search.default.indexwriter.ram_buffer_size",64);
    blockSize = LQPConfiguration.getInstance().getConfiguration().getInt("leads.processor.infinispan.leveldb.blocksize",blockSize);
    cacheSize = LQPConfiguration.getInstance().getConfiguration().getInt("leads.processor.infinispan.leveldb.cachesize",cacheSize);
    segments =  LQPConfiguration.getInstance().getConfiguration().getInt("leads.processor.infinispan.segments",54);
    if(LQPConfiguration.getInstance().getConfiguration().getBoolean("leads.processor.infinispan.leveldb.compression",false)){
      compressionType =CompressionType.SNAPPY;
    }
    else{
      compressionType = CompressionType.SNAPPY;
    }

    //    System.out.println("maximum entries are " + maxEntries);
    //    log.error("maximum entries are " + maxEntries);
    serverPort = 11222;
  }

  public ClusterInfinispanManager(EmbeddedCacheManager manager) {
    this.manager = manager;
    maxEntries = LQPConfiguration.getInstance().getConfiguration().getInt("node.infinispan.maxentries",5000);
    blockSize = LQPConfiguration.getInstance().getConfiguration().getInt("leads.processor.infinispan.leveldb.blocksize",blockSize);
    cacheSize = LQPConfiguration.getInstance().getConfiguration().getInt("leads.processor.infinispan.leveldb.cachesize",cacheSize);
    if(LQPConfiguration.getInstance().getConfiguration().getBoolean("leads.processor.infinispan.leveldb.compression",false)){
      compressionType =CompressionType.SNAPPY;
    }
    else{
      compressionType = CompressionType.SNAPPY;
    }
    //    System.out.println("maximum entries are " + maxEntries);
    //    log.error("maximum entries are " + maxEntries);
    initDefaultCacheConfig();
  }

  @Override
  public String getUniquePath() {

    System.out.println("UNIQUE PATH = " + uniquePath);
    return uniquePath;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void setConfigurationFile(String configurationFile) {
    this.configurationFile = configurationFile;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void startManager(String configurationFile)  {

    currentComponent = LQPConfiguration.getInstance().getConfiguration().getString(
        "node.current.component");
    externalIP = LQPConfiguration.getInstance().getConfiguration().getString(StringConstants.PUBLIC_IP);
    if(currentComponent == null)
      currentComponent = "testingComponents-"+UUID.randomUUID();
    uniquePath = resolveUniquePath();
    ParserRegistry registry = new ParserRegistry();
    //    ConfigurationBuilderHolder holder = null;

    try {
      if (configurationFile != null && !configurationFile.equals("")) {
        holder = registry.parseFile(configurationFile);

      } else {
        System.err.println("\n\n\nUSING DEFAULT FILE ERROR\n\n");
        holder = registry.parseFile(StringConstants.ISPN_CLUSTER_FILE);
      }
    }catch(IOException e){
      e.printStackTrace();
    }
    //    GlobalConfigurationBuilder gbuilder = GlobalConfigurationBuilder.defaultClusteredBuilder();
    //    Transport transport = gbuilder.transport().getTransport();
    //    gbuilder.transport().transport(transport);
    //    gbuilder.transport().clusterName("cluster");
    //    TransportFlags transportFlags = new TransportFlags();
    //    transportFlags.withReplay2(false);



    TestCacheManagerFactory.amendMarshaller(holder.getGlobalConfigurationBuilder());
    holder.getDefaultConfigurationBuilder().transaction().transactionManagerLookup(new JBossStandaloneJTAManagerLookup());
    GlobalConfiguration gc = holder.getGlobalConfigurationBuilder().build();
    manager = new DefaultCacheManager(gc, initDefaultCacheConfigBuilder().build(gc), true);
    manager.defineConfiguration("defaultCache",getCacheDefaultConfiguration("defaultCache"));
    manager.getCache("defaultCache");
    manager.getCache();

    //    manager = TestCacheManagerFactory.createClusteredCacheManager(holder.getGlobalConfigurationBuilder(),initDefaultCacheConfigBuilder());
    //    TestCacheManagerFactory.amendGlobalConfiguration(holder.getGlobalConfigurationBuilder(),transportFlags);


    //    manager.getCache();
    //    startHotRodServer(manager,host,serverPort);

    //Join Infinispan Cluster
    //      manager.start();
    //    ConfigurationBuilder builder = getDefaultClusteredCacheConfig(CacheMode.DIST_SYNC, false);
    //    //    builder.read(holder.getGlobalConfigurationBuilder().serialization().marshaller(marshaller).build());
    //    builder.indexing()
    //        .enable()
    //        .index(Index.LOCAL)
    //        .addProperty("default.directory_provider", "filesystem")
    //        .addProperty("hibernate.search.default.indexBase","/tmp/leadsprocessor-data/"+uniquePath+"/infinispan/webpage/")
    //        .addProperty("hibernate.search.default.exclusive_index_use", "true")
    //        .addProperty("hibernate.search.default.indexmanager", "near-real-time")
    //        .addProperty("hibernate.search.default.indexwriter.ram_buffer_size", "128")
    //        .addProperty("lucene_version", "LUCENE_CURRENT");
    //    builder.clustering().l1().disable().clustering().hash().numOwners(1);
    //    builder.jmxStatistics().enable();
    //    builder.transaction().transactionMode(TransactionMode.TRANSACTIONAL)
    //        //            .persistence().passivation(false)
    //        .persistence().passivation(false).addSingleFileStore().location
    //        ("/tmp/leadsprocessor-data/"+uniquePath+"/webpage/")
    //        .fetchPersistentState(false)
    //        .shared(false).purgeOnStartup(false).preload(false).expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(
    //        false);
    //    //    builder.transaction().transactionManagerLookup(new GenericTransactionManagerLookup()).dataContainer().valueEquivalence(AnyEquivalence.getInstance());
    //    Configuration configuration = builder.build();
    //    manager.defineConfiguration("WebPage", configuration);
    //    Cache nutchCache = manager.getCache("WebPage", true);
    if(LQPConfiguration.getConf().getBoolean("processor.start.hotrod"))
    {
      host = LQPConfiguration.getConf().getString("node.ip");

      //      if(!LQPConfiguration.getConf().getString("node.current.component").equals("planner"))
      startHotRodServer(manager,host, serverPort);

    }
    getPersisentCache("clustered");
    //    getPersisentCache("pagerankCache");
    //    getPersisentCache("approx_sum_cache");
    getPersisentCache(StringConstants.STATISTICS_CACHE);
    getPersisentCache(StringConstants.OWNERSCACHE);
    getPersisentCache(StringConstants.PLUGIN_ACTIVE_CACHE);
    getPersisentCache(StringConstants.PLUGIN_CACHE);
    getPersisentCache(StringConstants.QUERIESCACHE);

    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".webpages");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".testpages");

    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".entities");

    //    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".content");
    //    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".page");
    //    Cache uridirCache = (Cache) getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".urldirectory");
    //    Cache uridirCacheEcom = (Cache) getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".urldirectory_ecom");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".page_core");
    //    getInMemoryCache(StringConstants.DEFAULT_DATABASE_NAME + ".page_core.compressed", 4000);
    //    BatchPutListener listener = new BatchPutListener(StringConstants.DEFAULT_DATABASE_NAME+".page_core.compressed",StringConstants.DEFAULT_DATABASE_NAME+".page_core");
    //    addListener(listener, StringConstants.DEFAULT_DATABASE_NAME + ".page_core.compressed");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".keywords");
    //    getInMemoryCache(StringConstants.DEFAULT_DATABASE_NAME + ".keywords.compressed", 4000);
    //    listener = new BatchPutListener(StringConstants.DEFAULT_DATABASE_NAME+".keywords.compressed",StringConstants.DEFAULT_DATABASE_NAME+".keywords");
    //    addListener(listener, StringConstants.DEFAULT_DATABASE_NAME + ".keywords.compressed");
    //    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME+".resourcepart");
    //    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".site");
    //    Cache adidasKeywords = (Cache) getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".adidas_keywords");

    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".rankings");
    //    getInMemoryCache(StringConstants.DEFAULT_DATABASE_NAME + ".rankings.compressed", 4000);
    //    listener = new BatchPutListener(StringConstants.DEFAULT_DATABASE_NAME+".rankings.compressed",StringConstants.DEFAULT_DATABASE_NAME+".rankings");
    //    addListener(listener, StringConstants.DEFAULT_DATABASE_NAME + ".rankings.compressed");
    getPersisentCache(StringConstants.DEFAULT_DATABASE_NAME + ".uservisits");
    //    getInMemoryCache(StringConstants.DEFAULT_DATABASE_NAME + ".uservisits.compressed", 4000);
    //    listener = new BatchPutListener(StringConstants.DEFAULT_DATABASE_NAME+".uservisits.compressed",StringConstants.DEFAULT_DATABASE_NAME+".uservisits");
    //    addListener(listener, StringConstants.DEFAULT_DATABASE_NAME + ".uservisits.compressed");
    //    getPersisentCache("leads.processor.catalog.tablespaces");
    //    getPersisentCache("leads.processor.catalog.databases");
    //    getPersisentCache("leads.processor.catalog.functions");
    //    getPersisentCache("leads.processor.catalog.indexes");
    getPersisentCache("testCache");
    getPersisentCache("input");
    getPersisentCache("testInput");
    getPersisentCache("test");
    getPersisentCache("metrics");
    getPersisentCache("leads.processor.catalog.indexesByColumn");
    getPersisentCache("leads.processor.databases.sub." + StringConstants.DEFAULT_DATABASE_NAME);
    getPersisentCache("batchputTest");
    getPersisentCache("testCache");
    getPersisentCache("input");
    getPersisentCache("testInput");
    getPersisentCache("test");
    getPersisentCache("inputCache");
    getPersisentCache("sampleInput");
    getPersisentCache("gutenberg");
    getInMemoryCache("batchputTest.compressed", 4000);

    BatchPutListener batchPutListener = new BatchPutListener("batchputTest.compressed","batchputTest");
    addListener(batchPutListener, "batchputTest.compressed");

    //    putAdidasKeyWords(adidasKeywords);
    //    putUriDirData(uridirCache);
    //    putUriDirEcomData(uridirCacheEcom);
    getPersisentCache("TablesSize");
    Cache<String, String> allIndexes = (Cache) getPersisentCache("allIndexes");
    for (String column : allIndexes.keySet()) {
      System.out.println("Found index Cache key: " + column + " cacheName: " + allIndexes.get(column));
      getIndexedPersistentCache(allIndexes.get(column));
      getPersisentCache(allIndexes.get(column) + ".sketch");
    }
    //    NutchLocalListener nlistener = new NutchLocalListener(this,"default.webpages",LQPConfiguration.getInstance().getConfiguration().getString("nutch.listener.prefix"),currentComponent);
    //
    //    manager.getCache("WebPage").addListener(nlistener);

    //I might want to sleep here for a little while
    PrintUtilities.printList(manager.getMembers());



    System.out.println("We have started host:" + host);

  }

  private void putUriDirData(Cache uridirCache) {
    //    schema.addColumn("uri", Type.TEXT);
    //    schema.addColumn("ts", Type.INT8);
    //    schema.addColumn("dirassumption", Type.TEXT);
    //    schema.addColumn("ecomassumptionpagesno", Type.TEXT);
    //    schema.addColumn("pagesno", Type.TEXT);
  }

  private void putUriDirEcomData(Cache uridirCacheEcom) {
    //    schema.addColumn("uri", Type.TEXT);
    //    schema.addColumn("ts", Type.INT8);
    //    schema.addColumn("isatbbuttonindir", Type.TEXT);
    //    schema.addColumn("atbbuttonextractionlog", Type.TEXT);
    //    schema.addColumn("nameextractiontuples", Type.TEXT);
    //    schema.addColumn("priceextractiontuples", Type.TEXT);
    //    schema.addColumn("productclustercenter", Type.TEXT);
    //    schema.addColumn("categoryclustercenter", Type.TEXT);
    //    schema.addColumn("productcluster50pcdist", Type.TEXT);
    //    schema.addColumn("productcluster80pcdist", Type.TEXT);
    //    schema.addColumn("categorycluster50pcdist", Type.TEXT);
    //    schema.addColumn("categorycluster80pcdist", Type.TEXT);
    //    schema.addColumn("scalermean", Type.TEXT);
    //    schema.addColumn("scalerstd", Type.TEXT);
  }

  private void putAdidasKeyWords(Cache adidasKeywords) {
    String[] testKeywords = {"adidas","Nike","Puma","Asics","nike mercurial",
        "nike air max",
        "nike air zoom",
        "nike roshe",
        "nike jordan",
        "nike free 5.0",
        "nike lunarglide",
        "ronaldo",
        "adidas supercolor",
        "adidas superstar",
        "adidas supernova boost",
        "adidas terrex",
        "yeezy boost",
        "adidas ultra boost",
        "adidas ace",
        "messi",
        "kanye west",
        "run",
        "football",
        "shoe",
        "adidas",
        "arsenal",
        "pharrell williams"};
    String prefix = "default.adidas_keywords.";
    int counter = 0;
    for(String k : testKeywords){
      Tuple t = new Tuple();
      t.setAttribute(prefix+"id",Integer.toString(counter));
      t.setAttribute(prefix+"inorder",new Boolean(false));
      t.setAttribute(prefix+"distancebetweenwords",3);
      t.setAttribute(prefix+"nonmatchingchars",1);
      t.setAttribute(prefix+"nonmatchingwords",0);
      t.setAttribute(prefix+"keywords",k);
      adidasKeywords.putIfAbsent(prefix+Integer.toString(counter++),t);
    }
  }

  private String resolveUniquePath() {

    if(currentComponent.startsWith("testing")){
      uniquePath = "testing-"+UUID.randomUUID().toString();
    }
    else{
      //      System.err.println("uniquePath: " + uniquePath + " " + "currentComponent " + currentComponent)
      File file = new File("/tmp/leadsprocessor-data/");
      if(file.exists()){
        if(file.isDirectory()){
          File[] files = file.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File dir, String name) {
              if(name.startsWith(currentComponent)){
                return true;
              }
              else {
                return false;
              }
            }
          });
          if(files.length > 0){
            uniquePath = files[0].getName().toString();
          }
          else{
            uniquePath = currentComponent +"-"+UUID.randomUUID().toString();
          }
          //          for(int i = 1; i < files.length;i++){
          //            files[i].delete();
          //          }
          System.out.println("UNIQUE PATH = " + uniquePath);


        }
        else{
          log.error("/tmp/leadsprocessor-data is not a directory deleting...");
          file.delete();
          file.mkdirs();
          file.mkdir();
          uniquePath = currentComponent + "-"+UUID.randomUUID().toString();
        }
      }else{
        uniquePath = currentComponent +"-"+UUID.randomUUID().toString();
      }
    }

    return uniquePath;
  }

  private ConfigurationBuilder initDefaultCacheConfigBuilder() {

    ConfigurationBuilder result = null;
    if (LQPConfiguration.getConf().getBoolean("leads.processor.infinispan.persistence", true)) { //perssistence
      if (!LQPConfiguration.getConf().getBoolean("leads.processor.infinispan.useLevelDB", false)) {
        result = new ConfigurationBuilder();
        GlobalConfiguration gc = holder.getGlobalConfigurationBuilder().build();
        Configuration  c = holder.getDefaultConfigurationBuilder().build(gc);
        result.read(c).clustering()
            .cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering()
            .hash().numOwners(1).numSegments(segments)
            .indexing().index(Index.NONE).transaction().transactionMode(
            TransactionMode.NON_TRANSACTIONAL)
            .persistence().passivation(false)
            //                                                      .addStore(LevelDBStoreConfigurationBuilder.class)
            //                                                                      .location("/tmp/").shared(true).purgeOnStartup(true).preload(false).compatibility().enable()

            .addSingleFileStore().location("/tmp/leadsprocessor-data/" + uniquePath + "/filestore/")
            .fetchPersistentState(false)
            .shared(false).purgeOnStartup(false).preload(false).compatibility().enable()
            .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(
            false).eviction().maxEntries(maxEntries).strategy(EvictionStrategy.LRU).threadPolicy(
            EvictionThreadPolicy.DEFAULT);

      } else { //Use leveldb
        result = new ConfigurationBuilder();
        result.read(holder.getDefaultConfigurationBuilder().build())
            .clustering()
            .cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering()
            .hash().numOwners(1).numSegments(segments)
            .indexing().index(Index.NONE).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
            .persistence().passivation(false)
            .addStore(LevelDBStoreConfigurationBuilder.class)
            .location("/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/data/")
                //                                 .location("/tmp/leveldb/data-foo/" + "/")
            .expiredLocation("/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/expired/")
                //                                 .expiredLocation("/tmp/leveldb/expired-foo" + "/")
            .implementationType(LevelDBStoreConfiguration.ImplementationType.JNI)
            .blockSize(blockSize * 1024 * 1024)
            .compressionType(CompressionType.SNAPPY)
            .cacheSize(cacheSize * 1024 * 1024)
            .fetchPersistentState(false)
            .shared(false).purgeOnStartup(true).preload(false).compatibility().enable()
            .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false).eviction().maxEntries(
            maxEntries).strategy(EvictionStrategy.LRU).threadPolicy(EvictionThreadPolicy.DEFAULT).storeAsBinary()
            //            .locking().concurrencyLevel(1000)
            .build();
      }
    } else { //do not use persistence
      result = new ConfigurationBuilder();
      result.read(holder.getDefaultConfigurationBuilder().build())
          .clustering()
          .cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering()
          .hash().numOwners(1).numSegments(segments)
          .indexing().index(Index.NONE).transaction().transactionMode(
          TransactionMode.NON_TRANSACTIONAL).compatibility().enable()
          .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(
          false)//.eviction().maxEntries(maxEntries).strategy(EvictionStrategy.NONE)
          .build();
    }

    return result;
  }

  public HotRodServer getServer() {
    return server;
  }

  public void setServer(HotRodServer server) {
    this.server = server;
  }

  public int getServerPort() {
    return serverPort;
  }

  public void setServerPort(int serverPort) {
    this.serverPort = serverPort;
  }

  public String getHost() {
    return host;
  }

  public void setHost(String host) {
    this.host = host;
  }

  private void startHotRodServer(EmbeddedCacheManager targetManager, String localhost, int port) {
    log.info("Starting HotRod Server");
    System.err.println("Starting HotRod Server");
    serverPort = port;

    boolean isStarted = false;
    while (!isStarted) {
      server = new HotRodServer();
      HotRodServerConfigurationBuilder serverConfigurationBuilder = new HotRodServerConfigurationBuilder();
      if (externalIP != null && !externalIP.equals("")){
        if(externalIP.contains(":")) {
          String external = externalIP.split(":")[0];
          String portString = externalIP.split(":")[1];
          System.err.println("EXPOSED IP = " + external + ":"+portString);
          serverConfigurationBuilder.host(localhost).port(serverPort).proxyHost(external)
              .proxyPort(Integer.parseInt(portString));
        }
        else{
          System.err.println("EXPOSED IP = " + externalIP + ":" + serverPort);
          serverConfigurationBuilder.host(localhost).port(serverPort).proxyHost(externalIP)
              .proxyPort(serverPort);
        }
      }else{
        System.err.println("NO EXTERNAL IP DEFINES SO EXPOSED IP = " + localhost + ":"+serverPort);
        serverConfigurationBuilder.host(localhost).port(serverPort);
      }
      //.defaultCacheName("defaultCache");
      //                 .keyValueFilterFactory("leads-processor-filter-factory",new LeadsProcessorKeyValueFilterFactory(manager))
      //                 .converterFactory("leads-processor-converter-factory",new LeadsProcessorConverterFactory());

      try {
        //       server = TestHelper.startHotRodServer(manager);

        server.start(serverConfigurationBuilder.build(),targetManager);
        server.addCacheEventConverterFactory("leads-processor-converter-factory",
            new LeadsProcessorConverterFactory());
        server.addCacheEventFilterFactory("leads-processor-filter-factory",
            new LeadsProcessorKeyValueFilterFactory(manager));

        isStarted = true;
      } catch (Exception e) {
        System.out.println("Exception e " + e.getClass().getCanonicalName() + e.getMessage());
        //            if(e == null){
        //               System.out.println(port + " " +)
        //            }
        if(e instanceof  NullPointerException){
          e.printStackTrace();
          server = new HotRodServer();
          serverConfigurationBuilder = new HotRodServerConfigurationBuilder();
          if (externalIP != null && !externalIP.equals("")){
            serverConfigurationBuilder.host(localhost).port(serverPort).proxyHost(externalIP)
                .proxyPort(serverPort);
          }else{
            serverConfigurationBuilder.host(localhost).port(serverPort);
          }
        }
        else{
          System.err.println("Not NullPointerExcepiton");
        }
        serverPort++;
        isStarted = false;
        try {
          Thread.sleep(300);
        } catch (InterruptedException e1) {
          e1.printStackTrace();
        }
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public EmbeddedCacheManager getCacheManager() {
    return this.manager;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void stopManager() {

    for(String cacheName : this.manager.getCacheNames())
      if(manager.isRunning(cacheName)) {
        manager.getCache(cacheName).getAdvancedCache().withFlags(Flag.CACHE_MODE_LOCAL).stop();
        System.out.println("local Cache closed: " + cacheName);
      }
    this.manager.stop();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public ConcurrentMap getPersisentCache(String name) {
    if (manager.cacheExists(name))
      manager.getCache(name);
      //            createCache(name,manager.getDefaultCacheConfiguration());
    else {
      createCache(name, null);
    }
    return manager.getCache(name);
  }

  @Override public ConcurrentMap getInMemoryCache(String name, int inMemSize) {
    if(manager.cacheExists(name)){
      return manager.getCache(name);
    }
    else{
      createInMemoryCache(name,inMemSize);
    }
    return manager.getCache(name);
  }

  private void createInMemoryCache(String cacheName, int inMemSize) {

    DistributedExecutorService des = new DefaultExecutorService(manager.getCache("clustered"));
    List<Future<Void>> list = null;
    DistributedTaskBuilder builder = null;
    builder = des.createDistributedTaskBuilder(new StartCacheCallable(cacheName,true,false,inMemSize));
    builder.timeout(20, TimeUnit.SECONDS);
    DistributedTask task = builder.build();
    list = des.submitEverywhere(task);
    //
    System.out.println(cacheName + " in memory  " + list.size());
    for (Future<Void> future : list) {
      try {
        future.get(); // wait for task to complete
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
      } catch (Exception e){
        e.printStackTrace();
      }
    }
  }


  public Configuration getInMemoryConfiguration(String name, int inMemSize) {
    //do not use persistence

    Configuration config = new ConfigurationBuilder()//.read(manager.getDefaultCacheConfiguration())
        .clustering().l1().disable().clustering().cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering().hash().numOwners(1).numSegments(segments).indexing()
        .index(Index.NONE).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
        .compatibility().enable()//.marshaller(new TupleMarshaller())
        .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(
            false).eviction().maxEntries(inMemSize).strategy(EvictionStrategy.LRU).build();
    return config;
  }



  /**
   * {@inheritDoc}
   */
  @Override
  public ConcurrentMap getPersisentCache(String name, Configuration configuration) {
    if (manager.cacheExists(name))
      manager.getCache(name);
    else {
      createCache(name, configuration);
    }
    return manager.getCache(name);
  }

  @Override
  public ConcurrentMap getIndexedPersistentCache(String name) {
    if (manager.cacheExists(name)){
      return manager.getCache(name);
    }
    return getIndexedPersistentCache(name, getIndexedCacheDefaultConfiguration(name));
  }


  @Override
  public ConcurrentMap getIndexedPersistentCache(String name, Configuration configuration) {
    if(manager.cacheExists(name)) {
      return manager.getCache(name);
    }
    else{

      //      createIndexedCache(name,configuration);
      manager.defineConfiguration(name,configuration);
    }
    return manager.getCache(name);
  }


  private Configuration getDefaultIndexedCacheConfiguration() {
    if(defaultIndexConfig == null){
      initIndexDefaultCacheConfig();
    }
    return defaultIndexConfig;
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removePersistentCache(String name) {
    removeCache(name);
  }

  private void removeCache(String name) {

    System.err.println("------REMOVE " + name);
    DistributedExecutorService des = new DefaultExecutorService(manager.getCache());
    List<Future<Void>> list = des.submitEverywhere(new StopCacheCallable(name));
    for (Future<Void> future : list) {
      try {
        future.get();
      } catch (InterruptedException e) {
        log.error(e.getClass().toString() + " while removing " + name + " " + e.getMessage());
      } catch (ExecutionException e) {
        log.error(e.getClass().toString() + " while removing " + name + " " + e.getMessage());
      }catch (Exception e){
        e.printStackTrace();
      }
    }

    try {
      manager.removeCache(name);
      if (manager.cacheExists(name + ".compressed")) {
        System.err.println("---------REMOVE " + name + " and " + name + ".compressed");
        manager.removeCache(name + ".compressed");
      }

      //      PrintUtilities.printCaches(manager);
    } catch (Exception e) {
      if (manager.isRunning(name)) {
        GlobalComponentRegistry gcr = manager.getGlobalComponentRegistry();
        ComponentRegistry cr = gcr.getNamedComponentRegistry(name);
        if (cr != null) {
          RemoveCacheCommand cmd = new RemoveCacheCommand(name, manager, gcr, cr.getComponent(PersistenceManager.class),
              cr.getComponent(CacheJmxRegistration.class));
          try {
            cmd.perform(null);
          } catch (Throwable throwable) {
            throwable.printStackTrace();
            PrintUtilities.logStackTrace(log, throwable.getStackTrace());
          }
        }
        log.error("Exception while remove " + name + " " + e.getClass().toString() + " " + e.getMessage());
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Object listener, Cache cache) {

    boolean toadd = true;
    if (cache.getCacheManager().cacheExists(cache.getName())) {
      if (listener instanceof LeadsListener) {
        for (Object l : cache.getListeners()) {
          if (l.getClass().getCanonicalName().equals(listener.getClass().getCanonicalName())) {
            toadd = false;
          }
        }
      }
      if (!toadd) {
        return;
      }
    }

    DistributedExecutorService des = new DefaultExecutorService(cache);
    List<Future<String>> list = null;
    DistributedTaskBuilder builder = null;
    builder = des.createDistributedTaskBuilder(new AddListenerCallable(cache.getName(),listener));
    builder.timeout(10, TimeUnit.MINUTES);
    DistributedTask task = builder.build();
    try {
      list =     des.submitEverywhere(task);
    } catch (Exception e) {
      System.out.println(e.getMessage());
    }

    for (Future<String> future : list) {
      try {
        future.get(); // wait for task to complete
      } catch (InterruptedException e) {
        e.printStackTrace();
      } catch (ExecutionException e) {
        e.printStackTrace();
      }catch (Exception e){
        e.printStackTrace();
      }
    }
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void addListener(Object listener, String name) {
    Cache c = (Cache) this.getPersisentCache(name);
    addListener(listener, c);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public void removeListener(Object listener, Cache cache) {
    //    cache.removeListener(listener);
    String listenerId = null;
    if(listener instanceof LeadsListener){
      LeadsListener l = (LeadsListener)listener;
      listenerId = l.getId();
    }
    else if(listener instanceof String){
      String name = (String) listener;
      //      if(name.equals("scan")){
      //        name =  ScanCQLListener.class.toString();
      //      }else if(name.equals("topk-1")){
      //        name =  TopkFirstStageListener.class.toString();
      //      }else if(name.equals("topk-2")){
      //        name =  TopkSecondStageListener.class.toString();
      //      }else if(name.equals("output")){
      //        name = OutputCQLListener.class.toString();
      //      }
      //      else{
      //        System.err.println("Listener unknown " + name);
      //      }
      listenerId = name;
    }

    RemoveListenerCallable callable = new RemoveListenerCallable(cache.getName(),listenerId);
    DistributedExecutorService des = new DefaultExecutorService(cache);

    List<Future<Void>> list = null;
    DistributedTaskBuilder builder = des.createDistributedTaskBuilder(callable);
    builder.timeout(2, TimeUnit.MINUTES);
    DistributedTask task = builder.build();
    des.submitEverywhere(task);
    //
    System.out.println("removed " + listenerId+" from  " + cache.getName());
    log.error("REMOVELISTENER " + listenerId + " from  " + cache.getName());
    for (Future<Void> future : list) {
      try {
        future.get(); // wait for task to complete
      } catch (InterruptedException e) {
      } catch (ExecutionException e) {
      } catch (Exception e){
        e.printStackTrace();
      }
    }

  }

  /**
   * {@inheritDoc}
   */
  @Override public void removeListener(Object listener, String cacheName) {
    Cache cache = (Cache) getPersisentCache(cacheName);
    removeListener(listener, cache);
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public List<Address> getMembers() {
    return manager.getCache("clustered").getAdvancedCache().getRpcManager().getMembers();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public Address getMemberName() {
    return manager.getAddress();
  }

  /**
   * {@inheritDoc}
   */
  @Override
  public boolean isStarted() {
    return manager.getStatus().equals(ComponentStatus.RUNNING);
  }

  @Override public Cache getLocalCache(String cacheName) {
    if(manager.cacheExists(cacheName)){
      return manager.getCache(cacheName);
    }
    Configuration configuration = new ConfigurationBuilder()
        .clustering().l1().disable().clustering().cacheMode(CacheMode.LOCAL)
        .transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
        .persistence().passivation(false)
        .addStore(LevelDBStoreConfigurationBuilder.class)
        .location("/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/data/")
            //                                 .location("/tmp/leveldb/data-foo/" + "/")
        .expiredLocation("/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/expired/")
            //                                 .expiredLocation("/tmp/leveldb/expired-foo" + "/")
        .implementationType(LevelDBStoreConfiguration.ImplementationType.JNI)
        .blockSize(blockSize * 1024 * 1024).compressionType(compressionType)
        .cacheSize(cacheSize * 1024 * 1024)
        .fetchPersistentState(false)
        .shared(false).purgeOnStartup(false).preload(false).compatibility().enable()
        .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false).eviction().maxEntries(maxEntries).strategy(

            EvictionStrategy.LRU).threadPolicy(EvictionThreadPolicy.DEFAULT).storeAsBinary()
            //        .locking().concurrencyLevel(1000)
        .build();
    manager.defineConfiguration(cacheName, configuration);
    Cache startedCache = manager.getCache(cacheName);
    return startedCache;
  }

  private void createCache(String cacheName, Configuration cacheConfiguration) {
    //    if (!cacheConfiguration.clustering().l1().disable().clustering().cacheMode().isClustered()) {
    //      log.warn("Configuration given for " + cacheName
    //          + " is not clustered so using default cluster configuration");
    //      //            cacheConfiguration = new ConfigurationBuilder().clustering().l1().disable().clustering().cacheMode(CacheMode.DIST_ASYNC).async().l1().lifespan(100000L).hash().numOwners(3).build();
    //    }
    //      if(manager.cacheExists(cacheName))
    //         return;
    if(cacheName.equals("clustered")){
      Configuration cacheConfig = getCacheDefaultConfiguration(cacheName);
      manager.defineConfiguration(cacheName,cacheConfig);
      Cache cache = manager.getCache(cacheName);
    }
    else {
      //      manager.defineConfiguration(cacheName,getCacheDefaultConfiguration(cacheName));
      DistributedExecutorService des = new DefaultExecutorService(manager.getCache());
      List<Future<Void>> list = null;
      DistributedTaskBuilder builder = null;
      builder = des.createDistributedTaskBuilder(new StartCacheCallable(cacheName));
      builder.timeout(1, TimeUnit.MINUTES);
      DistributedTask task = builder.build();
      list = des.submitEverywhere(task);
      //
      System.out.println(cacheName+"  " + list.size());
      log.error("LGCREATE "+cacheName+"  " + list.size());
      for (Future<Void> future : list) {
        try {
          future.get(); // wait for task to complete
        } catch (InterruptedException e) {
        } catch (ExecutionException e) {
        }catch (Exception e){
          e.printStackTrace();
        }
      }
    }
  }

  private void createIndexedCache(String name, Configuration configuration) {
    //    manager.defineConfiguration(name,getCacheDefaultConfiguration(name));
    //    DistributedExecutorService des = new DefaultExecutorService(manager.getCache("clustered"));
    //    List<Future<Void>> list = des.submitEverywhere(new StartCacheCallable(name,true));
    //    //
    //    System.out.println(" i "+ name +" " + list.size());
    //    for (Future<Void> future : list) {
    //      try {
    //        future.get(); // wait for task to complete
    //      } catch (InterruptedException e) {
    //      } catch (ExecutionException e) {
    //      }
    //    }
  }


  private void initDefaultCacheConfig() {
    if (LQPConfiguration.getConf().getBoolean("leads.processor.infinispan.persistence", true)) { //perssistence
      if (!LQPConfiguration.getConf().getBoolean("leads.processor.infinispan.useLevelDB", false)) {
        defaultConfig = new ConfigurationBuilder()//.read(manager.getDefaultCacheConfiguration())
            .clustering()
            .cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering()
            .hash().numOwners(1).numSegments(segments)
            .indexing().index(Index.NONE).transaction().transactionMode(
                TransactionMode.NON_TRANSACTIONAL)
            .persistence().passivation(false)
                //                                                      .addStore(LevelDBStoreConfigurationBuilder.class)
                //               .location("/tmp/").shared(true).purgeOnStartup(true).preload(false).compatibility().enable()

            .addSingleFileStore().location("/tmp/leadsprocessor-data/" + uniquePath + "/filestore")
            .fetchPersistentState(false)
            .shared(false).purgeOnStartup(false).preload(false).compatibility().enable()//.marshaller(new TupleMarshaller())
            .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(
                false).eviction().maxEntries(maxEntries).strategy(EvictionStrategy.LRU).threadPolicy(EvictionThreadPolicy.DEFAULT).storeAsBinary()
            .build();

      } else { //Use leveldb
        defaultConfig = new ConfigurationBuilder() //.read(manager.getDefaultCacheConfiguration())
            .clustering()
            .cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering()
            .hash().numOwners(1).numSegments(segments)
            .indexing().index(Index.NONE).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL)
            .persistence().passivation(false)
            .addStore(LevelDBStoreConfigurationBuilder.class)
            .location("/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/data/")
                //                                 .location("/tmp/leveldb/data-foo/" + "/")
            .expiredLocation("/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/expired/")
                //                                 .expiredLocation("/tmp/leveldb/expired-foo" + "/")
            .implementationType(LevelDBStoreConfiguration.ImplementationType.JNI)
            .blockSize(blockSize * 1024 * 1024)
            .compressionType(compressionType)
            .cacheSize(cacheSize * 1024 * 1024)
            .fetchPersistentState(false)
            .shared(false).purgeOnStartup(false).preload(false).compatibility().enable()//.marshaller(new TupleMarshaller())
            .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false).eviction().maxEntries(

                maxEntries).strategy(EvictionStrategy.LRU).threadPolicy(EvictionThreadPolicy.DEFAULT).storeAsBinary()
                //            .locking().concurrencyLevel(1000)
            .build();
      }
    } else { //do not use persistence
      defaultConfig = new ConfigurationBuilder()//.read(manager.getDefaultCacheConfiguration())
          .clustering()
          .cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering()
          .hash().numOwners(1).numSegments(segments)
          .indexing().index(Index.NONE).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).compatibility().enable()//.marshaller(new TupleMarshaller())
          .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)//.eviction().maxEntries(maxEntries).strategy(EvictionStrategy.NONE)
          .build();
    }
  }

  private void initIndexDefaultCacheConfig() {
    //    if(defaultConfig == null){
    //      initDefaultCacheConfig();
    //    }
    defaultIndexConfig = new ConfigurationBuilder() //.read(manager.getDefaultCacheConfiguration())
        .clustering().l1().disable().clustering().cacheMode(CacheMode.LOCAL)
        .indexing().index(Index.LOCAL).transaction().transactionMode(TransactionMode.NON_TRANSACTIONAL).persistence()
        .passivation(false).addStore(LevelDBStoreConfigurationBuilder.class).location(
            "/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/data/")
            //                                 .location("/tmp/leveldb/data-foo/" + "/")
        .expiredLocation("/tmp/leadsprocessor-data/" + uniquePath + "/leveldb/expired/")
            //                                 .expiredLocation("/tmp/leveldb/expired-foo" + "/")
        .implementationType(LevelDBStoreConfiguration.ImplementationType.JNI)
        .blockSize(blockSize * 1024 * 1024).compressionType(compressionType)
        .cacheSize(cacheSize * 1024 * 1024).fetchPersistentState(false).shared(false).purgeOnStartup(false).preload(
            false).compatibility().enable()//.marshaller(new TupleMarshaller())
        .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false).eviction().maxEntries(maxEntries).strategy(
            EvictionStrategy.LRU).threadPolicy(EvictionThreadPolicy.DEFAULT).storeAsBinary()
            //        .locking().concurrencyLevel(1000)
        .build();
    //    defaultIndexConfig =  new ConfigurationBuilder().read(defaultConfig).clustering()
    //        .cacheMode(CacheMode.LOCAL).transaction()
    //        .transactionMode(TransactionMode.NON_TRANSACTIONAL)
    //            .clustering().l1().disable().clustering().persistence().passivation(false).indexing().index(Index.LOCAL).build();
  }



  public Configuration getCacheDefaultConfiguration(String cacheName) {
    Configuration cacheConfig = null;
    if(cacheName.equals("clustered") && cacheName.equals("default")){ //&& intentionally, code fore reference code


      cacheConfig = new ConfigurationBuilder()//.read(manager.getDefaultCacheConfiguration())
          .clustering().cacheMode(CacheMode.DIST_SYNC).storeAsBinary().clustering().hash().numOwners(1).numSegments(segments)
          .indexing().index(Index.LOCAL).transaction().transactionMode(TransactionMode
              .NON_TRANSACTIONAL)
          .persistence()
              //                            .addStore(LevelDBStoreConfigurationBuilder.class
              //                            .location("/tmp/").shared(true).purgeOnStartup(true).preload(false).compatibility().enable()
          .addSingleFileStore().location("/tmp/leadsprocessor-data/" + uniquePath+"/filestore").fetchPersistentState(false).purgeOnStartup(false).shared(false).preload(false).compatibility().enable()//.marshaller(new TupleMarshaller())
          .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)
          .build();
    }
    else{
      if(defaultConfig == null) {
        initDefaultCacheConfig();
      }
      cacheConfig =  defaultConfig;

    }
    return cacheConfig;
  }

  public Configuration getIndexedCacheDefaultConfiguration(String cacheName) {
    Configuration cacheConfig = null;
    if(cacheName.equals("clustered") && cacheName.equals("default")){//&& intentionally, code fore reference code


      cacheConfig = new ConfigurationBuilder()//.read(manager.getDefaultCacheConfiguration())
          .clustering()
          .cacheMode(CacheMode.LOCAL)
          .indexing().setProperty("default.directory_provider", "ram").index(Index.LOCAL).transaction().transactionMode(TransactionMode
              .NON_TRANSACTIONAL)
          .persistence()
              //                            .addStore(LevelDBStoreConfigurationBuilder.class
              //                            .location("/tmp/").shared(true).purgeOnStartup(true).preload(false).compatibility().enable()
          .addSingleFileStore().location("/tmp/leadsprocessor-data/" +uniquePath+"/indexedImpossible")
          .shared(false).preload(false).compatibility().enable()
          .expiration().lifespan(-1).maxIdle(-1).wakeUpInterval(-1).reaperEnabled(false)
          .build();
    }
    else{
      if(defaultIndexConfig == null) {
        initIndexDefaultCacheConfig();
      }

      cacheConfig =  new ConfigurationBuilder().read(defaultIndexConfig).transaction()
          .transactionMode(TransactionMode.NON_TRANSACTIONAL).persistence().passivation(false).indexing().index(Index.LOCAL).addProperty("default.directory_provider", "filesystem")
          .addProperty("hibernate.search.default.indexBase", "/tmp/leadsprocessor-data/" + uniquePath + "/infinispan/" + cacheName + "/")
          .addProperty("hibernate.search.default.exclusive_index_use", "true")
          .addProperty("hibernate.search.default.indexmanager", "near-real-time")
          .addProperty("hibernate.search.default.indexwriter.ram_buffer_size",Integer.toString(indexwriter_ram_buffer_size))
          .addProperty("lucene_version", "LUCENE_CURRENT").build();
      System.out.println(" Indexed Cache Configuration for: " + cacheName );
    }
    return cacheConfig;
  }


}
