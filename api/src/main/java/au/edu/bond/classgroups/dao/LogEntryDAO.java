package au.edu.bond.classgroups.dao;

import au.edu.bond.classgroups.model.LogEntry;
import au.edu.bond.classgroups.model.Task;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.persistence.EntityManager;
import javax.persistence.EntityManagerFactory;
import javax.persistence.EntityTransaction;
import javax.persistence.TypedQuery;
import java.io.Closeable;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Created by Shane Argo on 11/06/2014.
 */
public class LogEntryDAO implements Closeable {

    private EntityManagerFactory entityManagerFactory;

    @Autowired
    public LogEntryDAO(EntityManagerFactory entityManagerFactory) {
        this.entityManagerFactory = entityManagerFactory;
    }

    public Collection<LogEntry> getAllForTask(Task task) {
        EntityManager entityManager = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            TypedQuery<LogEntry> query = entityManager.createQuery("FROM LogEntry WHERE taskId = :taskId", LogEntry.class);
            query.setParameter("taskId", task.getId());
            return query.getResultList();
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public Collection<LogEntry> getAllForTaskAfterId(Task task, Long id) {
        EntityManager entityManager = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            TypedQuery<LogEntry> query = entityManager.createQuery("FROM LogEntry " +
                    "WHERE taskId = :taskId " +
                    "AND id > :afterId", LogEntry.class);
            query.setParameter("taskId", task.getId());
            query.setParameter("afterId", id);
            return query.getResultList();
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public LogEntry get(long id) {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            return entityManager.find(LogEntry.class, id);
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public void create(LogEntry entry) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();

            entityTransaction.begin();
            entityManager.persist(entry);
            entityTransaction.commit();
        } catch(RuntimeException e) {
            if (entityTransaction != null) {
                try {
                    entityTransaction.rollback();
                } catch (RuntimeException ignored) {
                }
            }
            throw e;
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public void update(LogEntry entry) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();

            entityTransaction.begin();
            entityManager.remove(entry);
            entityTransaction.commit();
        } catch(RuntimeException e) {
            if(entityTransaction != null) {
                try {
                    entityTransaction.rollback();
                } catch(RuntimeException ignored) {
                }
            }
            throw e;
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public void delete(LogEntry entry) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();

            entityTransaction.begin();
            entityManager.remove(entry);
            entityTransaction.commit();
        } catch(RuntimeException e) {
            if(entityTransaction != null) {
                try {
                    entityTransaction.rollback();
                } catch(RuntimeException ignored) {
                }
            }
            throw e;
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }


    public void delete(long id) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();

            entityTransaction.begin();
            entityManager.remove(get(id));
            entityTransaction.commit();
        } catch(RuntimeException e) {
            if(entityTransaction != null) {
                try {
                    entityTransaction.rollback();
                } catch(RuntimeException ignored) {
                }
            }
            throw e;
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    @Override
    public void close() {
        if(entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }
}
