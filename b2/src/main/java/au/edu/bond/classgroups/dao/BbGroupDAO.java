package au.edu.bond.classgroups.dao;

import blackboard.data.ValidationException;
import blackboard.data.course.Group;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.persist.course.GroupDbLoader;
import blackboard.persist.course.GroupDbPersister;
import blackboard.platform.course.CourseGroupManager;
import blackboard.platform.course.CourseGroupManagerFactory;

import java.util.Collection;
import java.util.Set;

/**
 * Created by Shane Argo on 10/06/2014.
 */
public class BbGroupDAO {

    private GroupDbLoader groupDbLoader;
    private GroupDbPersister groupDbPersister;
    private CourseGroupManager courseGroupManager;

    public Group getById(Id id) throws PersistenceException {
        return getGroupDbLoader().loadById(id);
    }

    public Group getById(long id) throws PersistenceException {
        return getById(getIdFromLong(id));
    }

    public Collection<Group> getByCourseId(Id id) throws PersistenceException {
        return getGroupDbLoader().loadGroupsAndSetsByCourseId(id);
    }

    public void createOrUpdate(Group group) throws PersistenceException, ValidationException {
        getGroupDbPersister().persist(group);
    }

    public void delete(Id id) {
        getCourseGroupManager().deleteGroup(id);
    }

    public void createOrUpdate(Group group, Set<Id> courseMembershipIds) {
        getCourseGroupManager().persistGroupAndEnroll(group, courseMembershipIds);
    }

    private Id getIdFromLong(long id) {
        return Id.toId(Group.DATA_TYPE, id);
    }

    private GroupDbLoader getGroupDbLoader() throws PersistenceException {
        if(groupDbLoader == null) {
            groupDbLoader = GroupDbLoader.Default.getInstance();
        }
        return groupDbLoader;
    }

    private CourseGroupManager getCourseGroupManager() {
        if(courseGroupManager == null) {
            courseGroupManager = CourseGroupManagerFactory.getInstance();
        }
        return courseGroupManager;
    }

    private GroupDbPersister getGroupDbPersister() throws PersistenceException {
        if (groupDbPersister == null) {
            groupDbPersister = GroupDbPersister.Default.getInstance();
        }

        return groupDbPersister;
    }
}
