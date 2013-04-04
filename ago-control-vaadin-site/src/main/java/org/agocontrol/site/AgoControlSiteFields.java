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

import com.vaadin.data.Validator;
import com.vaadin.ui.TextField;
import org.agocontrol.model.Bus;
import org.agocontrol.model.BusConnectionStatus;
import org.agocontrol.model.Element;
import org.agocontrol.model.ElementType;
import org.agocontrol.model.Record;
import org.agocontrol.model.RecordSet;
import org.agocontrol.model.RecordType;
import org.agocontrol.site.viewlet.bus.BusConnectionStatusField;
import org.agocontrol.site.viewlet.element.ElementTypeField;
import org.agocontrol.site.viewlet.record.RecordSetField;
import org.agocontrol.site.viewlet.recordset.ElementField;
import org.agocontrol.site.viewlet.recordset.RecordTypeField;
import org.vaadin.addons.sitekit.grid.FieldDescriptor;
import org.vaadin.addons.sitekit.grid.field.TimestampField;
import org.vaadin.addons.sitekit.grid.formatter.BigDecimalFormatter;
import org.vaadin.addons.sitekit.grid.formatter.TimestampFormatter;
import org.vaadin.addons.sitekit.site.LocalizationProvider;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * AgoControl Site field descriptors.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class AgoControlSiteFields {

    /**
     * Private default constructor to disable construction.
     */
    private AgoControlSiteFields() {
    }

    /**
     * Flag reflecting whether initialization of field descriptors has been done
     * for JVM.
     */
    private static boolean initialized = false;

    /**
     * Map of entity class field descriptors.
     */
    private static Map<Class<?>, List<FieldDescriptor>> fieldDescriptors = new HashMap<Class<?>, List<FieldDescriptor>>();

    /**
     * Adds a field descriptor for given entity class.
     * @param entityClass The entity class.
     * @param fieldDescriptor The field descriptor to add.
     */
    public static void add(final Class<?> entityClass, final FieldDescriptor fieldDescriptor) {
        if (!fieldDescriptors.containsKey(entityClass)) {
            fieldDescriptors.put(entityClass, new ArrayList<FieldDescriptor>());
        }
        fieldDescriptors.get(entityClass).add(fieldDescriptor);
    }

    /**
     * Adds a field descriptor for given entity class.
     * @param entityClass The entity class.
     * @param fieldDescriptor The field descriptor to add.
     * @param validator The field validator.
     */
    public static void add(final Class<?> entityClass, final FieldDescriptor fieldDescriptor, final Validator validator) {
        fieldDescriptor.addValidator(validator);
        add(entityClass, fieldDescriptor);
    }

    /**
     * Gets field descriptors for given entity class.
     * @param entityClass The entity class.
     * @return an unmodifiable list of field descriptors.
     */
    public static List<FieldDescriptor> getFieldDescriptors(final Class<?> entityClass) {
        return Collections.unmodifiableList(fieldDescriptors.get(entityClass));
    }

    /**
     * Initialize field descriptors if not done yet.
     * @param localizationProvider the localization provider
     * @param locale the locale
     */
    public static synchronized void initialize(final LocalizationProvider localizationProvider, final Locale locale) {
        if (initialized) {
            return;
        }
        initialized = true;

        AgoControlSiteFields.add(Bus.class, new FieldDescriptor(
               "name", "Name",
                TextField.class, null,
                100, null, String.class, "",
                false, true, true));
        AgoControlSiteFields.add(Bus.class, new FieldDescriptor(
                "jsonRpcUrl", "JSON RPC URL",
                TextField.class, null,
                100, null, String.class, "",
                false, true, true));
        AgoControlSiteFields.add(Bus.class, new FieldDescriptor(
                "connectionStatus", "Connection Status",
                BusConnectionStatusField.class, null,
                100, null, BusConnectionStatus.class, null,
                true, true, true));
        AgoControlSiteFields.add(Bus.class, new FieldDescriptor(
                "inventorySynchronized", "Inventory Synchronized",
                TimestampField.class, TimestampFormatter.class,
                150, null, Date.class, null,
                true, true, true));
        AgoControlSiteFields.add(Bus.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, TimestampFormatter.class,
                150, null, Date.class, null, true,
                true, true));
        AgoControlSiteFields.add(Bus.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, TimestampFormatter.class,
                150, null, Date.class, null,
                true, true, true));

        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "elementId", "Element ID",
                TextField.class, null,
                100, null, String.class, null,
                true, false, false));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "parentId", "Parent ID",
                TextField.class, null,
                100, null, String.class, null,
                true, false, false));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "name", "Name",
                TextField.class, null,
                100, null, String.class, "",
                false, true, true));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "category", "Category",
                TextField.class, null,
                100, null, String.class, "",
                false, true, true));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "type", "Type",
                ElementTypeField.class, null,
                100, null, ElementType.class, null,
                false, true, true));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "treeIndex", "Tree Index",
                TextField.class, null,
                60, null, Integer.class,
                "", false, true, true));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "treeDepth", "Tree Depth",
                TextField.class, null,
                60, null, Integer.class,
                "", false, true, true));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, TimestampFormatter.class,
                150, null, Date.class, null, true,
                true, true));
        AgoControlSiteFields.add(Element.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, TimestampFormatter.class,
                150, null, Date.class, null,
                true, true, true));

        AgoControlSiteFields.add(Record.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, TimestampFormatter.class,
                100, null, Date.class, null, true,
                true, true));
        AgoControlSiteFields.add(Record.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, TimestampFormatter.class,
                100, null, Date.class, null,
                true, true, true));
        AgoControlSiteFields.add(Record.class, new FieldDescriptor(
                "recordSet", "Record Set", RecordSetField.class, null,
                300, null, Element.class,
                null, false, true, true));
        AgoControlSiteFields.add(Record.class, new FieldDescriptor(
                "value", "Value",
                TextField.class, BigDecimalFormatter.class,
                100, null, BigDecimalFormatter.class, "",
                false, true, true));

        AgoControlSiteFields.add(RecordSet.class, new FieldDescriptor(
                "created", "Created",
                TimestampField.class, TimestampFormatter.class,
                150, null, Date.class, null, true,
                true, true));
        AgoControlSiteFields.add(RecordSet.class, new FieldDescriptor(
                "modified", "Modified",
                TimestampField.class, TimestampFormatter.class,
                150, null, Date.class, null,
                true, true, true));
        AgoControlSiteFields.add(RecordSet.class, new FieldDescriptor(
                "element", "Element", ElementField.class, null,
                100, null, Element.class,
                null, false, true, true));
        AgoControlSiteFields.add(RecordSet.class, new FieldDescriptor(
                "name", "Name",
                TextField.class, null,
                300, null, String.class, "",
                false, true, true));
        AgoControlSiteFields.add(RecordSet.class, new FieldDescriptor(
                "type", "Type",
                RecordTypeField.class, null,
                100, null, RecordType.class, "",
                false, true, true));
        AgoControlSiteFields.add(RecordSet.class, new FieldDescriptor(
                "unit", "Unit",
                TextField.class, null,
                25, null, String.class, "",
                false, false, true));


    }
}
