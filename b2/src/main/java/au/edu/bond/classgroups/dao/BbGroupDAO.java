package au.edu.bond.classgroups.dao;

import blackboard.data.ValidationException;
import blackboard.data.course.Group;
import blackboard.data.course.GroupMembership;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.persist.course.GroupDbLoader;
import blackboard.persist.course.GroupDbPersister;
import blackboard.persist.course.GroupMembershipDbPersister;

import java.util.Collection;
import java.util.List;
import java.util.Set;

/**
 * Created by Shane Argo on 10/06/2014.
 */
public class BbGroupDAO {

    private GroupDbLoader groupDbLoader;
    private GroupDbPersister groupDbPersister;
    private GroupMembershipDbPersister groupMembershipDbPersister;

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

    public void delete(Id id) throws PersistenceException {
        getGroupDbPersister().deleteById(id);
    }

    public void createOrUpdate(Group group, Set<Id> courseMembershipIds) throws PersistenceException, ValidationException {
        getGroupDbPersister().persist(group);

        // persist enrolments
        Id groupId = group.getId();

        List<GroupMembership> currentGroupMembers = group.getGroupMemberships();

        // delete any current members that are no longer in the group
        for (GroupMembership currentGroupMember : currentGroupMembers) {
            if (!courseMembershipIds.contains(currentGroupMember.getCourseMembershipId())) {
                getGroupMembershipDbPersister().deleteById(currentGroupMember.getCourseMembershipId());
            }
        }

        // update or insert members
        for (Id courseMembershipId : courseMembershipIds) {
            GroupMembership groupMembership = new GroupMembership();
            groupMembership.setGroupId(groupId);
            groupMembership.setCourseMembershipId(courseMembershipId);
            getGroupMembershipDbPersister().persist(groupMembership);
        }
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

    private GroupDbPersister getGroupDbPersister() throws PersistenceException {
        if (groupDbPersister == null) {
            groupDbPersister = GroupDbPersister.Default.getInstance();
        }

        return groupDbPersister;
    }

    private GroupMembershipDbPersister getGroupMembershipDbPersister() throws PersistenceException {
        if (groupMembershipDbPersister == null) {
            groupMembershipDbPersister = GroupMembershipDbPersister.Default.getInstance();
        }

        return groupMembershipDbPersister;
    }
}
