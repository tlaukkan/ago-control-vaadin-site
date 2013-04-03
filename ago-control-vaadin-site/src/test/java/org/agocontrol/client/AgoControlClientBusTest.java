package org.agocontrol.client;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.model.PostalAddress;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/**
 * Test class for resting ago control client.
 */
public class AgoControlClientBusTest {
    /** The properties category used in instantiating default services. */
    private static final String PROPERTIES_CATEGORY = "test";
    /** The persistence unit to be used. */
    public static final String PERSISTENCE_UNIT = "ago-control-vaadin-site";
    /** The entity manager factory for test. */
    private static EntityManagerFactory entityManagerFactory;

    static {
        @SuppressWarnings("rawtypes")
        final Map properties = new HashMap();
        properties.put(PersistenceUnitProperties.JDBC_DRIVER,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_DRIVER));
        properties.put(PersistenceUnitProperties.JDBC_URL,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_URL));
        properties.put(PersistenceUnitProperties.JDBC_USER,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_USER));
        properties.put(PersistenceUnitProperties.JDBC_PASSWORD,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_PASSWORD));
        properties.put(PersistenceUnitProperties.DDL_GENERATION,
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, PersistenceUnitProperties.DDL_GENERATION));
        entityManagerFactory = Persistence.createEntityManagerFactory(PERSISTENCE_UNIT, properties);
    }

    /** The entity manager for test. */
    private EntityManager entityManager;
    private Company owner;

    /**
     * @throws Exception if exception occurs in setup.
     */
    @Before
    public void setUp() throws Exception {

        entityManager = entityManagerFactory.createEntityManager();

        final PostalAddress invoicingAddress = new PostalAddress("", "", "", "", "", "");
        final PostalAddress deliveryAddress = new PostalAddress("", "", "", "", "", "");
        entityManager.persist(invoicingAddress);
        entityManager.persist(deliveryAddress);

        owner = new Company("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", invoicingAddress, deliveryAddress);
        entityManager.getTransaction().begin();
        entityManager.persist(owner);
        entityManager.getTransaction().commit();
    }

    @Test
    public void testConnectivity() throws Exception, Throwable {

        final AgoControlBusClient client = new AgoControlBusClient("http://127.0.0.1:8008/jsonrpc");
        client.synchronizeInventory(entityManager, owner, 0);


        /*final JsonRpcHttpClient client = new JsonRpcHttpClient(
                new URL("http://127.0.0.1:8008/jsonrpc"));
        final Map parameters = new HashMap();
        final Map content = new HashMap();
        content.put("command", "inventory");
        content.put("id","2");
        parameters.put("content", content);
        final Object result = client.invoke("message", parameters, HashMap.class);
        System.out.println(result.toString());*/
    }
}
