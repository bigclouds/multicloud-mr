package eu.leads.processor.common.infinispan;

import eu.leads.processor.conf.LQPConfiguration;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Created by vagvaz on 5/17/14.
 */


/**
 * This Class abstracts the interface for an Infinsipan cluster right now there is no functionality
 * implemented
 */
public class InfinispanCluster {

    private static volatile boolean isStarted = false;
    private Logger log = LoggerFactory.getLogger(this.getClass());
    private InfinispanManager manager;

    public InfinispanCluster(InfinispanManager manager) {
        this.manager = manager;

    }

    public void initialize() {
        if (!manager.isStarted()) {
            manager.startManager("conf/" + LQPConfiguration.getConf()
                                               .getString("processor.infinispan.file"));
            this.isStarted = true;
        }
    }

    /**
     * Getter for property 'manager'.
     *
     * @return Value for property 'manager'.
     */
    public InfinispanManager getManager() {
        return manager;
    }

    public void shutdown() {
        if (manager.isStarted()) {
            manager.stopManager();
        }
        manager = null;
    }


}
