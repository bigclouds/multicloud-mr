package eu.leads.processor.infinispan;

import java.io.IOException;
import java.io.Serializable;

/**
 * Created by vagvaz on 3/7/15.
 */
//@Indexed
public class IndexedComplexIntermediateKey implements Comparable, Serializable {
  //  @Field(index= Index.YES, analyze= Analyze.NO, store= Store.YES)
  private String site;

  //  @Field(index= Index.YES, analyze= Analyze.NO, store= Store.YES)
  private String node;
  //  @Field(index= Index.YES, analyze= Analyze.NO, store= Store.YES,name="key")
  private String key;



  private String cache;

  public IndexedComplexIntermediateKey(String site, String node, String cacheName) {
    this.site = new String(site);
    this.node = new String(node);
    this.cache = new String(cacheName);

  }

  public IndexedComplexIntermediateKey(String site, String node, String cacheName, String key) {
    this.site = new String(site);
    this.node = new String(node);
    this.cache = new String(cacheName);
    this.key = new String(key);

  }

  public IndexedComplexIntermediateKey(IndexedComplexIntermediateKey other) {
    this(other.getSite(), other.getNode(), other.getCache(), other.getKey());
  }

  public IndexedComplexIntermediateKey() {

  }

  public String getSite() {
    return site;
  }

  public void setSite(String site) {
    this.site = site;
  }

  public String getNode() {
    return node;
  }

  public void setNode(String node) {
    this.node = node;
  }

  public String getCache() {
    return cache;
  }

  public void setCache(String cache) {
    this.cache = cache;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  @Override public boolean equals(Object o) {

    if (o == null || getClass() != o.getClass())
      return false;

    IndexedComplexIntermediateKey that = (IndexedComplexIntermediateKey) o;

    if (site != null ? !site.equals(that.getSite()) : that.getSite() != null)
      return false;
    if (node != null ? !node.equals(that.getNode()) : that.getNode() != null)
      return false;
    if (cache != null ? !cache.equals(that.getCache()) : that.getCache() != null)
      return false;
    return !(key != null ? !key.equals(that.getKey()) : that.getKey() != null);

  }

  @Override public int hashCode() {
    int result = Integer.valueOf(key) % 2;//key.hashCode();
    return result;
  }

  @Override public int compareTo(Object o) {
    if (o == null || getClass() != o.getClass())
      return -1;

    IndexedComplexIntermediateKey that = (IndexedComplexIntermediateKey) o;
    int result = -1;
    if (site != null) {
      result = site.compareTo(that.getSite());
      if (result != 0)
        return result;
    } else {
      return -1;
    }

    if (node != null) {
      result = node.compareTo(that.getNode());
      if (result != 0)
        return result;
    } else {
      return -1;
    }
    if (cache != null) {
      result = cache.compareTo(that.getCache());
      if (result != 0)
        return result;
    } else {
      return -1;
    }
    if (key != null) {
      return key.compareTo(that.getKey());
    }
    return -1;
  }


  private void writeObject(java.io.ObjectOutputStream out) throws Exception {
    if (site == null || node == null || key == null || cache == null) {
      throw new Exception(this.toString() + " EXCEPTION " + this.getClass().toString());
    }
    out.writeObject(site);
    out.writeObject(node);
    out.writeObject(key);
    out.writeObject(cache);
    //        String toWrite =  site+"--"+node+"--"+key+"--"+counter;
    //        out.writeObject(toWrite);
  }

  private void readObject(java.io.ObjectInputStream in) throws IOException, ClassNotFoundException {
    site = (String) in.readObject();
    node = (String) in.readObject();
    key = (String) in.readObject();
    cache = (String) in.readObject();
    //        String stringRead = (String) in.readObject();
    //        String[] values = stringRead.split("--");
    //        site = values[0].trim();
    //        node = values[1].trim();
    //        key = values[2].trim();
    //        counter = Integer.parseInt(values[3].trim());
  }


  public String getUniqueKey() {
    return site + node + cache + key;
  }

  @Override public String toString() {
    return key;
  }
}
