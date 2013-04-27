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

import com.vaadin.annotations.Theme;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.vaadin.addons.sitekit.site.AbstractSiteUI;
import org.vaadin.addons.sitekit.site.ContentProvider;
import org.vaadin.addons.sitekit.util.PropertiesUtil;

import java.nio.charset.Charset;
import java.util.Map;
import java.util.UUID;

/**
 * BareSite UI.
 *
 * @author Tommi S.E. Laukkanen
 */
@SuppressWarnings({ "serial", "unchecked" })
@Theme("eelis")
public final class AgoControlAgent {

    /** The time in milliseconds to sleep between shutdown checks. */
    public static final int SHUTDOWN_WAIT_SLEEP_MILLIS = 1000;
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(AgoControlAgent.class);
    /** The properties category used in instantiating default services. */
    private static final String PROPERTIES_CATEGORY = "ago-control-server-agent";
    /** The shutdown request flag. */
    private static boolean shutdown = false;

    /**
     * Main method for running BareSiteUI.
     * @param args the commandline arguments
     * @throws Exception if exception occurs in jetty startup.
     */
    public static void main(final String[] args) throws Exception {
        final Thread mainThread = Thread.currentThread();
        DOMConfigurator.configure("./log4j.xml");

        final String serverName = PropertiesUtil.getProperty(PROPERTIES_CATEGORY, "server-name");
        final String serverType = PropertiesUtil.getProperty(PROPERTIES_CATEGORY, "server-type");
        final String serverId = UUID.nameUUIDFromBytes(serverName.getBytes(Charset.forName("UTF-8"))).toString();

        final AgentBusClient busClient = new AgentBusClient(
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, "ago-control-bus-user"),
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, "ago-control-bus-password"),
                PropertiesUtil.getProperty(PROPERTIES_CATEGORY, "ago-control-bus-address"),
                Integer.parseInt(PropertiesUtil.getProperty(PROPERTIES_CATEGORY, "ago-control-bus-port")));

        busClient.addCommandListener(serverId, new ShellCommandListener());

        busClient.addCommandListener(null, new CommandListener() {
            @Override
            public Map<String, Object> commandReceived(final Map<String, Object> parameters) {
                LOGGER.debug("Received command: " + parameters.toString());
                final String command = (String) parameters.get("command");
                if ("discover".equals(command)) {
                    LOGGER.debug("Announcing all devices.");
                    busClient.announceAllDevices();
                }
                return null;
            }
        });

        if (!busClient.addDevice(serverId, serverType, serverName)) {
            busClient.close();
            LOGGER.error("Error announcing server as device. Startup of ago control server agent canceled.");
            return;
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                busClient.removeDevice(serverId, serverType);
                try {
                    busClient.close();
                } catch (final Throwable t) {
                    LOGGER.error("Error in event processor stop.", t);
                }
                shutdown = true;
                mainThread.interrupt();
                try {
                    mainThread.join();
                } catch (InterruptedException e) {
                }
            }
        });

        while(!shutdown) {
            try {
                Thread.sleep(SHUTDOWN_WAIT_SLEEP_MILLIS);
            } catch (final InterruptedException e) {
                LOGGER.debug("Shutdown wait sleep interrupted.");
            }
        }


    }



}
