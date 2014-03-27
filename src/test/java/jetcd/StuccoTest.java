package jetcd;

import gov.pnnl.stucco.jetcd.StuccoClient;
import gov.pnnl.stucco.jetcd.StuccoJetcdUtil;

import java.io.FileWriter;
import java.io.PrintWriter;
import java.util.List;
import java.util.Map;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;


public class StuccoTest {
    private static StuccoClient client;
    private static PrintWriter out;

    @BeforeClass
    public static void setUpClass() throws Exception {
        out = new PrintWriter(new FileWriter("etcdClient.out"));

        String host = System.getenv("ETCD_HOST");
        if (host == null) {
            host = "localhost";
        }
        
        String port = System.getenv("ETCD_PORT");
        if (port == null) {
            port = "4001";
        }
        
        String url = String.format("http://%s:%s", host, port);
        client = new StuccoClientImpl(url);
    }
    
    @AfterClass
    public static void tearDownClass() throws Exception {
        if (out != null) {   
            out.close();
        }
    }

    @Test
    public void testGetStucco() throws EtcdException {        
        Map<String, Object> nested = client.listNested("stucco");

        out.println();
        String indent = "";
        printNested(indent, nested);
        
        Map<String, Object> trimmed = StuccoJetcdUtil.trimKeyPaths(nested);
        out.println();
        printNested(indent, trimmed);
    }
    
    /** Recursively prints a Map<String, Object> obtained from listNested(). */
    private void printNested(String indent, Map<String, Object> nested) {
        for (Map.Entry<String, Object> entry : nested.entrySet()) {
            String key = entry.getKey();
            Object value = entry.getValue();
            if (value instanceof String) {
                out.printf("%s%s=%s%n", indent, key, value);
            }
            else {
                out.printf("%s%s:%n", indent, key);
                printNested(indent + "  ", (Map<String, Object>) value);
            }
        }
    }

}
