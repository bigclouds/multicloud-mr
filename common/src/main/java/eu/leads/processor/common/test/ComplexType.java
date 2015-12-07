package eu.leads.processor.common.test;

import org.apache.commons.configuration.ConfigurationException;
import org.apache.commons.configuration.XMLConfiguration;

import java.util.HashMap;

/**
 * Created by vagvaz on 6/3/14.
 */
public class ComplexType {
    private String foo;
    private HashMap<String, Integer> m;
    private XMLConfiguration config;

    public ComplexType(String a) {
        System.out.println("Complex Type initialized");
        foo = a;
        m = new HashMap<String, Integer>();
        m.put(foo, 3);
        try {
            config = new XMLConfiguration("/home/vagvaz/jgroups-tcp.xml");
        } catch (ConfigurationException e) {
            e.printStackTrace();
        }

    }

    /**
     * Getter for property 'config'.
     *
     * @return Value for property 'config'.
     */
    public XMLConfiguration getConfig() {
        return config;
    }

    /**
     * Setter for property 'config'.
     *
     * @param config Value to set for property 'config'.
     */
    public void setConfig(XMLConfiguration config) {
        this.config = config;
    }

    /**
     * Getter for property 'm'.
     *
     * @return Value for property 'm'.
     */
    public HashMap<String, Integer> getM() {
        return m;
    }

    /**
     * Setter for property 'm'.
     *
     * @param m Value to set for property 'm'.
     */
    public void setM(HashMap<String, Integer> m) {
        this.m = m;
    }

    /**
     * Getter for property 'foo'.
     *
     * @return Value for property 'foo'.
     */
    public String getFoo() {
        return foo;
    }

    /**
     * Setter for property 'foo'.
     *
     * @param foo Value to set for property 'foo'.
     */
    public void setFoo(String foo) {
        this.foo = foo;
    }
}
