package eu.leads.processor.common.test;


/**
 * Infinispan distributed executors demo .
 */
public class LeadsDistMapReduceTest {

  //    transient protected static Random r;
  //
  //    protected static long wordsC = 6000;
  //    private static String[] loc = {"a", "b,", "c", "d", "asa", "aasd", "pp",
  //            "kasd", "gadfa", "aerw", "oead", "ddsfa", "ewrwa", "cvaa", "dfa"};
  //    protected final boolean isMaster;
  //    protected final String cfgFile;
  //    protected final JSAPResult commandLineOptions;
  //    protected InfinispanManager iman;
  //    private String textFile;
  //    private transient Cache<String, String> InCache;
  //    private transient Cache<String, List<Integer>> CollectorCache;
  //    private transient Cache<String, Integer> OutCache;
  //
  //
  //    public LeadsDistMapReduceTest(String[] args) throws Exception {
  //        commandLineOptions = parseParameters(args);
  //        String nodeType = commandLineOptions.getString("nodeType");
  //        isMaster = nodeType != null && nodeType.equals("master");
  //        cfgFile = "/opt/Projects/infinispan/demos/distexec/src/main/release/etc/config-samples/minimal.xml";// leads_test_configuration.xml";//minimal.xml"
  //
  //        r = new Random(0);
  //        textFile = commandLineOptions.getString("textFile");
  //
  //        LQPConfiguration.initialize();
  //        InfinispanCluster cluster = InfinispanClusterSingleton.getInstance()
  //                .getCluster();
  //        iman = cluster.getManager();
  //    }
  //
  //    public static void main(String... args) throws Exception {
  //        new LeadsDistMapReduceTest(args).run();
  //    }
  //
  //    protected String getWord() {
  //        int l = 10;
  //        String result = "";
  //        for (int i = 0; i < l; i++) {
  //            result += loc[r.nextInt(loc.length)];
  //        }
  //        return result;
  //    }
  //
  //    protected String getLine() {
  //        int l = 10;
  //        String result = "";
  //        for (int i = 0; i < l; i++) {
  //            result += " " + getWord();
  //        }
  //        return result;
  //    }
  //
  //    protected JSAPResult parseParameters(String[] args) throws Exception {
  //        SimpleJSAP jsap = buildCommandLineOptions();
  //
  //        JSAPResult config = jsap.parse(args);
  //        if (!config.success() || jsap.messagePrinted()) {
  //            Iterator<?> messageIterator = config.getErrorMessageIterator();
  //            while (messageIterator.hasNext())
  //                System.err.println(messageIterator.next());
  //            System.err.println(jsap.getHelp());
  //            return null;
  //        }
  //
  //        return config;
  //    }
  //
  //    protected Cache<String, String> startCache() throws IOException {
  //        CacheBuilder cb = new CacheBuilder(cfgFile);
  //        EmbeddedCacheManager cacheManager = cb.getCacheManager();
  //        Configuration dcc = cacheManager.getDefaultCacheConfiguration();
  //
  //        cacheManager.defineConfiguration("wordcount",
  //
  //                new ConfigurationBuilder().read(dcc).clustering().l1()
  //                        .disable().clustering().cacheMode(CacheMode.DIST_SYNC)
  //                        .hash().numOwners(1).build());
  //        Cache<String, String> cache = cacheManager.getCache();
  //
  //        Transport transport = cache.getAdvancedCache().getRpcManager()
  //                .getTransport();
  //        if (isMaster)
  //            System.out.printf("Node %s joined as master. View is %s.%n",
  //                    transport.getAddress(), transport.getMembers());
  //        else
  //            System.out.printf("Node %s joined as slave. View is %s.%n",
  //                    transport.getAddress(), transport.getMembers());
  //
  //        return cache;
  //    }
  //
  //    public void run() throws Exception {
  //
  //
  //        // EmbeddedCacheManager manager = new DefaultCacheManager();
  //        // manager.defineConfiguration("InCache", new ConfigurationBuilder()
  //        // // .eviction().strategy(EvictionStrategy.LIRS ).maxEntries(1000)
  //        // .build());
  //        // manager.defineConfiguration("CollatorCache", new
  //        // ConfigurationBuilder()
  //        // // .eviction().strategy(EvictionStrategy.LIRS ).maxEntries(1000)
  //        // .build());
  //        // manager.defineConfiguration("OutCache", new ConfigurationBuilder()
  //        // // .eviction().strategy(EvictionStrategy.LIRS ).maxEntries(1000)
  //        // .build());
  //
  //        InCache = (Cache<String, String>) iman.getPersisentCache("InCache");
  //        CollectorCache = (Cache<String, List<Integer>>) iman
  //                .getPersisentCache("CollectorCache");
  //        OutCache = (Cache<String, Integer>) iman.getPersisentCache("OutCache");
  //
  //
  //        if (textFile != null)
  //            loadData(InCache);
  //
  //
  //        for (long word = 0; word < wordsC; word++)
  //            InCache.put("rndwd" + word, getLine());
  //
  //        try {
  //            if (isMaster) {
  //
  //                DistributedExecutorService des = new DefaultExecutorService(
  //
  //                        InCache);
  //
  //                long start = System.currentTimeMillis();
  //                JsonObject configuration = new JsonObject();
  //                LeadsMapper<String, String, String, Integer> testMapper = new eu.leads.processor.common.test.WCM(
  //
  //                        configuration);
  //                LeadsCollector<String, Integer> testCollector = new LeadsCollector<String, Integer>(
  //                        5000, CollectorCache);
  //                LeadsMapperCallable<String, String, String, Integer> testMapperCAll = new LeadsMapperCallable<String, String, String, Integer>(
  //                        InCache, testCollector, testMapper);
  //
  //                LeadsReducer<String, Integer> testReducer = new eu.leads.processor.common.test.WCR(
  //                        configuration);
  //                LeadsReduceCallable<String, Integer> testReducerCAll = new LeadsReduceCallable<String, Integer>(
  //                        OutCache, testReducer);
  //
  //                System.out.println("InCache Cache Size:" + InCache.size());
  //
  //                Future<List<String>> res = des.submit(testMapperCAll);
  //
  //                if (res.get() != null)
  //                    System.out.println("Mapper Execution is done");
  //                else
  //                    System.out.println("Mapper Execution not done");
  //                System.out.println("testCollector Cache Size:"
  //
  //                        + testCollector.getCache().size());
  //
  //                DistributedExecutorService des_inter = new DefaultExecutorService(
  //                        CollectorCache);
  //                List<Future<Integer>> reducers_res = des_inter
  //                        .submitEverywhere(testReducerCAll);
  //                for (Future<Integer> f : reducers_res) {
  //
  //                    if (f != null)
  //
  //                        if (f.get() != null) {
  //                            System.out.println("Reducer Execution is done");
  //                            // List<Integer> wordCountList = reducer_res.get();
  //                            // System.out.println("result " +
  //                            // wordCountList.toString());
  //                        } else
  //                            System.out.println("Reducer Execution not done");
  //
  //                    System.out.println("Results: OutCache Size"
  //                            + OutCache.size());
  //                }
  //
  //                System.out.printf("%nCompleted in %s%n%n", Util
  //                        .prettyPrintTime(System.currentTimeMillis() - start));
  //            } else {
  //                System.out
  //                        .println("Slave node waiting for Map/Reduce tasks.  Ctrl-C to exit.");
  //                LockSupport.park();
  //                System.out.println("Unparked Doing someting.");
  //
  //            }
  //        } finally {
  //
  //            iman.getCacheManager().stop();
  //
  //        }
  //    }
  //
  //
  //    protected SimpleJSAP buildCommandLineOptions() throws JSAPException {
  //        return new SimpleJSAP("WordCountDemo",
  //                "Count words in Infinispan cache usin MapReduceTask ",
  //                new Parameter[]{
  //                        new FlaggedOption("configFile", JSAP.STRING_PARSER,
  //                                "config-samples/distributed-udp.xml",
  //                                JSAP.NOT_REQUIRED, 'c', "configFile",
  //                                "Infinispan transport config file"),
  //                        new FlaggedOption("nodeType", JSAP.STRING_PARSER,
  //                                "slave", JSAP.REQUIRED, 't', "nodeType",
  //                                "Node type as either master or slave"),
  //                        new FlaggedOption("textFile", JSAP.STRING_PARSER, null,
  //                                JSAP.NOT_REQUIRED, 'f', "textFile",
  //                                "Input text file to distribute onto grid"),
  //                        new FlaggedOption("mostPopularWords",
  //                                JSAP.INTEGER_PARSER, "15", JSAP.NOT_REQUIRED,
  //                                'n', "mostPopularWords",
  //                                "Number of most popular words to find")});
  //    }
  //
  //    private void loadData(Cache<String, String> cache) throws IOException {
  //        FileReader in = new FileReader(textFile);
  //        try {
  //            BufferedReader bufferedReader = new BufferedReader(in);
  //
  //            // chunk and insert into cache
  //            int chunkSize = 10; // 10K
  //            int chunkId = 0;
  //
  //            CharBuffer cbuf = CharBuffer.allocate(1024 * chunkSize);
  //            while (bufferedReader.read(cbuf) >= 0) {
  //                Buffer buffer = cbuf.flip();
  //                String textChunk = buffer.toString();
  //                cache.put(textFile + (chunkId++), textChunk);
  //                cbuf.clear();
  //                if (chunkId % 100 == 0)
  //                    System.out.printf(
  //
  //                            "  Inserted %s chunks from %s into grid%n",
  //                            chunkId, textFile);
  //            }
  //        } finally {
  //            Util.close(in);
  //        }
  //    }
}
