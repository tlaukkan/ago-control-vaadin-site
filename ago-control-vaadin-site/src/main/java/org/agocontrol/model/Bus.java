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
package org.agocontrol.model;

import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * Bus.
 *
 * @author Tommi S.E. Laukkanen
 */
@Entity
@Table(name = "bus")
public final class Bus implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String busId;

    /** Owning company. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;

    /** Name. */
    @Column(nullable = false)
    private String name;

    /** JSON RPC URL. */
    @Column(nullable = false)
    private String jsonRpcUrl;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * The default constructor for JPA.
     */
    public Bus() {
        super();
    }

    /**
     * @param owner the owning company
     * @param name the name
     * @param jsonRpcUrl the JSON RPC URL
     * @param created the create time stamp
     */
    public Bus(final Company owner, final String name, final String jsonRpcUrl, final Date created) {
        this.owner = owner;
        this.name = name;
        this.jsonRpcUrl = jsonRpcUrl;
        this.created = created;
    }

    /**
     * @return the owner
     */
    public Company getOwner() {
        return owner;
    }

    /**
     * @param owner the owner to set
     */
    public void setOwner(final Company owner) {
        this.owner = owner;
    }

    /**
     * @return the busId
     */
    public String getBusId() {
        return busId;
    }

    /**
     * @param busId the busId to set
     */
    public void setBusId(final String busId) {
        this.busId = busId;
    }

    /**
     * @return the name
     */
    public String getName() {
        return name;
    }

    /**
     * @param name the name to set
     */
    public void setName(final String name) {
        this.name = name;
    }

    /**
     * @return the jsonRpcUrl
     */
    public String getJsonRpcUrl() {
        return jsonRpcUrl;
    }

    /**
     * @param jsonRpcUrl the jsonRpcUrl to set
     */
    public void setJsonRpcUrl(final String jsonRpcUrl) {
        this.jsonRpcUrl = jsonRpcUrl;
    }

    /**
     * @return the created
     */
    public Date getCreated() {
        return created;
    }

    /**
     * @param created the created to set
     */
    public void setCreated(final Date created) {
        this.created = created;
    }

    /**
     * @return the modified
     */
    public Date getModified() {
        return modified;
    }

    /**
     * @param modified the modified to set
     */
    public void setModified(final Date modified) {
        this.modified = modified;
    }

    @Override
    public String toString() {
        return name;
    }

    @Override
    public int hashCode() {
        return busId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Bus && busId.equals(((Bus) obj).getBusId());
    }

}
