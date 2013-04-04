package org.agocontrol.site.viewlet.dashboard;

import com.github.wolfie.refresher.Refresher;
import com.vaadin.server.Resource;
import com.vaadin.ui.Alignment;
import com.vaadin.ui.Embedded;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import com.vaadin.ui.Label;
import com.vaadin.ui.Panel;
import com.vaadin.ui.UI;
import com.vaadin.ui.VerticalLayout;
import org.agocontrol.dao.ElementDao;
import org.agocontrol.dao.RecordDao;
import org.agocontrol.dao.RecordSetDao;
import org.agocontrol.model.Element;
import org.agocontrol.model.Record;
import org.agocontrol.model.RecordSet;
import org.agocontrol.model.RecordType;
import org.agocontrol.site.AgoControlSiteUI;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.Site;
import org.vaadin.addons.sitekit.site.SiteContext;

import javax.persistence.EntityManager;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

/**
 * The building control panel.
 *
 * @author Tommi S.E. Laukkanen
 */
public class BuildingControlPanel extends Panel {
    /** The logger. */
    private static final Logger LOGGER = Logger.getLogger(BuildingControlPanel.class);

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
     * The room icon.
     */
    private final Resource roomIcon;
    /**
     * The device icon.
     */
    private final Resource deviceIcon;
    /**
     * The temperature icon.
     */
    private final Resource temperatureIcon;
    /**
     * The brightness icon.
     */
    private final Resource brightnessIcon;
    /**
     * The humidity icon.
     */
    private final Resource humidityIcon;
    /**
     * The event icon.
     */
    private final Resource eventIcon;
    /**
     * The record layouts.
     */
    private final Map<String, GridLayout> recordsLayouts = new HashMap<>();
    /**
     * The record layouts.
     */
    private final BlockingQueue<List<Record>> recordsQueue = new LinkedBlockingQueue<>();
    /**
     * True if record reader should exit.
     */
    private boolean recordReaderExitRequested = false;
    /**
     * The record thread.
     */
    private Thread recordReaderThread = null;


    /**
     * Default constructor.
     */
    public BuildingControlPanel() {
        site = ((AgoControlSiteUI) UI.getCurrent()).getSite();
        siteContext = site.getSiteContext();
        entityManager = siteContext.getObject(EntityManager.class);

        layout = new VerticalLayout();
        layout.setSpacing(true);
        layout.setMargin(true);

        roomIcon = site.getIcon("room");
        deviceIcon = site.getIcon("device");
        temperatureIcon = site.getIcon("temperature");
        brightnessIcon = site.getIcon("brightness");
        humidityIcon = site.getIcon("humidity");
        eventIcon = site.getIcon("event");

        setContent(layout);

        // the Refresher polls automatically
        final Refresher refresher = new Refresher();
        refresher.setRefreshInterval(200);
        refresher.addListener(new Refresher.RefreshListener() {
            @Override
            public void refresh(final Refresher refresher) {
                while (!recordsQueue.isEmpty()) {
                    final List<Record> records = recordsQueue.remove();
                    if (records.size() > 0) {
                        final Record record = records.get(0);
                        final RecordSet recordSet = record.getRecordSet();
                        final Element element = recordSet.getElement();

                        final GridLayout recordsLayout = recordsLayouts.get(element.getElementId());
                        if (recordsLayout == null) {
                            continue;
                        }

                        final int columnIndex = recordSet.getType().ordinal();
                        final int rowIndex = 0;
                        if (recordsLayout.getComponent(columnIndex, rowIndex) != null) {
                            continue;
                        }

                        final VerticalLayout recordLayout = new VerticalLayout();
                        recordLayout.setSpacing(true);
                        final Resource recordIcon;
                        switch (recordSet.getType()) {
                            case TEMPERATURE:
                                recordIcon = temperatureIcon;
                                break;
                            case BRIGHTNESS:
                                recordIcon = brightnessIcon;
                                break;
                            case HUMIDITY:
                                recordIcon = humidityIcon;
                                break;
                            default:
                                recordIcon = eventIcon;
                                break;
                        }

                        final Embedded embedded = new Embedded(null, recordIcon);
                        recordLayout.addComponent(embedded);
                        recordLayout.setExpandRatio(embedded, 0.1f);
                        embedded.setWidth(32, Unit.PIXELS);
                        embedded.setHeight(32, Unit.PIXELS);


                        final Label label = new Label();
                        recordLayout.addComponent(label);
                        recordLayout.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
                        label.setValue(record.getValue().toString() + " " + recordSet.getUnit());
                        label.setDescription(record.getCreated().toString());

                        recordsLayout.addComponent(recordLayout, columnIndex, rowIndex);
                    }
                }
            }
        });
        addExtension(refresher);

    }

    /**
     * Invoked when view is entered.
     * @param parameters the parameters
     */
    public final synchronized void enter(final String parameters) {
        if (recordReaderThread != null) {
            recordReaderExitRequested = true;
            recordReaderThread.interrupt();
            try {
                recordReaderThread.join();
            } catch (final InterruptedException e) {
                LOGGER.warn("Record reader thread death wait interrupted.");
            }
        }
        layout.removeAllComponents();
        recordsLayouts.clear();
        recordsQueue.clear();
        recordReaderExitRequested = false;

        final Company company = siteContext.getObject(Company.class);
        if (company == null || parameters == null || parameters.length() == 0) {
            return;
        }

        final String buildingId = parameters;
        final List<Element> elements = ElementDao.getElements(entityManager, company);

        boolean started = false;
        for (final Element element : elements) {
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

            final HorizontalLayout elementLayout = new HorizontalLayout();
            layout.addComponent(elementLayout);
            elementLayout.setSpacing(true);

            final Resource elementIcon;
            switch (element.getType()) {
                case ROOM:
                    elementIcon = roomIcon;
                    break;
                case DEVICE:
                    elementIcon = deviceIcon;
                    break;
                default:
                    elementIcon = deviceIcon;
                    break;
            }

            final Embedded embedded = new Embedded(null, elementIcon);
            elementLayout.addComponent(embedded);
            elementLayout.setExpandRatio(embedded, 0.1f);
            embedded.setWidth(32, Unit.PIXELS);
            embedded.setHeight(32, Unit.PIXELS);

            final Label label = new Label();
            elementLayout.addComponent(label);
            elementLayout.setComponentAlignment(label, Alignment.MIDDLE_LEFT);
            label.setValue(element.toString());

            final GridLayout recordLayout = new GridLayout();
            recordLayout.setSpacing(true);
            recordLayout.setColumns(4);
            recordLayout.setRows(1);

            layout.addComponent(recordLayout);
            recordsLayouts.put(element.getElementId(), recordLayout);

            recordReaderThread = new Thread(new Runnable() {
                @Override
                public void run() {
                    try {

                        final List<RecordSet> recordSets = RecordSetDao.getRecordSets(
                                entityManager, element);
                        for (final RecordSet recordSet : recordSets) {
                            recordsQueue.put(RecordDao.getRecords(entityManager, recordSet));
                            if (recordReaderExitRequested) {
                                break;
                            }
                        }

                        Thread.sleep(10);
                    } catch (InterruptedException e) {
                    }
                }
            });
            recordReaderThread.start();
        }
    }

}
