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
package org.agocontrol.agent;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.apache.qpid.client.AMQAnyDestination;
import org.apache.qpid.client.message.JMSBytesMessage;
import org.apache.qpid.messaging.Address;

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
import java.util.Enumeration;
import java.util.HashMap;
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
public class AgentBusClient {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(AgentBusClient.class);
    /**
     * The message poll wait in milliseconds.
     */
    public static final int POLL_WAIT_MILLIS = 100;
    /**
     * JSON object mapper.
     * */
    private final ObjectMapper mapper = new ObjectMapper();
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
    private final BlockingQueue<Message> replyMessageQueue = new LinkedBlockingQueue<>(10);
    /**
     * The host.
     */
    private final String host;
    /**
     * The port.
     */
    private final int port;

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
    public AgentBusClient(final String userName, final String password, final String host, final int port)
            throws Exception {

        this.host = host;
        this.port = port;

        final Properties properties = new Properties();
        properties.put("java.naming.factory.initial",
                "org.apache.qpid.jndi.PropertiesFileInitialContextFactory");
        properties.put("connectionfactory.qpidConnectionfactory",
                "amqp://" + userName + ":" + password + "@agocontrolvaadinsite/client" +
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
                        handleEvent();
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
     * @param commandMessage the commandMessage
     * @return the reply
     */
    public final Message sendCommand(final MapMessage commandMessage) {
        synchronized (messageProducer) {
            try {
                replyMessageQueue.clear(); // clear reply queue to remove any unprocessed responses.

                commandMessage.setJMSReplyTo(replyQueue);
                messageProducer.send(commandMessage);

                final Message replyMessage = replyMessageQueue.poll(5, TimeUnit.SECONDS);
                if (replyMessage == null) {
                    throw new TimeoutException("Timeout in command processing.");
                }

                final Message replyMessageTwo = replyMessageQueue.poll(300, TimeUnit.MILLISECONDS);

                if (replyMessageTwo != null) {
                    LOGGER.debug("Received two commands responses.");
                }

                return replyMessageTwo != null ? replyMessageTwo : replyMessage;
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
    private void handleEvent() throws Exception  {
        final Message message = messageConsumer.receive();

        if (message instanceof MapMessage) {
            final MapMessage mapMessage = (MapMessage) message;
            if (mapMessage == null) {
                return;
            }

            final String subject = mapMessage.getStringProperty("qpid.subject");
            if (subject == null || !subject.startsWith("event")) {
                return;
            }
            if (subject.equals("event.environment.timechanged")) {
                return; // Ignore time changed events.
            }

            final Map<String, Object> map = convertMapMessageToMap(mapMessage);

            map.put("event", subject);

            // TODO implement event handling

            return;
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
        final Map<String, Object> map = new HashMap<>();

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
