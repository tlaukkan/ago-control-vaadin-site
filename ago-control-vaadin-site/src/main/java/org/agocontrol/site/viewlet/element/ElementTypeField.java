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
