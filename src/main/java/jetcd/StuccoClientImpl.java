package jetcd;

/*
 * Copyright 2013 Diwaker Gupta
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

import gov.pnnl.stucco.jetcd.StuccoClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import com.google.common.base.Preconditions;
import com.google.common.base.Strings;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;

import retrofit.ErrorHandler;
import retrofit.RestAdapter;
import retrofit.RetrofitError;
import retrofit.converter.JacksonConverter;
import jetcd.EtcdClient;

/**
 * EtcdClientImpl, extended with additional functionality. This copies source
 * since the original is final, preventing us from extending it, our preferred
 * option.
 */
public class StuccoClientImpl implements StuccoClient {
  private static final ObjectMapper objectMapper = new ObjectMapper()
    .configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);
  private final EtcdApi etcd;

  public StuccoClientImpl(final String server) {
    RestAdapter restAdapter = new RestAdapter.Builder()
      .setConverter(new JacksonConverter(objectMapper))
      .setServer(server)
      .setErrorHandler(new EtcdErrorHandler())
      .build();
    etcd = restAdapter.create(EtcdApi.class);
  }

//  @Override
  public String get(final String key) throws EtcdException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    return etcd.get(key).getNode().getValue();
  }

//  @Override
  public void set(final String key, final String value) throws EtcdException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(value));
    etcd.set(key, value, null, null, null);
  }

//  @Override
  public void set(final String key, final String value, final int ttl)
      throws EtcdException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(value));
    Preconditions.checkArgument(ttl > 0);
    etcd.set(key, value, ttl, null, null);
  }

//  @Override
  public void delete(final String key) throws EtcdException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    etcd.delete(key);
  }

//  @Override
  public Map<String, String> list(final String path) throws EtcdException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
    ImmutableMap.Builder<String, String> builder = ImmutableMap.builder();
    for (EtcdResponse.Node node : etcd.get(path).getNode().getNodes()) {
        String key = node.getKey();
        String value = node.getValue();
        if (value != null) {
            builder.put(key, value);
        }
    }
    return builder.build();
  }

//  @Override
  public void compareAndSwap(final String key, final String oldValue,
                             final String newValue) throws EtcdException {
    Preconditions.checkArgument(!Strings.isNullOrEmpty(key));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(oldValue));
    Preconditions.checkArgument(!Strings.isNullOrEmpty(newValue));
    Preconditions.checkArgument(!oldValue.equals(newValue));

    EtcdResponse response = etcd.set(key, newValue, null, null, oldValue);
    response.getNode().getValue();
  }

  private static final class EtcdErrorHandler implements ErrorHandler {
//    @Override
    public Throwable handleError(final RetrofitError cause) {
      return (EtcdException) cause.getBodyAs(EtcdException.class);
    }
  }

  
  // NEW CODE:
  
//  @Override
  public List<String> listKeys(String path) throws EtcdException {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
      ImmutableList.Builder<String> builder = ImmutableList.builder();
            
      List<EtcdResponse.Node> children = getChildrenProxies(path); 
      for (EtcdResponse.Node child : children) {
          builder.add(child.getKey());
      }
      
      return builder.build();
  }
  
//  @Override
  public boolean isDirectory(String path) throws EtcdException {
      List<EtcdResponse.Node> children = getChildrenProxies(path);
      return (children != null  &&  !children.isEmpty());
  }
  
//  @Override
  public Map<String, Object> listNested(String path) throws EtcdException {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
      ImmutableMap.Builder<String, Object> builder = ImmutableMap.builder();
      
      // For each child key in the directory
      List<EtcdResponse.Node> children = getChildrenProxies(path);
      for (EtcdResponse.Node node : children) {
          String key = node.getKey();
          Object value;
          if (isDirectory(key)) {
              // It's a subdirectory, so get a recursive submap
              value = listNested(key);
              
              // See if the submap should really be a list
              value = convertIfReallyList(key, (Map<String, Object>) value);
          }
          else {
              // It's a value, so save it
              value = node.getValue();
          }
          builder.put(key, value);
      }
      return builder.build();      
  }

  /** 
   * Gets proxies for the children of a given directory. 
   * 
   * <p> IMPORTANT: These are proxies in the sense that their names will be 
   * valid but they won't contain any of their own children. 
   * 
   * <p>If the path isn't a directory, the result will be null.
   */
  private List<EtcdResponse.Node> getChildrenProxies(String path) throws EtcdException {
      Preconditions.checkArgument(!Strings.isNullOrEmpty(path));
      EtcdResponse.Node root = etcd.get(path).getNode();
      List<EtcdResponse.Node> children = root.getNodes(); 
      return children;      
  }
  
  /** 
   * Checks a Map to see if it would more naturally be represented by a list.
   * 
   * @return  List<Object> or the original Map (if it wasn't a list)
   */
  private Object convertIfReallyList(String key, Map<String, Object> map) {
      int count = map.size();
      List<Object> valueList = new ArrayList<Object>(count);
      
      // Gather the values in numeric key order
      for (int i = 0; i < count; i++) {
          String keyStr = key + "/" + String.valueOf(i);
          if (!map.containsKey(keyStr)) {
              // Didn't find a number key we expected, so it's not a list
              return map;
          }
          
          // Add the value for this key
          valueList.add(map.get(keyStr));
      }
      
      return valueList;
  }
  
}





















