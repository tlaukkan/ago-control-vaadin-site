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
package org.agocontrol.site.viewlet.record;

import com.vaadin.ui.Select;
import com.vaadin.ui.UI;
import org.agocontrol.model.RecordSet;
import org.agocontrol.site.AgoControlSiteUI;
import org.vaadin.addons.sitekit.model.Company;
import org.vaadin.addons.sitekit.site.AbstractSiteUI;

import javax.persistence.EntityManager;
import javax.persistence.TypedQuery;
import javax.persistence.criteria.CriteriaBuilder;
import javax.persistence.criteria.CriteriaQuery;
import javax.persistence.criteria.Root;

/**
 * @author Tommi S.E. Laukkanen
 *
 */
public class RecordSetField extends Select {
    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /**
     * Default constructor which populates the select with existing customers.
     */
    public RecordSetField() {
        super();
    }

    @Override
    public final void attach() {
        super.attach();
        final EntityManager entityManager = ((AbstractSiteUI) getUI().getUI()).getSite().getSiteContext().getObject(
                EntityManager.class);
        final CriteriaBuilder queryBuilder = entityManager.getCriteriaBuilder();
        final Company company = ((AgoControlSiteUI) UI.getCurrent()).getSite().getSiteContext().getObject(
                Company.class);
        final CriteriaQuery<RecordSet> criteriaQuery = queryBuilder.createQuery(RecordSet.class);
        final Root<RecordSet> root = criteriaQuery.from(RecordSet.class);
        criteriaQuery.where(queryBuilder.equal(root.get("owner"), company));
        criteriaQuery.orderBy(queryBuilder.asc(root.get("name")));
        final TypedQuery<RecordSet> elementQuery = entityManager.createQuery(criteriaQuery);

        for (final RecordSet element : elementQuery.getResultList()) {
            addItem(element);
        }
    }

}
