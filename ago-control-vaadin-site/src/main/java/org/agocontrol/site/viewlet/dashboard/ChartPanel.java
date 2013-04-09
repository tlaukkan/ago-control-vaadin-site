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
package org.agocontrol.site.viewlet.dashboard;

import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;
import org.agocontrol.dao.ElementDao;
import org.agocontrol.model.Element;
import org.agocontrol.model.ElementType;
import org.agocontrol.site.AgoControlSiteUI;
import org.agocontrol.site.component.flot.Flot;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;
import sun.security.util.UntrustedCertificates;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * The building control panel.
 *
 * @author Tommi S.E. Laukkanen
 */
public class ChartPanel extends AbstractViewlet {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(ChartPanel.class);

    /**
     * The layout.
     */
    private final VerticalLayout layout;
    /**
     * The site.
     */
    private final Site site;
    /**
     * The site context.
     */
    private final SiteContext siteContext;
    /**
     * The entity manager.
     */
    private final EntityManager entityManager;
    /**
     * The rooms layout.
     */
    private final VerticalLayout chartLayout;


    /**
     * Default constructor.
     */
    public ChartPanel() {
        site = ((AgoControlSiteUI) UI.getCurrent()).getSite();
        siteContext = site.getSiteContext();
        entityManager = siteContext.getObject(EntityManager.class);

        layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setSizeFull();
        layout.setStyleName(Reindeer.LAYOUT_WHITE);

        final Label title = new Label("Record Chart");
        title.setIcon(getSite().getIcon("folder"));
        title.setStyleName(Reindeer.LABEL_H2);
        layout.addComponent(title);
        layout.setExpandRatio(title, 0);

        chartLayout = new VerticalLayout();
        layout.addComponent(chartLayout);
        layout.setExpandRatio(chartLayout, 1);
        chartLayout.setSpacing(true);
        chartLayout.setSizeFull();

        setCompositionRoot(layout);
    }

    /**
     * Invoked when view is entered.
     * @param parameters the parameters
     */
    public final void enter(final String parameters) {
        final Company company = siteContext.getObject(Company.class);
        if (company != null) {
            final List<Element> buildings = ElementDao.getElements(entityManager, company, ElementType.BUILDING);
            chartLayout.removeAllComponents();

            final Flot flot = new Flot();
            flot.setHeight(250, Unit.PIXELS);
            //setWidth(400, Unit.PIXELS);
            //setHeight(300, Unit.PIXELS);
            chartLayout.addComponent(flot);
        }

    }

}
