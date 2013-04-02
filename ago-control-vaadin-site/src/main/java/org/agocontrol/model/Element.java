package org.agocontrol.model;

import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.EnumType;
import javax.persistence.Enumerated;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * The inventory element.
 */
@Entity
public final class Element implements Serializable {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    @GeneratedValue(generator = "uuid")
    private String elementId;

    /** The bus. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Bus bus;

    /** Type. */
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private ElementType type;

    /** Category. */
    @Column(nullable = false)
    private String category;

    /** Inventory tree index. */
    @Column(nullable = false)
    private int treeIndex;

    /** Inventory tree depth. */
    @Column(nullable = false)
    private int treeDepth;

    /** Element name. */
    @Column(nullable = false)
    private String name;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * @return the elementID
     */
    public String getElementId() {
        return elementId;
    }

    /**
     * @param elementId the elementId
     */
    public void setElementId(final String elementId) {
        this.elementId = elementId;
    }

    /**
     * @return the Bus
     */
    public Bus getBus() {
        return bus;
    }

    /**
     * @param bus the Bus
     */
    public void setBus(final Bus bus) {
        this.bus = bus;
    }

    /**
     * @return the tree depth
     */
    public int getTreeDepth() {
        return treeDepth;
    }

    /**
     * @param treeDepth the tree depth
     */
    public void setTreeDepth(final int treeDepth) {
        this.treeDepth = treeDepth;
    }

    /**
     * @return the type
     */
    public ElementType getType() {
        return type;
    }

    /**
     * @param type the type to set
     */
    public void setType(final ElementType type) {
        this.type = type;
    }

    /**
     * @return the category
     */
    public String getCategory() {
        return category;
    }

    /**
     * @param category the category to set
     */
    public void setCategory(final String category) {
        this.category = category;
    }

    /**
     * @return the treeIndex
     */
    public int getTreeIndex() {
        return treeIndex;
    }

    /**
     * @param treeIndex the treeIndex to set
     */
    public void setTreeIndex(final int treeIndex) {
        this.treeIndex = treeIndex;
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
        return treeIndex + " " + name;
    }

    @Override
    public int hashCode() {
        return elementId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Element && elementId.equals(((Element) obj).getElementId());
    }

}
