package au.edu.bond.classgroups.dao;

import au.edu.bond.classgroups.model.Task;
import au.edu.bond.classgroups.util.DbUtil;
import org.hibernate.Session;
import org.hibernate.jdbc.Work;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.*;
import java.io.Closeable;
import java.sql.CallableStatement;
import java.sql.Connection;
import java.sql.SQLException;
import java.sql.Types;
import java.util.*;

/**
 * Created by Shane Argo on 11/06/2014.
 */
public class TaskDAO implements Closeable {

    private EntityManagerFactory entityManagerFactory;
    private DbUtil dbUtil;

    @Autowired
    public TaskDAO(EntityManagerFactory entityManagerFactory, DbUtil dbUtil) {
        this.entityManagerFactory = entityManagerFactory;
        this.dbUtil = dbUtil;
    }

    public List<Task> getByStatus(Task.Status status, EntityManager entityManager) {
        final HashSet<Task.Status> statuses = new HashSet<Task.Status>(1);
        statuses.add(status);
        return getByStatus(statuses, entityManager);
    }

    public List<Task> getByStatus(Task.Status status) {
        final HashSet<Task.Status> statuses = new HashSet<Task.Status>(1);
        statuses.add(status);
        return getByStatus(statuses);
    }

    public List<Task> getByStatus(Collection<Task.Status> statuses, EntityManager entityManager) {
        TypedQuery<Task> query = entityManager.createQuery("FROM Task " +
                "WHERE status IN (:statuses) " +
                "ORDER BY enteredDate DESC, id DESC", Task.class);
        query.setParameter("statuses", statuses);
        return query.getResultList();
    }

