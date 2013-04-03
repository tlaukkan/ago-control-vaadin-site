/**
 *
 */
package org.agocontrol.site.viewlet.element;

import com.vaadin.ui.Select;
import org.agocontrol.model.ElementType;

/**
 * @author Tommi Laukkanen
 *
 */
public class ElementTypeField extends Select {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor which populates the select with existing customers.
     */
    public ElementTypeField() {
        super();
    }

    @Override
    public final void attach() {
        super.attach();

        for (final ElementType type : ElementType.values()) {
            addItem(type);
        }
    }

}
