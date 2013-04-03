/**
 *
 */
package org.agocontrol.site.viewlet.bus;

import com.vaadin.ui.Select;
import org.agocontrol.model.BusConnectionStatus;
import org.agocontrol.model.ElementType;

/**
 * @author Tommi Laukkanen
 *
 */
public class BusConnectionStatusField extends Select {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor which populates the select with existing customers.
     */
    public BusConnectionStatusField() {
        super();
    }

    @Override
    public final void attach() {
        super.attach();

        for (final BusConnectionStatus type : BusConnectionStatus.values()) {
            addItem(type);
        }
    }

}
