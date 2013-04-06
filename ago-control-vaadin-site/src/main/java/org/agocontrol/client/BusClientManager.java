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

import org.agocontrol.dao.BusDao;
import org.agocontrol.model.Bus;
import org.agocontrol.model.BusConnectionStatus;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bus client manager class.
 *
 * @author Tommi S.E. Laukkanen
 */
public class BusClientManager {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(BusClientManager.class);
    /**
     * The entityManagerFactory.
     */
    private EntityManagerFactory entityManagerFactory;
    /**
     * Set true to shutdown threads.
     */
    private boolean closeRequested = false;
    /**
     * The manager thread.
     */
    private final Thread managerThread;
    /**
     * The bus clients.
     */
    private Map<Bus, BusClient> clients = new HashMap<>();

    /**
     * Constructor which allows setting the entity manager factory.
     *
     * @param entityManagerFactory the entityManagerFactory
     */
    public BusClientManager(final EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;

        managerThread = new Thread(new Runnable() {
            @Override
            public void run() {
                final EntityManager entityManager = entityManagerFactory.createEntityManager();
                while (!closeRequested) {
                    try {
                        manageClients(entityManager);
                    } catch (final Throwable t) {
                        LOGGER.error("Error in manager thread.", t);
                    }
                    try {
                        Thread.sleep(30000);
                    } catch (final InterruptedException t) {
                        LOGGER.warn("Interrupt in manager thread sleep.", t);
                    }
                }

                for (final Bus bus : clients.keySet()) {
                    try {
                        final BusClient client = clients.get(bus);
                        client.close();
                        bus.setConnectionStatus(BusConnectionStatus.Disconnected);
                        BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                    } catch (final Exception e) {
                        LOGGER.warn("Exception in bus client close.", e);
                    }
                }

                entityManager.close();
            }
        });

        managerThread.start();
    }

    /**
     * Closes the manager.
     */
    public final void close() {
        closeRequested = true;
        managerThread.interrupt();
        try {
            managerThread.join();
        } catch (InterruptedException e) {
            LOGGER.warn("BusClientManager close wait interrupted.", e);
        }
    }

    /**
     * Manages clients.
     * @param entityManager the entityManager
     */
    private void manageClients(final EntityManager entityManager) {
        final Set<Bus> activeBuses = new HashSet<>();

        final List<Company> companies = CompanyDao.getCompanies(entityManager);
        for (final Company company : companies) {
            for (final Bus bus : BusDao.getBuses(entityManager, company)) {
                if (bus.getJsonRpcUrl() != null && bus.getJsonRpcUrl().length() > 0) {
                    activeBuses.add(bus);
                }
            }
        }

        for (final Bus bus : clients.keySet()) {
            if (!activeBuses.contains(bus)) {
                final BusClient client = clients.remove(bus);
                try {
                    client.close();
                    bus.setConnectionStatus(BusConnectionStatus.Disconnected);
                    BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                } catch (final Exception e) {
                    LOGGER.warn("Exception in bus client close.", e);
                }
            }
        }

        for (final Bus bus : activeBuses) {
            if (!clients.containsKey(bus)) {
                try {
                    clients.put(bus, new BusClient(entityManagerFactory, bus));
                    bus.setConnectionStatus(BusConnectionStatus.Connected);
                    BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                } catch (final Exception e) {
                    LOGGER.warn("Exception in bus client connect.", e);
                    bus.setConnectionStatus(BusConnectionStatus.Error);
                    BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                }
            }
        }

    }
}
