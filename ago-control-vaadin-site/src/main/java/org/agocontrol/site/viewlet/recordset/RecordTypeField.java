/**
 *
 */
package org.agocontrol.site.viewlet.recordset;

import com.vaadin.ui.Select;
import org.agocontrol.model.RecordType;

/**
 * @author Tommi Laukkanen
 *
 */
public class RecordTypeField extends Select {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor which populates the select with existing customers.
     */
    public RecordTypeField() {
        super();
    }

    @Override
    public final void attach() {
        super.attach();

        for (final RecordType type : RecordType.values()) {
            addItem(type);
        }
    }

}
