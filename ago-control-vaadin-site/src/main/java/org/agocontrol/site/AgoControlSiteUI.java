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
package org.agocontrol.site;

import com.vaadin.annotations.Theme;
import com.vaadin.server.VaadinRequest;
import com.vaadin.server.VaadinService;
import com.vaadin.server.VaadinServletRequest;
import org.agocontrol.client.AgoControlBusClient;
import org.agocontrol.dao.BusDao;
import org.agocontrol.model.Bus;
import org.agocontrol.model.BusConnectionStatus;
import org.agocontrol.site.viewlet.bus.BusFlowViewlet;
import org.agocontrol.site.viewlet.element.ElementFlowViewlet;
import org.apache.log4j.Logger;
import org.apache.log4j.xml.DOMConfigurator;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.webapp.WebAppContext;
import org.eclipse.persistence.config.PersistenceUnitProperties;
import org.vaadin.addons.sitekit.dao.CompanyDao;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractSiteUI;
import org.vaadin.addons.sitekit.site.ContentProvider;
import org.vaadin.addons.sitekit.site.FixedWidthView;
import org.vaadin.addons.sitekit.site.LocalizationProvider;
import org.vaadin.addons.sitekit.site.LocalizationProviderBundleImpl;
import org.vaadin.addons.sitekit.site.NavigationDescriptor;
import org.vaadin.addons.sitekit.site.NavigationVersion;
import org.vaadin.addons.sitekit.site.SecurityProviderSessionImpl;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;
import org.vaadin.addons.sitekit.site.SiteDescriptor;
import org.vaadin.addons.sitekit.site.SiteMode;
import org.vaadin.addons.sitekit.site.ViewDescriptor;
import org.vaadin.addons.sitekit.site.ViewVersion;
import org.vaadin.addons.sitekit.site.ViewletDescriptor;
import org.vaadin.addons.sitekit.util.PropertiesUtil;
import org.vaadin.addons.sitekit.viewlet.administrator.company.CompanyFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.administrator.customer.CustomerFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.administrator.group.GroupFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.administrator.user.UserFlowViewlet;
import org.vaadin.addons.sitekit.viewlet.anonymous.CompanyFooterViewlet;
import org.vaadin.addons.sitekit.viewlet.anonymous.CompanyHeaderViewlet;
import org.vaadin.addons.sitekit.viewlet.anonymous.EmailValidationViewlet;
import org.vaadin.addons.sitekit.viewlet.anonymous.ImageViewlet;
import org.vaadin.addons.sitekit.viewlet.anonymous.NavigationViewlet;
import org.vaadin.addons.sitekit.viewlet.anonymous.login.LoginFlowViewlet;
import org.vaadin.addons.sitekit.web.BareSiteFields;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.Persistence;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * BareSite UI.
 *
 * @author Tommi S.E. Laukkanen
 */
@SuppressWarnings({ "serial", "unchecked" })
@Theme("eelis")
public final class AgoControlSiteUI extends AbstractSiteUI implements ContentProvider {

    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(AgoControlSiteUI.class);
    /** The properties category used in instantiating default services. */
    private static final String PROPERTIES_CATEGORY = "ago-control-vaadin-site";
    /** The persistence unit to be used. */
    public static final String PERSISTENCE_UNIT = "ago-control-vaadin-site";
    /** The shutdown flag for signaling shutdown. */
    private static boolean shutdownRequested = false;

