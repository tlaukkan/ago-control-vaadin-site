package org.agocontrol.site.viewlet.dashboard;

import com.vaadin.ui.GridLayout;
import com.vaadin.ui.UI;
import org.agocontrol.dao.ElementDao;
import org.agocontrol.model.Element;
import org.agocontrol.model.ElementType;
import org.agocontrol.site.AgoControlSiteUI;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * ago control site dashboard.
 *
 * @author  Tommi S.E. Laukkanen
 */
public class DashboardViewlet extends AbstractViewlet {

    final BuildingSelectPanel buildingSelectPanel;

    final BuildingControlPanel buildingControlPanel;
    private final Site site;
    private final SiteContext siteContext;
    private final EntityManager entityManager;
    private final EventPanel eventPanel;

    public DashboardViewlet() {
        site = ((AgoControlSiteUI) UI.getCurrent()).getSite();
        siteContext = getSite().getSiteContext();
        entityManager = siteContext.getObject(EntityManager.class);

        final GridLayout gridLayout = new GridLayout();
        gridLayout.setSizeFull();
        gridLayout.setRows(2);
        gridLayout.setColumns(2);
        gridLayout.setSpacing(true);
        gridLayout.setColumnExpandRatio(0, 0.5f);
        gridLayout.setColumnExpandRatio(1, 0.5f);
        gridLayout.setRowExpandRatio(0, 0);
        gridLayout.setRowExpandRatio(1, 1);


        buildingSelectPanel = new BuildingSelectPanel();
        //buildingSelectPanel.setSizeFull();
        gridLayout.addComponent(buildingSelectPanel, 0, 0);

        buildingControlPanel = new BuildingControlPanel();
        buildingControlPanel.setSizeFull();
        gridLayout.addComponent(buildingControlPanel, 0, 1);

        eventPanel = new EventPanel();
        eventPanel.setSizeFull();
        gridLayout.addComponent(eventPanel, 1,1);

        setCompositionRoot(gridLayout);
    }

    @Override
    public void enter(final String parameters) {
        final Company company = siteContext.getObject(Company.class);

        final List<Element> buildings = ElementDao.getElements(entityManager, company, ElementType.BUILDING);
        if (parameters.length() == 0 && buildings.size() > 0) {
            UI.getCurrent().getNavigator().navigateTo("default/" + buildings.get(0).getElementId());
        } else {
            buildingSelectPanel.enter(parameters);
            buildingControlPanel.enter(parameters);
        }

        eventPanel.enter(parameters);
    }

}
