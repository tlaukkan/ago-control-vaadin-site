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
package org.agocontrol.site.viewlet.recordset;

import com.vaadin.data.util.BeanItem;
import com.vaadin.ui.Button;
import com.vaadin.ui.Button.ClickEvent;
import com.vaadin.ui.Button.ClickListener;
import com.vaadin.ui.GridLayout;
import com.vaadin.ui.HorizontalLayout;
import org.agocontrol.model.RecordSet;
import org.agocontrol.site.AgoControlSiteFields;
import org.vaadin.addons.sitekit.flow.AbstractFlowlet;
import org.vaadin.addons.sitekit.grid.ValidatingEditor;
import org.vaadin.addons.sitekit.grid.ValidatingEditorStateListener;

import javax.persistence.EntityManager;

/**
 * RecordSet edit flow.
 *
 * @author Tommi S.E. Laukkanen
 */
public final class RecordSetFlowlet extends AbstractFlowlet implements ValidatingEditorStateListener {

    /** Serial version UID. */
    private static final long serialVersionUID = 1L;

    /** The entity manager. */
    private EntityManager entityManager;
    /** The recordSet flow. */
    private RecordSet entity;

    /** The entity form. */
    private ValidatingEditor recordSetEditor;
    /** The save button. */
    private Button saveButton;
    /** The discard button. */
    private Button discardButton;

    @Override
    public String getFlowletKey() {
        return "record-set";
    }

    @Override
    public boolean isDirty() {
        return recordSetEditor.isModified();
    }

    @Override
    public boolean isValid() {
        return recordSetEditor.isValid();
    }

    @Override
    public void initialize() {
        entityManager = getSite().getSiteContext().getObject(EntityManager.class);

        final GridLayout gridLayout = new GridLayout(1, 2);
        gridLayout.setSizeFull();
        gridLayout.setMargin(false);
        gridLayout.setSpacing(true);
        gridLayout.setRowExpandRatio(1, 1f);
        setViewContent(gridLayout);

        recordSetEditor = new ValidatingEditor(AgoControlSiteFields.getFieldDescriptors(RecordSet.class));
        recordSetEditor.setCaption("RecordSet");
        recordSetEditor.addListener((ValidatingEditorStateListener) this);
        gridLayout.addComponent(recordSetEditor, 0, 0);

        final HorizontalLayout buttonLayout = new HorizontalLayout();
        buttonLayout.setSpacing(true);
        gridLayout.addComponent(buttonLayout, 0, 1);

        saveButton = new Button("Save");
        saveButton.setImmediate(true);
        buttonLayout.addComponent(saveButton);
        saveButton.addListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                recordSetEditor.commit();
                entityManager.getTransaction().begin();
                try {
                    entity = entityManager.merge(entity);
                    entityManager.persist(entity);
                    entityManager.getTransaction().commit();
                    entityManager.detach(entity);
                } catch (final Throwable t) {
                    if (entityManager.getTransaction().isActive()) {
                        entityManager.getTransaction().rollback();
                    }
                    throw new RuntimeException("Failed to save entity: " + entity, t);
                }
            }
        });

        discardButton = new Button("Discard");
        discardButton.setImmediate(true);
        buttonLayout.addComponent(discardButton);
        discardButton.addListener(new ClickListener() {
            /** Serial version UID. */
            private static final long serialVersionUID = 1L;

            @Override
            public void buttonClick(final ClickEvent event) {
                recordSetEditor.discard();
            }
        });

    }

    /**
     * Edit an existing recordSet.
     * @param entity entity to be edited.
     * @param newEntity true if entity to be edited is new.
     */
    public void edit(final RecordSet entity, final boolean newEntity) {
        this.entity = entity;
        recordSetEditor.setItem(new BeanItem<RecordSet>(entity), newEntity);
    }

    @Override
    public void editorStateChanged(final ValidatingEditor source) {
        if (isDirty()) {
            if (isValid()) {
                saveButton.setEnabled(true);
            } else {
                saveButton.setEnabled(false);
            }
            discardButton.setEnabled(true);
        } else {
            saveButton.setEnabled(false);
            discardButton.setEnabled(false);
        }
    }

    @Override
    public void enter() {
    }

}