    /**
     * Main method for running BareSiteUI.
     * @param args the commandline arguments
     * @throws Exception if exception occurs in jetty startup.
     */
    public static void main(final String[] args) throws Exception {
        final Thread mainThread = Thread.currentThread();
        DOMConfigurator.configure("./log4j.xml");

        final Map properties = new HashMap();
        properties.put(PersistenceUnitProperties.JDBC_DRIVER, PropertiesUtil.getProperty(
                PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_DRIVER));
        properties.put(PersistenceUnitProperties.JDBC_URL, PropertiesUtil.getProperty(
                PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_URL));
        properties.put(PersistenceUnitProperties.JDBC_USER, PropertiesUtil.getProperty(
                PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_USER));
        properties.put(PersistenceUnitProperties.JDBC_PASSWORD, PropertiesUtil.getProperty(
                PROPERTIES_CATEGORY, PersistenceUnitProperties.JDBC_PASSWORD));
        properties.put(PersistenceUnitProperties.DDL_GENERATION, PropertiesUtil.getProperty(
                PROPERTIES_CATEGORY, PersistenceUnitProperties.DDL_GENERATION));
        entityManagerFactory = Persistence.createEntityManagerFactory(
                PERSISTENCE_UNIT, properties);



        final String webappUrl = AgoControlSiteUI.class.getClassLoader().getResource("webapp/").toExternalForm();

        final Server server = new Server(8081);

        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setDescriptor(webappUrl + "/WEB-INF/web.xml");
        context.setResourceBase(webappUrl);
        context.setParentLoaderPriority(true);

        server.setHandler(context);
        server.start();

        final List<Thread> eventFetchThreads = new ArrayList<>();
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        final List<Company> companies = CompanyDao.getCompanies(entityManager);
        for (final Company company : companies) {

            final List<Bus> buses = BusDao.getBuses(entityManager, company);
            for (final Bus bus : buses) {


                final Thread eventFetchThread = new Thread(new Runnable() {
                    @Override
                    public void run() {

                        final EntityManager threadEntityManager = entityManagerFactory.createEntityManager();
                        final Company threadCompany = threadEntityManager.getReference(Company.class,
                                company.getCompanyId());

                        final AgoControlBusClient client = new AgoControlBusClient(bus.getJsonRpcUrl());
                        String subscriptionId = "";
                        try {
                            subscriptionId= client.subscribe();
                        } catch (final Throwable t) {
                            LOGGER.error("Error in in initial subscription to events..", t);
                        }
                        while (!shutdownRequested) {
                            try {
                                client.processEvents(threadEntityManager, threadCompany);

                                final boolean subscriptionOk =
                                    client.fetch(threadEntityManager, threadCompany, subscriptionId);

                                if (!subscriptionOk) {
                                    subscriptionId = client.subscribe();
                                }

                            } catch (final Throwable t) {
                                LOGGER.error("Error in fetching events..", t);
                                try {
                                    Thread.sleep(10000);
                                } catch (final InterruptedException e) {
                                    break;
                                }
                            }
                            try {
                                Thread.sleep(200);
                            } catch (final InterruptedException e) {
                                LOGGER.debug("Event fetcher thread interrupted.", e);
                                break;
                            }
                        }

                        client.unsubscribe(subscriptionId);

                    }
                }, "Event fetcher: " + bus.getJsonRpcUrl());
                eventFetchThread.start();
                eventFetchThreads.add(eventFetchThread);
            }
        }

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                shutdownRequested = true;
                mainThread.interrupt();
                for (final Thread thread : eventFetchThreads) {
                    thread.interrupt();
                }
            }
        });

        while (!shutdownRequested) {
            try {
                for (final Company company : companies) {

                    final List<Bus> buses = BusDao.getBuses(entityManager, company);
                    int treeIndex = 0;
                    for (final Bus bus : buses) {

                        bus.setConnectionStatus(BusConnectionStatus.Synchronizing);
                        BusDao.saveBuses(entityManager, Collections.singletonList(bus));

                        final AgoControlBusClient client = new AgoControlBusClient(bus.getJsonRpcUrl());

                        boolean success;
                        try {
                            treeIndex = client.synchronizeInventory(entityManager, company, treeIndex);
                            success = true;
                        } catch (final Throwable t) {
                            success = false;
                            LOGGER.error("Failed to synchronize inventory to: " + bus.getJsonRpcUrl(), t);
                        }

                        entityManager.refresh(bus);
                        if (success) {
                            bus.setConnectionStatus(BusConnectionStatus.Connected);
                            bus.setInventorySynchronized(new Date());
                        } else {
                            bus.setConnectionStatus(BusConnectionStatus.Error);
                        }
                        BusDao.saveBuses(entityManager, Collections.singletonList(bus));
                    }
                }
            } catch (final Throwable t) {
                LOGGER.error("Error in inventory synchronization.", t);
            }
            try {
                Thread.sleep(5 * 60000);
            } catch (final InterruptedException e) {
                LOGGER.debug("Interrupt in inventory synchronization loop.", e);
                break;
            }
        }

        server.stop();
    }

    @Override
    protected Site constructSite(final VaadinRequest request) {
        final ContentProvider contentProvider = this;

        final LocalizationProvider localizationProvider =
                new LocalizationProviderBundleImpl(new String[] {"bare-site-localization",
                        "ago-control-vaadin-site-localization"});

        BareSiteFields.initialize(localizationProvider, getLocale());
        AgoControlSiteFields.initialize(localizationProvider, getLocale());

        final SiteContext siteContext = new SiteContext();
        final EntityManager entityManager = entityManagerFactory.createEntityManager();
        siteContext.putObject(EntityManager.class, entityManager);
        siteContext.putObject(Company.class, CompanyDao.getCompany(entityManager,
                ((VaadinServletRequest) VaadinService.getCurrentRequest()).getHttpServletRequest().getServerName()));

        final SecurityProviderSessionImpl securityProvider = new SecurityProviderSessionImpl(
                Arrays.asList("administrator", "user"));

        return new Site(SiteMode.PRODUCTION, contentProvider, localizationProvider, securityProvider, siteContext);
    }

    @Override
    public SiteDescriptor getSiteDescriptor() {
        final List<ViewDescriptor> viewDescriptors = new ArrayList<ViewDescriptor>();

        viewDescriptors.add(new ViewDescriptor("master", null, null, new ViewVersion(0, null, "Master", "",
                "This is a master view.", FixedWidthView.class.getCanonicalName(), new String[]{"admin"},
                Arrays.asList(
                        new ViewletDescriptor("logo", "Logo", "This is logo.", "ago-control-logo.png",
                                ImageViewlet.class.getCanonicalName()),
                        new ViewletDescriptor("header", "Header", "This is header.", null,
                                CompanyHeaderViewlet.class.getCanonicalName()),
                        new ViewletDescriptor("navigation", "NavigationDescriptor", "This is navigation.", null,
                                NavigationViewlet.class.getCanonicalName()),
                        new ViewletDescriptor("footer", "Footer", "This is footer.", null,
                                CompanyFooterViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("default", null, null, new ViewVersion(0, "master", "Default", "",
                "This is default view.", FixedWidthView.class.getCanonicalName(), new String[]{},
                Arrays.asList(new ViewletDescriptor[0])
        )));

        viewDescriptors.add(new ViewDescriptor("buses", null, null, new ViewVersion(
                0, "master", "Buses", "", "This is buses page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Buses Viewlet", "This is Buses viewlet.", null,
                        BusFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("elements", null, null, new ViewVersion(
                0, "master", "Elements", "", "This is elements page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Elements Viewlet", "This is Elements viewlet.", null,
                         ElementFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("users", null, null, new ViewVersion(
                0, "master", "Users", "", "This is users page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        UserFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("groups", null, null, new ViewVersion(
                0, "master", "Groups", "", "This is groups page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        GroupFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("customers", null, null, new ViewVersion(
                0, "master", "Customers", "customers", "This is customers page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        CustomerFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("companies", null, null, new ViewVersion(
                0, "master", "Companies", "companies", "This is companies page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Flowlet Sheet", "This is flow sheet.", null,
                        CompanyFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("login", null, null, new ViewVersion(
                0, "master", "Login SiteView", "login page", "This is login page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"anonymous"},
                Arrays.asList(
                        new ViewletDescriptor(
                                "content", "Flowlet Sheet", "This is flow sheet.", null,
                                LoginFlowViewlet.class.getCanonicalName())
                ))));
        viewDescriptors.add(new ViewDescriptor("validate", null, null, new ViewVersion(
                0, "master", "Email Validation", "email validation page", "This is email validation page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"anonymous"},
                Arrays.asList(
                        new ViewletDescriptor(
                                "content", "Email Validation", "This is email validation flowlet.", null,
                                EmailValidationViewlet.class.getCanonicalName())
                ))));

        final NavigationDescriptor navigationDescriptor = new NavigationDescriptor("navigation", null, null,
                new NavigationVersion(0, "default",
                        "default;buses;elements;customers;users;groups;companies;login", true));

        return new SiteDescriptor("Test site.", "test site", "This is a test site.",
                navigationDescriptor, viewDescriptors);

    }

    /** The entity manager factory for test. */
    private static EntityManagerFactory entityManagerFactory;

}
