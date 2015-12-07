package eu.leads.processor.common.utils.storage;

import org.apache.hadoop.io.Writable;

import java.io.DataInput;
import java.io.DataOutput;
import java.io.IOException;
import java.util.Arrays;

/**
 * Created by vagvaz on 2/10/15.
 */
public class HDFSByteChunk implements Writable {
  byte[] data;
  String fileName;
  public HDFSByteChunk(){
    data = null;
  }

  public HDFSByteChunk(byte[] bytes){
    data = Arrays.copyOf(bytes,bytes.length);
  }
  public HDFSByteChunk(byte[] bytes,String fileName){
    data = Arrays.copyOf(bytes,bytes.length);
    this.fileName = fileName;
  }

  @Override public void write(DataOutput out) throws IOException {
    out.writeUTF(fileName);
    out.writeInt(data.length);
    out.write(data);
  }

  public byte[] getData() {
    return data;
  }

  public void setData(byte[] data) {
    this.data = data;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  @Override public void readFields(DataInput in) throws IOException {
    fileName = in.readUTF();
    int size = in.readInt();
    data = new byte[size];
    in.readFully(data);

  }
}