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

import com.vaadin.data.Property;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.ComboBox;
import com.vaadin.ui.Label;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import com.vaadin.ui.themes.Reindeer;
import org.agocontrol.dao.ElementDao;
import org.agocontrol.dao.RecordDao;
import org.agocontrol.dao.RecordSetDao;
import org.agocontrol.model.Element;
import org.agocontrol.model.ElementType;
import org.agocontrol.model.Record;
import org.agocontrol.model.RecordSet;
import org.agocontrol.model.RecordType;
import org.agocontrol.site.AgoControlSiteUI;
import org.agocontrol.site.component.flot.DataSet;
import org.agocontrol.site.component.flot.Flot;
import org.agocontrol.site.component.flot.FlotState;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractViewlet;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;
import sun.security.util.UntrustedCertificates;

import javax.persistence.EntityManager;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
     * The building ID.
     */
    private String buildingId;
    private final ComboBox recordTypeComboBox;


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

        recordTypeComboBox = new ComboBox();
        layout.addComponent(recordTypeComboBox);
        layout.setComponentAlignment(recordTypeComboBox, Alignment.MIDDLE_LEFT);
        layout.setExpandRatio(recordTypeComboBox, 0);
        //buildingComboBox.setWidth(100, Unit.PERCENTAGE);
        recordTypeComboBox.setNullSelectionAllowed(false);
        recordTypeComboBox.setNewItemsAllowed(false);
        recordTypeComboBox.setTextInputAllowed(false);
        recordTypeComboBox.setImmediate(true);
        recordTypeComboBox.setBuffered(false);

        for (final RecordType recordType : RecordType.values()) {
            recordTypeComboBox.addItem(recordType);
        }
        recordTypeComboBox.setValue(RecordType.TEMPERATURE);
        recordTypeComboBox.addValueChangeListener(new Property.ValueChangeListener() {
            @Override
            public void valueChange(final Property.ValueChangeEvent event) {
                refresh();
            }
        });

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
        buildingId = parameters;
        refresh();
    }

    /**
     * Refreshes chart.
     */
    private void refresh() {
        final Company company = siteContext.getObject(Company.class);
        if (company == null || buildingId == null) {
            return;
        }

        final Element building = ElementDao.getElement(entityManager, buildingId);

        final RecordType recordType = (RecordType) recordTypeComboBox.getValue();
        final List<RecordSet> recordSets = new ArrayList<RecordSet>();

        final List<Element> elements = ElementDao.getElements(entityManager, company);
        final Map<String, Element> elementMap = new HashMap<>();

        boolean started = false;
        for (final Element element : elements) {
            elementMap.put(element.getElementId(), element);
            if (element.getElementId().equals(buildingId)) {
                started = true;
                continue;
            }
            if (!started) {
                continue;
            }
            if (element.getTreeDepth() == 0) {
                break;
            }
            if (element.getType() == ElementType.DEVICE) {
                break;
            }

            recordSets.addAll(RecordSetDao.getRecordSetsByParent(
                    entityManager, element, recordType));
        }

        chartLayout.removeAllComponents();

        if (recordSets.size() == 0) {
            return;
        }

        final Flot flot = new Flot();

        final FlotState state = flot.getState();
        state.getOptions("options").put("HtmlText", false);
        state.getOptions("options").put("title", recordType.toString() + "[" + recordSets.get(0).getUnit() + "]");
        state.getOptions("selection").put("mode", "x");
        state.getOptions("xaxis").put("mode", "time");
        state.getOptions("xaxis").put("labelsAngle", Double.valueOf(45));

        for (final RecordSet recordSet : recordSets) {
            final DataSet dataSet = new DataSet();
            final Element element = recordSet.getElement();
            final Element parentElement = elementMap.get(element.getParentId());
            dataSet.setLabel(parentElement.getName() + " / " + element.getName());

            final List<Record> records = RecordDao.getRecords(entityManager, recordSet);
            for (final Record record : records) {
                dataSet.addValue(record.getCreated(), record.getValue().doubleValue());
            }

            state.getDataSets().add(dataSet);
        }

        flot.setHeight(200, Unit.PIXELS);
        //setWidth(400, Unit.PIXELS);
        //setHeight(300, Unit.PIXELS);
        chartLayout.addComponent(flot);

    }

}