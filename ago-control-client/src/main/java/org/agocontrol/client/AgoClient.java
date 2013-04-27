/**
 * Copyright 2013 Tommi S.E. Laukkanen
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.agocontrol.client;

import org.apache.log4j.Logger;
import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.client.message.JMSBytesMessage;
import org.apache.qpid.messaging.Address;

import javax.jms.BytesMessage;
import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Queue;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Ago control qpid client.
 *
 * @author Tommi S.E. Laukkanen
 */
public class AgoClient {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(AgoClient.class);
    /**
     * The message poll wait in milliseconds.
     */
    public static final int POLL_WAIT_MILLIS = 100;
    /**
     * The context.
     */
    private final Context context;
    /**
     * The connection factory.
     */
    private final ConnectionFactory connectionFactory;
    /**
     * The connection.
     */
    private final Connection connection;
    /**
     * The session.
     */
    private final Session session;
    /**
     * The reply queue.
     */
    private final Queue replyQueue;
    /**
     * The message producer.
     */
    private final MessageProducer messageProducer;
    /**
     * The message consumer.
     */
    private final MessageConsumer messageConsumer;
    /**
     * The reply consumer.
     */
    private final MessageConsumer replyConsumer;
    /**
     * Set true to shutdown threads.
     */
    private boolean closeRequested = false;
    /**
     * Event handler thread.
     */
    private final Thread eventHandlerThread;
    /**
     * Reply handler thread.
     */
    private final Thread replyHandlerThread;
    /**
     * Reply message queue.
     */
    private final BlockingQueue<Message> replyMessageQueue = new LinkedBlockingQueue<Message>(10);
    /**
     * The host.
     */
    private final String host;
    /**
     * The port.
     */
    private final int port;
    /**
     * The command listeners.
     */
    private Map<String, List<CommandListener>> commandListeners = new HashMap();
    /**
     * The known devices.
     */
    private Map<String, AgoDevice> devices = new HashMap<String, AgoDevice>();


