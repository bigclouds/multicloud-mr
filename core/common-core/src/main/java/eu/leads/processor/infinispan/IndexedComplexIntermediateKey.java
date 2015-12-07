package eu.leads.processor.infinispan;

import org.hibernate.search.annotations.Analyze;
import org.hibernate.search.annotations.Field;
import org.hibernate.search.annotations.Index;
import org.hibernate.search.annotations.Indexed;
import org.hibernate.search.annotations.Store;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

/**
 * Created by vagvaz on 3/7/15.
 */
//@Indexed
public class IndexedComplexIntermediateKey implements Comparable, Serializable {

//  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES)
  private String site;

//  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES)
  private String node;
//  @Field(index = Index.YES, analyze = Analyze.NO, store = Store.YES, name = "key")
  private String key;


  private String cache;

  public IndexedComplexIntermediateKey(String site, String node, String cacheName) {
    this.site = site;
    this.node = node;
    this.cache = cacheName;
  }

  public IndexedComplexIntermediateKey(String site, String node, String cacheName, String key) {
    this.site = site;
    this.node = node;
    this.cache = cacheName;
    this.key = key;

  }

  public IndexedComplexIntermediateKey(IndexedComplexIntermediateKey other) {
    this(other.getSite(), other.getNode(), other.getCache(), other.getKey());
  }

  public IndexedComplexIntermediateKey() {

  }

  private void readObject(ObjectInputStream in
  ) throws ClassNotFoundException, IOException {
    this.site = in.readUTF();
    this.node = in.readUTF();
    this.cache = in.readUTF();
    this.key = in.readUTF();
  }

  /**
   * This is the default implementation of writeObject. Customise if necessary.
   */
  private void writeObject(ObjectOutputStream out) throws IOException {
    out.writeUTF(site);
    out.writeUTF(node);
    out.writeUTF(cache);
    out.writeUTF(key);
  }

  public void unserialize(byte[] asbytes) throws IOException {
    ByteArrayInputStream input = new ByteArrayInputStream(asbytes);
    ObjectInputStream ios = new ObjectInputStream(input);
    this.site = ios.readUTF();
    this.node = ios.readUTF();
    this.cache = ios.readUTF();
    this.key = ios.readUTF();
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

  @Override
  public boolean equals(Object o) {

    if (o == null || getClass() != o.getClass()) {
      return false;
    }

    IndexedComplexIntermediateKey that = (IndexedComplexIntermediateKey) o;

    if (site != null ? !site.equals(that.site) : that.site != null) {
      return false;
    }
    if (node != null ? !node.equals(that.node) : that.node != null) {
      return false;
    }
    if (cache != null ? !cache.equals(that.cache) : that.cache != null) {
      return false;
    }
    return !(key != null ? !key.equals(that.key) : that.key != null);

  }

  @Override
  public int hashCode() {
    int result = key.hashCode();
    return result;
  }

  @Override
  public int compareTo(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return -1;
    }

    IndexedComplexIntermediateKey that = (IndexedComplexIntermediateKey) o;
    int result = -1;
    if (site != null) {
      result = site.compareTo(that.site);
      if (result != 0) {
        return result;
      }
    } else {
      return -1;
    }

    if (node != null) {
      result = node.compareTo(that.node);
      if (result != 0) {
        return result;
      }
    } else {
      return -1;
    }
    if (cache != null) {
      result = cache.compareTo(that.cache);
      if (result != 0) {
        return result;
      }
    } else {
      return -1;
    }
    if (key != null) {
      return key.compareTo(that.key);
    }
    return -1;
  }

  public String getUniqueKey() {
    return site + node + cache + key;
  }

  @Override
  public String toString() {
    return key.toString();
  }
}
