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
import org.agocontrol.client.AgoControlBusRpcClient;
import org.agocontrol.client.BusClientManager;
import org.agocontrol.client.EventProcessor;
import org.agocontrol.dao.BusDao;
import org.agocontrol.model.Bus;
import org.agocontrol.model.BusConnectionStatus;
import org.agocontrol.site.viewlet.bus.BusFlowViewlet;
import org.agocontrol.site.viewlet.dashboard.DashboardViewlet;
import org.agocontrol.site.viewlet.element.ElementFlowViewlet;
import org.agocontrol.site.viewlet.record.RecordFlowViewlet;
import org.agocontrol.site.viewlet.recordset.RecordSetFlowViewlet;
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
import org.vaadin.addons.sitekit.util.PersistenceUtil;
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

    /**
     * Main method for running BareSiteUI.
     * @param args the commandline arguments
     * @throws Exception if exception occurs in jetty startup.
     */
    public static void main(final String[] args) throws Exception {
        final Thread mainThread = Thread.currentThread();
        DOMConfigurator.configure("./log4j.xml");

        /*final Map properties = new HashMap();
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
                PERSISTENCE_UNIT, properties);*/

        entityManagerFactory = PersistenceUtil.getEntityManagerFactory(PERSISTENCE_UNIT, PROPERTIES_CATEGORY);

        final String webappUrl = AgoControlSiteUI.class.getClassLoader().getResource("webapp/").toExternalForm();

        final int port = Integer.parseInt(PropertiesUtil.getProperty("ago-control-vaadin-site", "port"));
        final Server server = new Server(port);

        final WebAppContext context = new WebAppContext();
        context.setContextPath("/");
        context.setDescriptor(webappUrl + "/WEB-INF/web.xml");
        context.setResourceBase(webappUrl);
        context.setParentLoaderPriority(true);

        server.setHandler(context);
        server.start();

        final BusClientManager busClientManager = new BusClientManager(entityManagerFactory);
        final EventProcessor eventProcessor = new EventProcessor(entityManagerFactory);

        Runtime.getRuntime().addShutdownHook(new Thread() {
            public void run() {
                try {
                    eventProcessor.close();
                } catch (final Throwable t) {
                    LOGGER.error("Error in event processor stop.", t);
                }
                try {
                    busClientManager.close();
                } catch (final Throwable t) {
                    LOGGER.error("Error in bus client manager stop.", t);
                }
                    try {
                    server.stop();
                } catch (final Throwable t) {
                    LOGGER.error("Error in jetty server stop.", t);
                }
            }
        });

        server.join();
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
        siteContext.putObject(EntityManagerFactory.class, entityManagerFactory);
        siteContext.putObject(EntityManager.class, entityManager);

        Company company = CompanyDao.getCompany(entityManager,
                ((VaadinServletRequest) VaadinService.getCurrentRequest()).getHttpServletRequest().getServerName());
        if (company == null) {
            // If no exact host match exists then try to find global company marked with *.
            company = CompanyDao.getCompany(entityManager, "*");
        }
        siteContext.putObject(Company.class, company);

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

        /*final EntityManager entityManager = getSite().getSiteContext().getObject(EntityManager.class);
        final Company company = getSite().getSiteContext().getObject(Company.class);
        if (company != null) {
            final List<Element> buildings = ElementDao.getElements(entityManager, company, ElementType.BUILDING);
            for (final Element building : buildings) {
                viewDescriptors.add(new ViewDescriptor(building.getName(), null, null,
                        new ViewVersion(0, "master", building.getName(), "",
                        "", FixedWidthView.class.getCanonicalName(), new String[]{},
                        Arrays.asList(new ViewletDescriptor[0])
                )));
            }
        }*/

        viewDescriptors.add(new ViewDescriptor("default", null, null, new ViewVersion(0, "master", "Default", "",
                "This is default view.", FixedWidthView.class.getCanonicalName(), new String[]{},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Dashboard Viewlet", "This is Dashboard viewlet.", null,
                        DashboardViewlet.class.getCanonicalName())
                )
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

        viewDescriptors.add(new ViewDescriptor("records", null, null, new ViewVersion(
                0, "master", "Records", "", "This is records page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Records Viewlet", "This is Records viewlet.", null,
                        RecordFlowViewlet.class.getCanonicalName())
                ))));

        viewDescriptors.add(new ViewDescriptor("record-sets", null, null, new ViewVersion(
                0, "master", "Record Sets", "", "This is record sets page.",
                FixedWidthView.class.getCanonicalName(), new String[]{"administrator"},
                Arrays.asList(new ViewletDescriptor(
                        "content", "Record Sets Viewlet", "This is Record Sets viewlet.", null,
                        RecordSetFlowViewlet.class.getCanonicalName())
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
                        "default;buses;elements;records;record-sets;customers;users;groups;companies;login", true));

        return new SiteDescriptor("Test site.", "test site", "This is a test site.",
                navigationDescriptor, viewDescriptors);

    }

    /** The entity manager factory for test. */
    private static EntityManagerFactory entityManagerFactory;

}
