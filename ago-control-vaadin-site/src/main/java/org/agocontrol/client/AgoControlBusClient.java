package org.agocontrol.client;

import com.googlecode.jsonrpc4j.JsonRpcHttpClient;
import org.agocontrol.dao.ElementDao;
import org.agocontrol.dao.EventDao;
import org.agocontrol.model.Element;
import org.agocontrol.model.ElementType;
import org.agocontrol.model.Event;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * ago control bus client.
 *
 * Thread safe.
 *
 * @author Tommi S.E. Laukkanen
 */
public class AgoControlBusClient {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(AgoControlBusClient.class);
    /** Default bus name. */
    private static final String DEFAULT = "";
    /**
     * The JSON RPC URL.
     */
    private String jsonRpcUrl;

    /**
     * The JSON RPC Http Client.
     */
    private JsonRpcHttpClient client;

    /**
     * The RPC message ID counter.
     */
    private int messageIDCounter = 0;

    /**
     * Constructor for setting the JSON RPC URL.
     * @param jsonRpcUrl the JSON RPC URL
     */
    public AgoControlBusClient(final String jsonRpcUrl) {
        this.jsonRpcUrl = jsonRpcUrl;

    }

    /**
     * Subscribe to events.
     *
     * @return subscription ID.
     */
    public final synchronized String subscribe() {
        if (!ensureConnection()) {
            throw new RuntimeException("Failed to connect: " + jsonRpcUrl);
        }

        final Map parameters = new HashMap();
        parameters.put("id", Integer.toString(++messageIDCounter));

        final String result;
        try {
            result = client.invoke("subscribe", parameters, String.class);
        } catch (Throwable throwable) {
            LOGGER.error("Error subscribing to bus: " + jsonRpcUrl, throwable);
            throw new RuntimeException("Error subscribing to bus: " + jsonRpcUrl);
        }

        return result;
    }

    /**
     * Ubsubscribe from events.
     *
     * @param subscriptionId the subscription ID
     */
    public final synchronized void unsubscribe(final String subscriptionId) {
        if (!ensureConnection()) {
            throw new RuntimeException("Failed to connect: " + jsonRpcUrl);
        }

        final Map parameters = new HashMap();
        parameters.put("id", Integer.toString(++messageIDCounter));
        parameters.put("uuid", subscriptionId);

        try {
            client.invoke("unsubscribe", parameters, Object.class);
        } catch (Throwable throwable) {
            LOGGER.error("Error unsubscribing from bus: " + jsonRpcUrl, throwable);
            throw new RuntimeException("Error unsubscribing to bus: " + jsonRpcUrl);
        }
    }

    /**
     * Fetch events. If there is no events then this call will block.
     *
     * @param entityManager the entityManager
     * @param owner the owning company
     * @param subscriptionId the subscription ID
     */
    public final synchronized void fetch(final EntityManager entityManager, final Company owner,
                                         final String subscriptionId) {
        if (!ensureConnection()) {
            throw new RuntimeException("Failed to connect: " + jsonRpcUrl);
        }

        final Map parameters = new HashMap();
        parameters.put("id", Integer.toString(++messageIDCounter));
        parameters.put("uuid", subscriptionId);

        final String result;
        try {
            result = client.invoke("getevent", parameters, String.class);
        } catch (Throwable throwable) {
            LOGGER.error("Error getting event from bus " + jsonRpcUrl, throwable);
            return;
        }

        EventDao.saveEvents(entityManager, Collections.singletonList(
                new Event(owner, result, new Date())
        ));
    }



