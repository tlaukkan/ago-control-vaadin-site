package org.agocontrol.dao;

import org.agocontrol.model.Element;
import org.agocontrol.model.RecordSet;
import org.apache.log4j.Logger;
import org.vaadin.addons.sitekit.model.Company;

import javax.persistence.EntityManager;
import javax.persistence.EntityNotFoundException;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.util.Date;
import java.util.List;

/**
 * @author Tommi S.E. Laukkanen
 *
 */
public final class RecordSetDao {

    /** The logger. */
    private static final Logger LOG = Logger.getLogger(RecordSetDao.class);

    /**
     * Saves recordSets to database.
     * @param entityManager the entity manager
     * @param recordSets the recordSets
     */
    public static void saveRecordSets(final EntityManager entityManager, final List<RecordSet> recordSets) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            for (final RecordSet recordSet : recordSets) {
                recordSet.setModified(new Date());
                entityManager.persist(recordSet);
            }
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in add recordSet.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Removes recordSet from database.
     * @param entityManager the entity manager
     * @param recordSet the recordSet
     */
    public static void removeRecordSet(final EntityManager entityManager, final RecordSet recordSet) {
        final EntityTransaction transaction = entityManager.getTransaction();
        transaction.begin();
        try {
            entityManager.remove(recordSet);
            transaction.commit();
        } catch (final Exception e) {
            LOG.error("Error in remove recordSet.", e);
            if (transaction.isActive()) {
                transaction.rollback();
            }
            throw new RuntimeException(e);
        }
    }

    /**
     * Gets given recordSet.
     * @param entityManager the entity manager.
     * @param element the element
     * @param name the name of the recordSet
     * @return the recordSet
     */
    public static RecordSet getRecordSet(final EntityManager entityManager, final Element element, final String name) {
        final TypedQuery<RecordSet> query = entityManager.createQuery(
                "select e from RecordSet as e where e.element=:element and e.name=:name",
                RecordSet.class);
        query.setParameter("element", element);
        query.setParameter("name", name);
        final List<RecordSet> recordSets = query.getResultList();
        if (recordSets.size() == 1) {
            return recordSets.get(0);
        } else if (recordSets.size() == 0) {
            return null;
        } else {
            throw new RuntimeException("Multiple recordSets with same element and name in database. Constraint is missing.");
        }
    }

    /**
     * Gets given recordSet.
     * @param entityManager the entity manager.
     * @param owner the owning company
     * @return list of recordSets.
     */
    public static List<RecordSet> getUnprocessedRecordSets(final EntityManager entityManager, final Company owner) {
        final TypedQuery<RecordSet> query = entityManager.createQuery("select e from RecordSet as e where e.owner=:owner" +
                " and e.processed is null order by e.created",
                RecordSet.class);
        query.setParameter("owner", owner);
        return query.getResultList();
    }

    /**
     * Gets given recordSet.
     * @param entityManager the entity manager.
     * @param id the name of the recordSet
     * @return the recordSet
     */
    public static RecordSet getRecordSet(final EntityManager entityManager, final String id) {
        try {
            return entityManager.getReference(RecordSet.class, id);
        } catch (final EntityNotFoundException e) {
            return null;
        }
    }


}
