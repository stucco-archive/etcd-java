# Purpose #
This repository provides minor extensions to the *jetcd* 0.2.0 library (diwakergupta version). This is motivated by the desire to keep configuration usage backwards-compatible with the *Yaml* code previously used to read configuration directly from file. *Yaml* provides the means to grab an entire configuration subtree. Using *etcd* directly also supports subtree access.  In contrast, *jetcd* seems to assume that nesting is only one level deep, providing such access only at the leaf level of the configuration tree. 
# Usage #
The *etcd-java* code consists of a small number of new methods. These are described in the JavaDoc comments, but for convenience the information is repeated here.
## StuccoClientImpl ##
- `public List<String> listKeys(String path)` lists the child keys of a given key path.
- `public boolean isDirectory(String path)` determines whether a key path is a directory within the configuration. 
- `public Map<String, Object> listNested(String path)` gets the content at a subtree level of the configuration. The values in the returned `Map` depend on the location of the key. For a leaf key, the value will be a `String`. For a directory key, the value will be a nested `Map<String, Object>`. 
## StuccoJetcdUtil ##
- `public static Map<String, Object> trimKeyPaths(Map<String, Object> configMap)` trims the key names in a `jetcd` configuration Map, so you no longer have to provide full path info. For example, the key-value pair `(“/full/path/key”, “value”)` would be replaced with `(“key”, “value”)`, so you could then just ask for `“key”` instead of `“/full/path/key”`.
- `public static List<Object> trimKeyPaths(List<Object> configList)` does the same thing, only for a *jetcd* configuration List.
# Example #
Here is an example adapted from how configuration is used in the Replayer in the collectors repository.
                  
    // Open the configuration service
    StuccoClient client = new StuccoClientImpl(“http://10.10.10.100:4001”);
    
    // Grab the entire configuration
    Map<String, Object> config = client.listNested("/");
    
    // Make it accessible by simple keys (like “key” instead of “/full/path/key”)
    config = StuccoJetcdUtil.trimKeyPaths(config);
    
    // Get a specific section known to be at the top level of the config
    Map<String, Object> replayConfig = (Map<String, Object>) config.get(“replayer-web”);
    
    // Get the value for a specific key in that section  
    String outputDir = (String) replayerConfig.get(“output-dir”);

# Implementation Note #
The *etcd-java* repository is separate from *jetcd*. However, it depends on the *jetcd* jar, and also adds a class to the *jetcd* package. The addition is necessitated by *jetcd*’s design, which declares many classes final, preventing subclassing.