    /**
     * Synchronizes inventory.
     * @param entityManager for database synchronization
     * @param owner company under which the inventory is synchronized.
     * @param startTreeIndex the starting tree index
     * @return new tree index.
     */
    public final synchronized int synchronizeInventory(final EntityManager entityManager, final Company owner,
                                                           final int startTreeIndex) {
        if (!ensureConnection()) {
            throw new RuntimeException("Failed to connect: " + jsonRpcUrl);
        }

        final Map parameters = new HashMap();
        final Map content = new HashMap();
        content.put("command", "inventory");
        content.put("id", Integer.toString(++messageIDCounter));
        parameters.put("content", content);

        final Map<String, Object> result;
        try {
            result = client.invoke("message", parameters, HashMap.class);
        } catch (Throwable throwable) {
            LOGGER.error("Error getting inventory from bus: " + jsonRpcUrl, throwable);
            throw new RuntimeException("Error getting inventory from: " + jsonRpcUrl);
        }

        final List<Element> elements = new ArrayList<>(ElementDao.getElements(entityManager, owner));
        final Map<String, Element> idElementMap = new HashMap<>();
        final Map<String, Element> nameBuildingMap = new HashMap<>();

        for (final Element element : elements) {
            if (element.getType() == ElementType.Building) {
                nameBuildingMap.put(element.getName(), element);
            }
            idElementMap.put(element.getElementId(), element);
        }

        if (!nameBuildingMap.containsKey(DEFAULT)) {
            final Element building = new Element(owner, ElementType.Building, DEFAULT, "");
            nameBuildingMap.put(building.getName(), building);
            idElementMap.put(building.getElementId(), building);
            elements.add(building);
        }

        if (result.containsKey("rooms")) {
            final Map<String, Object> rooms = (Map) result.get("rooms");
            for (final String roomId : ((Map<String, Object>) result.get("rooms")).keySet()) {
                final Map<String, Object> roomMessage = (Map) rooms.get(roomId);
                final String roomName = (String) roomMessage.get("name");
                final String roomLocation = (String) roomMessage.get("location");

                final Element building;
                if (nameBuildingMap.containsKey(roomLocation)) {
                    building = nameBuildingMap.get(roomLocation);
                } else {
                    building = new Element(owner, ElementType.Building, DEFAULT, "");
                    nameBuildingMap.put(DEFAULT, building);
                    elements.add(building);
                    idElementMap.put(building.getElementId(), building);
                }

                final Element room;
                if (idElementMap.containsKey(roomId)) {
                    room = idElementMap.get(roomId);
                    room.setParentId(building.getElementId());
                    room.setName(roomName);
                } else {
                    room = new Element(roomId, building.getElementId(), owner, ElementType.Room, roomName, "");
                    elements.add(room);
                    idElementMap.put(room.getElementId(), room);
                }
            }
        }

        if (result.containsKey("inventory")) {
            final Map<String, Object> inventory = (Map) result.get("inventory");
            for (final String elementId : ((Map<String, Object>) result.get("inventory")).keySet()) {
                final Map<String, Object> elementMessage = (Map) inventory.get(elementId);
                final String name = (String) elementMessage.get("name");
                final String roomId = (String) elementMessage.get("room");
                final String category = (String) elementMessage.get("devicetype");

                final Element parent;
                if (idElementMap.containsKey(roomId)) {
                    parent = idElementMap.get(roomId);
                } else {
                    parent =  nameBuildingMap.get(DEFAULT);
                }

                final Element element;
                if (idElementMap.containsKey(elementId)) {
                    element = idElementMap.get(elementId);
                    element.setParentId(parent.getElementId());
                    element.setName(name);
                    element.setCategory(category);
                    element.setType(ElementType.Device);
                } else {
                    element = new Element(elementId, parent.getElementId(), owner, ElementType.Device, name, category);
                    elements.add(element);
                    idElementMap.put(element.getElementId(), element);
                }
            }
        }

        final List<Element> roots = new ArrayList<>();
        final Map<Element, Set<Element>> treeMap = new HashMap<>();

        for (final Element element : elements) {
            if (element.getElementId().equals(element.getParentId())) {
                element.setTreeDepth(0);
                roots.add(element);
            } else {
                final Element parent = idElementMap.get(element.getParentId());
                if (parent != null) {
                    if (!treeMap.containsKey(parent)) {
                        treeMap.put(parent, new TreeSet<Element>());
                    }
                    treeMap.get(parent).add(element);
                }
            }
        }

        final LinkedList<Element> elementsToIterate = new LinkedList<Element>();
        elementsToIterate.addAll(roots);

        int treeIndex = startTreeIndex;
        while (elementsToIterate.size() > 0) {
            final Element element = elementsToIterate.removeFirst();
            element.setTreeIndex(++treeIndex);
            final Set<Element> children = treeMap.get(element);
            if (children != null) {
                for (final Element child : children) {
                    child.setTreeDepth(element.getTreeDepth() + 1);
                }
                elementsToIterate.addAll(children);
            }
        }

        ElementDao.saveElements(entityManager, elements);

        return treeIndex;
    }

    /**
     * Checks if connection exists and if it does not then tries to rconnect.
     * @return false if connection did not exist and reconnect failed.
     */
    private boolean ensureConnection() {
        if (!isConnected()) {
            connect();
        }
        return isConnected();
    }

    /**
     * Checks whether connection exists.
     *
     * @return true if connected.
     */
    public final synchronized boolean isConnected() {
        return client != null;
    }

    /**
     * Connect.
     */
    private void connect() {
        try {
            client = new JsonRpcHttpClient(
                    new URL(jsonRpcUrl));
        } catch (MalformedURLException e) {
            LOGGER.error("Error connecting to: " + jsonRpcUrl, e);
        }
    }
}