    /**
     * Constructor which sets entityManagerFactory.
     *
     * @param userName the username
     * @param password the password
     * @param host the host
     * @param port the port
     *
     * @throws Exception if exception occurs in connecting to bus.
     */
    public AgoClient(final String userName, final String password, final String host, final int port)
            throws Exception {

        this.host = host;
        this.port = port;

        final Properties properties = new Properties();
        properties.put("java.naming.factory.initial",
                "org.apache.qpid.jndi.PropertiesFileInitialContextFactory");
        properties.put("connectionfactory.qpidConnectionfactory",
                "amqp://" + userName + ":" + password + "@agocontrol/javaclient" +
                        "?brokerlist='tcp://" + host + ":" + port + "'");

        context = new InitialContext(properties);
        connectionFactory = (ConnectionFactory) context.lookup("qpidConnectionfactory");
        connection = connectionFactory.createConnection();

        connection.start();

        session = connection.createSession(false, Session.AUTO_ACKNOWLEDGE);
        final Destination sendDestination = new AMQAnyDestination(new Address("agocontrol", "", null));
        final Destination receiveDestination = new AMQAnyDestination(new Address("agocontrol", "#", null));

        replyQueue = session.createTemporaryQueue();

        messageProducer = session.createProducer(sendDestination);
        messageConsumer = session.createConsumer(receiveDestination);
        replyConsumer = session.createConsumer(replyQueue);

        eventHandlerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!closeRequested) {
                    try {
                        Thread.sleep(POLL_WAIT_MILLIS);
                        handleMessage();
                    } catch (final Throwable t) {
                        LOGGER.error("Error in handling events.", t);
                    }
                }
            }
        });
        eventHandlerThread.start();

        replyHandlerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                while (!closeRequested) {
                    try {
                        Thread.sleep(POLL_WAIT_MILLIS);
                        handleReply();
                    } catch (final Throwable t) {
                        LOGGER.error("Error in handling events.", t);
                    }
                }
            }
        });
        replyHandlerThread.start();

        LOGGER.info("Connected to bus: " + host + ":" + port);
    }

    /**
     * Adds command listener for given device ID.
     * @param deviceId the deviceId
     * @param commandListener the commandListener
     */
    public final void addCommandListener(final String deviceId, final CommandListener commandListener) {
        synchronized (commandListeners) {
        if (!commandListeners.containsKey(deviceId)) {
            commandListeners.put(deviceId, new ArrayList<CommandListener>());
        }
        commandListeners.get(deviceId).add(commandListener);
        }
    }

    /**
     * Removes command listener.
     * @param deviceId the device id
     * @param commandListener the commandListener
     */
    public final void removeCommandListener(final String deviceId, final CommandListener commandListener) {
        if (commandListeners.containsKey(deviceId)) {
            commandListeners.get(deviceId).remove(commandListener);
        }
    }


    /**
     * Closes client.
     *
     * @throws Exception if exception occurs.
     */
    public final void close() throws Exception {
        closeRequested = true;
        replyHandlerThread.interrupt();
        replyHandlerThread.join();
        eventHandlerThread.interrupt();
        eventHandlerThread.join();
        connection.close();
        context.close();
        LOGGER.info("Disconnected from bus: " + host + ":" + port);
    }

    /**
     * Adds device by adding it to device list and sending announce event.
     * @param deviceId the deviceId
     * @param deviceType the deviceType
     * @param deviceName the deviceName
     * @return true if communication with ago-control was success.
     */
    public final boolean addDevice(final String deviceId, final String deviceType, final String deviceName) {
        synchronized (devices) {
            devices.put(deviceId, new AgoDevice(deviceId, deviceType, deviceName));
        }
        return sendAnnounceDevice(deviceId, deviceType, deviceName);
    }

    /**
     * Removes device by removing device from device list and sending remove event.
     * @param deviceId the deviceId
     * @param deviceType the deviceType
     * @return true if communication with ago-control was success.
     */
    public final boolean removeDevice(final String deviceId, final String deviceType) {
        synchronized (devices) {
            devices.remove(deviceId);
        }
        return sendRemoveDevice(deviceId, deviceType);
    }

    /**
     * Send announce event for all known devices.
     * @return true if communication with ago-control was success.
     */
    public final boolean announceAllDevices() {
        boolean success = true;
        synchronized (devices) {
            for (final AgoDevice device : devices.values()) {
               if (!sendAnnounceDevice(device.getDeviceId(), device.getDeviceType(), device.getDeviceName())) {
                   success = false;
               }
            }
        }
        return success;
    }
    /**
     * Sends announce device event and set name command.
     * @param deviceId the deviceId
     * @param deviceType the deviceType
     * @param deviceName the deviceName
     * @return true if communication with ago-control was successful.
     */
    private boolean sendAnnounceDevice(final String deviceId, final String deviceType, final String deviceName) {
        try {
            {
                final MapMessage commandMessage = createMapMessage();
                commandMessage.setStringProperty("qpid.subject", "event.device.announce");
                commandMessage.setString("uuid", deviceId);
                commandMessage.setString("devicetype", deviceType);
                sendEvent(commandMessage);
            }
            {
                final Map<String, Object> commandMap = new HashMap<String, Object>();
                commandMap.put("command", "setdevicename");
                commandMap.put("uuid", deviceId);
                commandMap.put("name", deviceName);
                sendCommand(commandMap);
            }
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error adding device: " + deviceId + " (" + deviceType + ")", e);
            return false;
        }

    }

    /**
     * Sends remove device event.
     * @param deviceId the deviceId
     * @param deviceType the deviceType
     * @return true if communication with ago-control was successful.
     */
    private boolean sendRemoveDevice(final String deviceId, final String deviceType) {
        try {
            final MapMessage commandMessage = createMapMessage();
            commandMessage.setStringProperty("qpid.subject", "event.device.remove");
            commandMessage.setString("uuid", deviceId);
            sendEvent(commandMessage);
            return true;
        } catch (final Exception e) {
            LOGGER.error("Error removing device: " + deviceId + " (" + deviceType + ")", e);
            return false;
        }
    }

    /**
     * Sends event message.
     *
     * @param eventMessage the eventMessage
     * @return the reply
     */
    public final void sendEvent(final MapMessage eventMessage) {
        synchronized (messageProducer) {
            try {
                messageProducer.send(eventMessage);
            } catch (Exception e) {
                throw new RuntimeException("Error in event message sending.", e);
            }
        }
    }

    /**
     * Sends command message and waits for response.
     *
     * @param commandMap the parameters
     * @return the reply
     */
    public final Message sendCommand(final Map<String, Object> commandMap) {
        synchronized (messageProducer) {
            try {
                replyMessageQueue.clear(); // clear reply queue to remove any unprocessed responses.

                final MapMessage commandMessage = createMapMessage();
                for (final String key : commandMap.keySet()) {
                    commandMessage.setObject(key, commandMap.get(key));
                }

                commandMessage.setJMSReplyTo(replyQueue);

                LOGGER.debug("Sending command: " + commandMap.toString());

                messageProducer.send(commandMessage);

                Message replyMessage = replyMessageQueue.poll(5, TimeUnit.SECONDS);
                if (replyMessage == null) {
                    throw new TimeoutException("Timeout in command processing.");
                }

                if (replyMessage instanceof BytesMessage) {
                    final byte[] buffer = new byte[(int) ((BytesMessage) replyMessage).getBodyLength()];
                    int readBytes = ((BytesMessage) replyMessage).readBytes(buffer);
                    LOGGER.debug("Command reply: " + new String(buffer));
                } else {
                    LOGGER.debug("Command reply: " + replyMessage);
                }

                while (true) {
                    final Message secondaryReplyMessage = replyMessageQueue.poll(1000, TimeUnit.MILLISECONDS);

                    if (secondaryReplyMessage != null) {
                        replyMessage = secondaryReplyMessage;
                        if (secondaryReplyMessage instanceof BytesMessage) {
                            final byte[] buffer = new byte[(int) ((BytesMessage) secondaryReplyMessage).getBodyLength()];
                            int readBytes = ((BytesMessage) secondaryReplyMessage).readBytes(buffer);

                            LOGGER.debug("Command reply: " + new String(buffer));
                        } else {
                            LOGGER.debug("Command reply: " + secondaryReplyMessage);
                        }
                    } else {
                        break;
                    }
                }


                return replyMessage;
            } catch (Exception e) {
                throw new RuntimeException("Error in command message sending.", e);
            }
        }
    }

    /**
     * @return the created message
     */
    public final MapMessage createMapMessage() {
        try {
            return session.createMapMessage();
        } catch (Exception e) {
            throw new RuntimeException("Error in message creation.", e);
        }
    }


    /**
     * Handle reply.
     *
     * @throws Exception if exception occurs.
     */
    private void handleReply() throws Exception  {
        final Message message = (Message) replyConsumer.receive();
        if (message == null) {
            return;
        }
        replyMessageQueue.put(message);
    }

    /**
     * Handle event.
     *
     * @throws Exception if exception occurs.
     */
    private void handleMessage() throws Exception  {
        final Message message = messageConsumer.receive();

        if (message == null) {
            return;
        }

        if (message instanceof MapMessage) {
            final MapMessage mapMessage = (MapMessage) message;

            final String subject = mapMessage.getStringProperty("qpid.subject");
            if (subject == null || !subject.startsWith("event")) {
                final Map<String, Object> map = convertMapMessageToMap(mapMessage);
                LOGGER.debug("Observed command: " + map.toString());

                if (!map.containsKey("uuid")
                        || (map.containsKey("uuid") && commandListeners.containsKey(map.containsKey("uuid")))) {
                    final MessageProducer replyProducer;
                    if (mapMessage.getJMSReplyTo() != null) {
                        if (mapMessage.getJMSReplyTo() instanceof AMQAnyDestination) {
                            final AMQAnyDestination destination = (AMQAnyDestination) mapMessage.getJMSReplyTo();
                            //final Queue replyToQueue = session.createQueue(destination.getSubject());
                            final Destination replyDestination = new AMQAnyDestination(new Address("amq.direct",
                                    destination.getSubject(), null));
                            replyProducer = session.createProducer(replyDestination);
                        } else {
                            replyProducer = session.createProducer(mapMessage.getJMSReplyTo());
                        }
                    } else {
                        replyProducer = null;
                    }

                    boolean replySent = false;
                    final String deviceId = (String) map.get("uuid");
                    for (final CommandListener commandListener : commandListeners.get(deviceId)) {
                        final Map<String, Object> replyMap = commandListener.commandReceived(map);

                        if (replyProducer != null) {
                            if (replyMap == null || replyMap.size() == 0) {
                                continue;
                            }

                            final MapMessage replyMessage = createMapMessage();
                            //replyMessage.setJMSMessageID("ID:" + UUID.randomUUID().toString());
                            for (final String key : replyMap.keySet()) {
                                replyMessage.setObject(key, replyMap.get(key));
                            }
                            replyProducer.send(replyMessage);
                            replySent = true;
                        }
                    }

                    if (replyProducer != null) {
                        if (!replySent) {
                            final BytesMessage okMessage = session.createBytesMessage();
                            okMessage.writeBytes("ACK".getBytes());
                            replyProducer.send(okMessage);
                        }
                        replyProducer.close();
                    }

                }

                return;
            } else {
                final Map<String, Object> map = convertMapMessageToMap(mapMessage);
                map.put("event", subject);
                LOGGER.debug("Observed event: " + map.toString());
                return;
            }
        }

        if (message instanceof JMSBytesMessage) {
            LOGGER.warn("Unhandled byte message: " + message.toString());
            return;
        }

        LOGGER.warn("Unhandled message type " + message.toString());
    }

    /**
     * Converts UTF-8 encoded byte to String.
     *
     * @param bytes the bytes
     * @return the string
     */
    private String bytesToString(final Object bytes) {
        if (bytes == null) {
            return null;
        }
        return new String((byte[]) bytes, Charset.forName("UTF-8"));
    }

    /**
     * Converts MapMessage to Map.
     *
     * @param message the message
     * @return the map
     * @throws javax.jms.JMSException if exception occurs in conversion.
     */
    private Map<String, Object> convertMapMessageToMap(final MapMessage message) throws JMSException {
        final Map<String, Object> map = new HashMap<String, Object>();

        final Enumeration<String> keys = message.getMapNames();
        while (keys.hasMoreElements()) {
            final String key = keys.nextElement();
            map.put(key, message.getObject(key));
        }
        convertByteArrayValuesToStrings(map);
        return map;
    }

    /**
     * Converts byte array values to string recursively.
     * @param map the map
     */
    private void convertByteArrayValuesToStrings(final Map<String, Object> map) {
        for (final String key : map.keySet()) {
            Object object = map.get(key);
            if (object instanceof Map) {
                convertByteArrayValuesToStrings((Map) object);
            }
            if (object instanceof byte[]) {
                map.put(key, bytesToString(object));
            }
        }
    }
}