    public List<Task> getByStatus(Collection<Task.Status> statuses) {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            return getByStatus(statuses, entityManager);
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public Task getNextPending(EntityManager entityManager) {
        TypedQuery<Task> query = entityManager.createQuery("FROM Task " +
                "WHERE status = :pending_status " +
                "AND NOT EXISTS (FROM Task WHERE status = :processing_status) " +
                "ORDER BY enteredDate, id", Task.class);
        query.setMaxResults(1);
        query.setParameter("pending_status", Task.Status.PENDING);
        query.setParameter("processing_status", Task.Status.PROCESSING);
        return query.getSingleResult();
    }

    public Task getNextPending() {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            return getNextPending(entityManager);
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public Collection<Task> getAll(EntityManager entityManager) {
        TypedQuery<Task> query = entityManager.createQuery("FROM Task", Task.class);
        return query.getResultList();
    }

    public Collection<Task> getAll() {
        EntityManager entityManager = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            return getAll(entityManager);
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public Task getMostRecentlyStarted(EntityManager entityManager) {
        TypedQuery<Task> query = entityManager.createQuery("FROM Task " +
                "WHERE startedDate is not null " +
                "ORDER BY startedDate DESC", Task.class).setMaxResults(1);
        return query.getSingleResult();
    }

    public Task getMostRecentlyStarted() {
        EntityManager entityManager = null;
        try {
        entityManager = entityManagerFactory.createEntityManager();
        return getMostRecentlyStarted(entityManager);
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public Task get(long id, EntityManager entityManager) {
        return entityManager.find(Task.class, id);
    }

    public Task get(long id) {
        EntityManager entityManager = null;
        try {
        entityManager = entityManagerFactory.createEntityManager();
        return get(id, entityManager);
        } finally {
            if(entityManager != null) {
                entityManager.close();
            }
        }
    }

    public void create(Task task, EntityManager entityManager) {
        entityManager.persist(task);
    }

    public void create(Task task) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;
        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();

            entityTransaction.begin();
            create(task, entityManager);
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

    public void update(Task task, EntityManager entityManager) {
        entityManager.merge(task);
    }

    public void update(Task task) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            update(task, entityManager);
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

    public void delete(Task task, EntityManager entityManager) {
        entityManager.remove(task);
    }

    public void delete(Task task) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            delete(task, entityManager);
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

    public void delete(long id, EntityManager entityManager) {
        final Task task = get(id, entityManager);
        delete(task, entityManager);
    }

    public void delete(long id) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            delete(id, entityManager);
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

    public Collection<Long> deleteOlderThan(Date date, EntityManager entityManager) {
        TypedQuery<Task> q = entityManager.createQuery("FROM Task WHERE enteredDate < :date", Task.class);
        q.setParameter("date", date);
        final List<Task> resultList = q.getResultList();
        final Set<Long> deletedIds = new HashSet<>();
        for (Task task : resultList) {
            deletedIds.add(task.getId());
            delete(task, entityManager);
        }
        return deletedIds;
    }

    public Collection<Long> deleteOlderThan(Date date) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            final Collection<Long> deletedIds = deleteOlderThan(date, entityManager);
            entityTransaction.commit();

            return deletedIds;
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

    public Task beginScheduled(long id, String node) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();
            entityTransaction.begin();

            Task returned = beginScheduled(id, node, entityManager);
            entityTransaction.commit();
            return returned;
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

    public Task beginScheduled(long id, String node, EntityManager entityManager) {
        final Query updateQuery = entityManager.createQuery("UPDATE Task " +
                "SET status = :newstatus, " +
                "processingNode = :node " +
                "WHERE id = :id " +
                "AND processingNode = null " +
                "AND status = :oldstatus");

        updateQuery.setParameter("newstatus", Task.Status.PENDING);
        updateQuery.setParameter("oldstatus", Task.Status.SCHEDULED);
        updateQuery.setParameter("node", node);
        updateQuery.setParameter("id", id);

        final int count = updateQuery.executeUpdate();
//        System.out.printf("Updated count: %s%n", count);
        if(count != 1) {
            return null;
        }

        TypedQuery<Task> selectQuery = entityManager.createQuery("FROM Task " +
                "WHERE status = :status " +
                "AND id = :id " +
                "AND processingNode = :node", Task.class);
        selectQuery.setParameter("status", Task.Status.PENDING);
        selectQuery.setParameter("id", id);
        selectQuery.setParameter("node", node);

        final List<Task> tasks = selectQuery.getResultList();
//        System.out.printf("Selected Count: %s%n", tasks.size());
        if(tasks.size() != 1) {
            return null;
        }

        return tasks.get(0);
    }

    public Task createScheduled(Task task) {
        EntityManager entityManager = null;
        EntityTransaction entityTransaction = null;

        try {
            entityManager = entityManagerFactory.createEntityManager();
            entityTransaction = entityManager.getTransaction();

            entityTransaction.begin();
            Task returned = createScheduled(task, entityManager);
            entityTransaction.commit();
            return returned;
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

    public Task createScheduled(final Task task, EntityManager entityManager) {
        assert task.getStatus() == Task.Status.SCHEDULED;

        final String queryString = dbUtil.createScheduleQuery(task);
        Session session = entityManager.unwrap(Session.class);
        session.doWork(new Work() {
            @Override
            public void execute(Connection connection) throws SQLException {
                final CallableStatement insertQuery = connection.prepareCall(queryString);
                insertQuery.setObject(1, new java.sql.Timestamp(task.getEnteredDate().getTime()), Types.TIMESTAMP);
                insertQuery.setObject(2, new java.sql.Timestamp(task.getScheduledDate().getTime()), Types.TIMESTAMP);
                insertQuery.setString(3, task.getEnteredNode());

                insertQuery.execute();
                connection.commit();
            }
        });

        final TypedQuery<Task> selectQuery = entityManager.createQuery("FROM Task " +
                "WHERE status = :status " +
                "AND enteredDate = :enteredDate " +
                "AND scheduledDate = :scheduledDate " +
                "AND enteredNode = :enteredNode", Task.class);
        selectQuery.setParameter("status", task.getStatus());
        selectQuery.setParameter("enteredDate", task.getEnteredDate());
        selectQuery.setParameter("scheduledDate", task.getScheduledDate());
        selectQuery.setParameter("enteredNode", task.getEnteredNode());

        final List<Task> tasks = selectQuery.getResultList();
        if(tasks.size() != 1) {
            return null;
        }

        return tasks.get(0);
    }

    @Override
    public void close() {
        if(entityManagerFactory != null) {
            entityManagerFactory.close();
        }
    }

}
