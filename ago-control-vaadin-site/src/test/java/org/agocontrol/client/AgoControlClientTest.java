package org.agocontrol.client;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import org.junit.Test;

import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class for resting ago control client.
 */
public class AgoControlClientTest {

    @Test
    public void testConnectivity() throws Exception, Throwable {
        final JsonRpcHttpClient client = new JsonRpcHttpClient(
                new URL("http://127.0.0.1:8008/jsonrpc"));
        final Map parameters = new HashMap();
        final Map content = new HashMap();
        content.put("command", "inventory");
        content.put("id","2");
        parameters.put("content", content);
        final Object result = client.invoke("message", parameters, HashMap.class);
        System.out.println(result.toString());
    }
}
