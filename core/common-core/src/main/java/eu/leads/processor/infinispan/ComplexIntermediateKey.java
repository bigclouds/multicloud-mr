package eu.leads.processor.infinispan;

import java.io.Serializable;

/**
 * Created by vagvaz on 3/6/15.
 */
public class ComplexIntermediateKey implements Comparable, Serializable {

  private String site;
  private String node;
  private String cache;
  private String key;
  private Integer counter;
  private static final long serialVersionUID = -81923791823178123L;

  public ComplexIntermediateKey(String site, String node, String cache) {
    this.site = site;
    this.node = node;
    this.cache = cache;
  }

  public ComplexIntermediateKey(String site, String node, String key, String cache, Integer counter) {
    this.site = site;
    this.node = node;
    this.key = key;
    this.cache = cache;
    this.counter = counter;
  }

  public ComplexIntermediateKey(ComplexIntermediateKey other) {
    this.site = other.getSite();
    this.node = other.getNode();
    this.key = other.getKey();
    this.cache = other.getCache();
    this.counter = other.getCounter();
  }

  public ComplexIntermediateKey() {
    site = "";
    node = "";
    key = "";
    cache = "";
    counter = -1;
  }

  public ComplexIntermediateKey(IndexedComplexIntermediateKey currentChunk) {
    this.site = currentChunk.getSite();
    this.node = currentChunk.getNode();
    this.key = currentChunk.getKey();
    this.cache = currentChunk.getCache();
    this.counter = new Integer(0);
  }
  //   private void writeObject(java.io.ObjectOutputStream out)
  //           throws IOException{
  ////      out.writeObject(site);
  ////      out.writeObject(node);
  ////      out.writeObject(key);
  ////      out.writeInt(counter);
  //      String toWrite =  site+"--"+node+"--"+key+"--"+counter;
  //      out.writeObject(toWrite);
  //   }
  //   private void readObject(java.io.ObjectInputStream in)
  //           throws IOException, ClassNotFoundException{
  ////         site = (String) in.readObject();
  ////         node = (String) in.readObject();
  ////         key = (String) in.readObject();
  ////         counter = new Integer(in.readInt());
  //      String stringRead = (String) in.readObject();
  //      String[] values = stringRead.split("--");
  //      site = values[0].trim();
  //      node = values[1].trim();
  //      key = values[2].trim();
  //      counter = Integer.parseInt(values[3].trim());
  //   }

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

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public Integer getCounter() {
    return counter;
  }

  public void setCounter(Integer counter) {
    this.counter =counter;
  }

  @Override public int hashCode() {
    return key.hashCode();// site.hashCode()+node.hashCode()+key.hashCode()+counter.hashCode();
  }

  @Override public boolean equals(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComplexIntermediateKey that = (ComplexIntermediateKey) o;

    //      if (site != null ? !site.equals(that.site) : that.site != null) return false;
    //      if (node != null ? !node.equals(that.node) : that.node != null) return false;
    //      if (key != null ? !key.equals(that.key) : that.key != null) return false;
    //      return !(counter != null ? !counter.equals(that.counter) : that.counter != null);
    if (site.equals(that.getSite())) {
      if (node.equals(that.getNode())) {
        if (key.equals(that.getKey())) {
          if (cache.equals(that.getCache())) {
            if (counter.equals(that.getCounter())) {
              return true;
            }
          }
        }
      }
    }
    return false;
    //     return that.toString().equals(this.toString());
  }

  @Override public int compareTo(Object o) {
    if (o == null || getClass() != o.getClass()) {
      return -1;
    }

    ComplexIntermediateKey that = (ComplexIntermediateKey) o;
    int result = 0;
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
      result = node.compareTo(that.cache);
      if (result != 0) {
        return result;
      }
    } else {
      return -1;
    }
    if (key != null) {
      result = key.compareTo(that.key);
      if (result != 0) {
        return result;
      }
    } else {
      return -1;
    }
    if (counter != null) {
      return counter.compareTo(that.counter);
    }
    return -1;
    //      return o.toString().compareTo(this.toString());
  }

  public void next() {
    counter = new Integer(counter + 1);
    //     return new ComplexIntermediateKey(site,node,key,counter);
  }

  @Override public String toString() {
    return key.toString();
  }

  public String getCache() {
    return cache;
  }

  public void setCache(String cache) {
    this.cache = cache;
  }
}
