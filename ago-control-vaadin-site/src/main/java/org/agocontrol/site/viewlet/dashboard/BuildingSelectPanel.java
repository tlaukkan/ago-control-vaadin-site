package org.agocontrol.site.viewlet.dashboard;

import com.vaadin.data.Property;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;
import org.agocontrol.dao.ElementDao;
import org.agocontrol.model.Element;
import org.agocontrol.model.ElementType;
import org.agocontrol.site.AgoControlSiteUI;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * The building control panel.
 *
 * @author Tommi S.E. Laukkanen
 */
public class BuildingSelectPanel extends AbstractViewlet {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(BuildingSelectPanel.class);

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
     * The building icon.
     */
    private final Resource buildingIcon;
    /**
     * The building combo box.
     */
    private final ComboBox buildingComboBox;
    /**
     * The rooms layout.
     */
    private final VerticalLayout roomsLayout;
    /**
     * The selected building id.
     */
    private String selectedBuildingId;


    /**
     * Default constructor.
     */
    public BuildingSelectPanel() {
        site = ((AgoControlSiteUI) UI.getCurrent()).getSite();
        siteContext = site.getSiteContext();
        entityManager = siteContext.getObject(EntityManager.class);
        buildingIcon = site.getIcon("building");

        layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);
        layout.setStyleName(Reindeer.LAYOUT_WHITE);

        final HorizontalLayout titleLayout = new HorizontalLayout();
        layout.addComponent(titleLayout);
        titleLayout.setSpacing(true);
        titleLayout.setSizeFull();

        final Embedded embedded = new Embedded(null, buildingIcon);
        titleLayout.addComponent(embedded);
        titleLayout.setExpandRatio(embedded, 0.1f);
        embedded.setWidth(32, Unit.PIXELS);
        embedded.setHeight(32, Unit.PIXELS);

        buildingComboBox = new ComboBox();
        titleLayout.addComponent(buildingComboBox);
        titleLayout.setComponentAlignment(buildingComboBox, Alignment.MIDDLE_LEFT);
        titleLayout.setExpandRatio(buildingComboBox, 0.9f);
        //buildingComboBox.setWidth(100, Unit.PERCENTAGE);
        buildingComboBox.setNullSelectionAllowed(false);
        buildingComboBox.setNewItemsAllowed(false);
        buildingComboBox.setTextInputAllowed(false);
        buildingComboBox.setImmediate(true);
        buildingComboBox.setBuffered(false);

        buildingComboBox.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(final Property.ValueChangeEvent event) {
                final Element building = (Element) buildingComboBox.getValue();
                if (building != null && !building.getElementId().equals(selectedBuildingId)) {
                    UI.getCurrent().getNavigator().navigateTo("default/" + building.getElementId());
                }
            }
        });

        roomsLayout = new VerticalLayout();
        layout.addComponent(roomsLayout);
        roomsLayout.setSpacing(true);
        roomsLayout.setSizeFull();

        setCompositionRoot(layout);
    }

    /**
     * Invoked when view is entered.
     * @param parameters the parameters
     */
    public final void enter(final String parameters) {
        selectedBuildingId = parameters;
        final Company company = siteContext.getObject(Company.class);
        if (company != null) {
            final List<Element> buildings = ElementDao.getElements(entityManager, company, ElementType.BUILDING);
            /*if (selectedBuildingId.length() == 0 && buildings.size() > 0) {
                UI.getCurrent().getNavigator().navigateTo("default/" + buildings.get(0).getElementId());
            }*/

            buildingComboBox.setValue(null);
            buildingComboBox.removeAllItems();
            roomsLayout.removeAllComponents();
            for (final Element building : buildings) {
                buildingComboBox.addItem(building);
                if (building.getElementId().equals(selectedBuildingId)) {
                    buildingComboBox.setValue(building);
                }
            }


        }

    }

}
