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
import java.util.UUID;

/**
 * The inventory element.
 */
@Entity
public final class Element implements Serializable, Comparable<Element> {
    /** Java serialization version UID. */
    private static final long serialVersionUID = 1L;

    /** Unique UUID of the entity. */
    @Id
    private String elementId;

    /** Parent ID of the parent entity or own id if root. */
    @Column(nullable = false)
    private String parentId;


    /** Owning company. */
    @JoinColumn(nullable = false)
    @ManyToOne(cascade = { CascadeType.DETACH, CascadeType.MERGE, CascadeType.REFRESH }, optional = false)
    private Company owner;

    /** Type. */
    @Column(nullable = false)
    @Enumerated(EnumType.ORDINAL)
    private ElementType type;

    /** Element name. */
    @Column(nullable = false)
    private String name;

    /** Category. */
    @Column(nullable = false)
    private String category;

    /** Inventory tree index. */
    @Column(nullable = false)
    private int treeIndex;

    /** Inventory tree depth. */
    @Column(nullable = false)
    private int treeDepth;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date created;

    /** Created time of the task. */
    @Temporal(TemporalType.TIMESTAMP)
    @Column(nullable = false)
    private Date modified;

    /**
     * Default constructor.
     */
    public Element() {
    }

    /**
     * Constructor for initializing element fields.
     * @param owner the owning company
     * @param type the type
     * @param name the name
     * @param category the category
     */
    public Element(final Company owner, final ElementType type, final String name, final String category) {
        this.created = new Date();
        this.elementId = UUID.randomUUID().toString().toUpperCase();
        this.parentId = elementId;
        this.owner = owner;
        this.type = type;
        this.name = name;
        this.category = category;
    }

    /**
     * @param elementId the elementId
     * @param parentId the parentId
     * @param owner the owner
     * @param type the type
     * @param name the name
     * @param category the category
     */
    public Element(final String elementId, final String parentId, final Company owner, final ElementType type,
                   final String name, final String category) {
        this.created = new Date();
        this.elementId = elementId;
        this.parentId = parentId;
        this.owner = owner;
        this.type = type;
        this.name = name;
        this.category = category;
    }

    /**
     * @param parentId the elementId
     * @param owner the owner
     * @param type the type
     * @param name the name
     * @param category the category
     */
    public Element(final String parentId, final Company owner, final ElementType type,
                   final String name, final String category) {
        this.created = new Date();
        this.elementId = UUID.randomUUID().toString().toUpperCase();
        this.parentId = parentId;
        this.owner = owner;
        this.type = type;
        this.name = name;
        this.category = category;
    }

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
     * @return the parentId
     */
    public String getParentId() {
        return parentId;
    }

    /**
     * @param parentId the parentId to set
     */
    public void setParentId(final String parentId) {
        this.parentId = parentId;
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
        return name;
    }

    @Override
    public int hashCode() {
        return elementId.hashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        return obj != null && obj instanceof Element && elementId.equals(((Element) obj).getElementId());
    }

    @Override
    public int compareTo(final Element o) {
        return toString().compareTo(o.toString());
    }
}
