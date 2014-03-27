package gov.pnnl.stucco.jetcd;

import java.util.List;
import java.util.Map;

import jetcd.EtcdClient;
import jetcd.EtcdException;


public interface StuccoClient extends EtcdClient {

    /**
     * Lists the child keys of a given key path.
     */
    public List<String> listKeys(String path) throws EtcdException;
    
    /** 
     * Gets whether a given key path is a directory.
     */
    public boolean isDirectory(String path) throws EtcdException;

    /** 
     * Gets the content at a hierarchy level. The values returned
     * will depend on the type of key. For a leaf key, the value will be a
     * String. For a directory, the value will be a Map<String, Object>
     * obtained by recursively calling this method.
     */
    public Map<String, Object> listNested(String path) throws EtcdException;
    
}
