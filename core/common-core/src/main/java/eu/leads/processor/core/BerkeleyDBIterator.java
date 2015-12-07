package eu.leads.processor.core;

import com.sleepycat.je.*;

import java.nio.charset.Charset;
import java.util.Iterator;
import java.util.NoSuchElementException;

/**
 * Created by vagvaz on 8/14/15.
 */
public class BerkeleyDBIterator implements Iterator<Object> {
  Cursor cursor;
  Database db;
  String key;
  DatabaseEntry searchKey;
  DatabaseEntry searchValue;
  OperationStatus status = null;

  public BerkeleyDBIterator(Database db, String key) {
    this.db = db;
    this.key = key;
    //        initialize(key);
    searchKey = new DatabaseEntry();
    searchValue = new DatabaseEntry();
    this.cursor = cursor;
  }

  public void initialize(String key) {
    this.key = key;
    if (cursor != null) {
      try {
        status = cursor.getNextNoDup(searchKey, searchValue, LockMode.DEFAULT);
        if (status == OperationStatus.SUCCESS) {
          String keyString = new String(searchKey.getData(), Charset.forName("UTF-8"));
          if (keyString.equals(key)) {
            //                        System.out.println("next success");
            return;
          }
        }
      } catch (DatabaseException e) {
        e.printStackTrace();
      }
    } else {
      System.out.println("unsucc for " + key);
    }
    try {
      if (cursor != null)
        cursor.close();
      searchKey.setData(key.getBytes(Charset.forName("UTF-8")));
      cursor = db.openCursor(null, null);
      status = cursor.getSearchKey(searchKey, searchValue, LockMode.DEFAULT);
    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }

  @Override public boolean hasNext() {
    if (status == OperationStatus.SUCCESS)
      return true;
    return false;
  }

  @Override public Object next() {
    try {
      if (status == OperationStatus.SUCCESS) {
        Object result = new String(searchValue.getData());
        status = cursor.getNextDup(searchKey, searchValue, LockMode.DEFAULT);
        return result;
      }
    } catch (DatabaseException e) {
      e.printStackTrace();
    }
    throw new NoSuchElementException("Iterator no more elements");

  }

  @Override public void remove() {

  }

  public void close() {
    try {
      if (cursor != null)
        cursor.close();
    } catch (DatabaseException e) {
      e.printStackTrace();
    }
  }
}
//
//import com.sleepycat.bind.tuple.TupleBinding;
//import com.sleepycat.je.*;
//import com.sleepycat.persist.EntityCursor;
//import com.sleepycat.persist.SecondaryIndex;
//
//import java.io.UnsupportedEncodingException;
//import java.nio.charset.Charset;
//import java.util.Iterator;
//import java.util.NoSuchElementException;
//
///**
// * Created by vagvaz on 8/14/15.
// */
//public class BerkeleyDBIterator implements Iterator<Object> {
//    Cursor cursor;
//    Database db;
//    String key;
//    DatabaseEntry searchKey;
//    DatabaseEntry searchValue;
//    OperationStatus status = null;
//    TupleWrapperBinding binding;
//    public BerkeleyDBIterator(Database db,String key) {
//        this.db = db;
//        this.key = key;
//        binding = new TupleWrapperBinding();
////        initialize(key);
//        searchKey = new DatabaseEntry();
//        searchValue = new DatabaseEntry();
//        this.cursor = cursor;
//    }
//
//    public void initialize(String key){
//        this.key= key;
//        if(cursor != null){
//            try {
//                status = cursor.getNextNoDup(searchKey, searchValue, LockMode.DEFAULT);
//                if(status == OperationStatus.SUCCESS){
//                    String keyString = new String(searchKey.getData(),Charset.forName("UTF-8"));
//                    if(keyString.equals(key))
//                    {
////                        System.out.println("next success");
//                        return;
//                    }
//                }
//            } catch (DatabaseException e) {
//                e.printStackTrace();
//            }
//        }else{
//            System.out.println("unsucc for " + key);
//        }
//        try {
//            if(cursor != null)
//                cursor.close();
//            searchKey.setData(key.getBytes(Charset.forName("UTF-8")));
//            cursor = db.openCursor(null,null);
//            status = cursor.getSearchKey(searchKey,searchValue,LockMode.DEFAULT);
//        } catch (DatabaseException e) {
//            e.printStackTrace();
//        }
//    }
//    @Override public boolean hasNext() {
//        if(status == OperationStatus.SUCCESS)
//            return true;
//        return false;
//    }
//
//    @Override public Object next() {
//        try {
//            if(status == OperationStatus.SUCCESS) {
//                Tuple wrapper = (Tuple) binding.entryToObject(searchValue);
////                if(wrapper.hasField("__complexKey"))
//                    wrapper.removeAtrribute("__complexKey");
//                Object result = wrapper;//wrapper.getTuple();
//                status = cursor.getNextDup(searchKey,searchValue,LockMode.DEFAULT);
//                return result;
//            }
//        } catch (DatabaseException e) {
//            e.printStackTrace();
//        }
//        throw new NoSuchElementException("Iterator no more elements");
//
//    }
//
//    @Override public void remove() {
//
//    }
//
//    public void close() {
//        try {
//            if(cursor != null)
//            cursor.close();
//        } catch (DatabaseException e) {
//            e.printStackTrace();
//        }
//    }
//}
