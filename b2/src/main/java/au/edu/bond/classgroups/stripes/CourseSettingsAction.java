package au.edu.bond.classgroups.stripes;

import au.edu.bond.classgroups.dao.BbCourseMembershipDAO;
import au.edu.bond.classgroups.dao.BbGroupDAO;
import au.edu.bond.classgroups.dao.BbGroupMembershipDAO;
import au.edu.bond.classgroups.dao.BbUserDAO;
import au.edu.bond.classgroups.groupext.GroupExtension;
import au.edu.bond.classgroups.groupext.GroupExtensionDAO;
import au.edu.bond.classgroups.service.GroupTitleService;
import au.edu.bond.classgroups.util.EqualityUtil;
import blackboard.data.ValidationException;
import blackboard.data.course.Course;
import blackboard.data.course.CourseMembership;
import blackboard.data.course.Group;
import blackboard.data.course.GroupMembership;
import blackboard.data.user.User;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.persist.PkId;
import com.alltheducks.bb.stripes.EntitlementRestrictions;
import net.sourceforge.stripes.action.*;
import net.sourceforge.stripes.controller.LifecycleStage;
import net.sourceforge.stripes.integration.spring.SpringBean;

import java.util.*;

/**
 * Created by Shane Argo on 3/06/2014.
 */
@EntitlementRestrictions(entitlements={"bond.classgroups.coursesettings.MODIFY"}, errorPage="/noaccess.jsp")
public class CourseSettingsAction implements ActionBean {

    private ActionBeanContext context;

    private String courseId;
    private String group;

    private BbGroupDAO bbGroupDAO;
    private GroupExtensionDAO groupExtensionDAO;
    private BbUserDAO bbUserDAO;
    private BbCourseMembershipDAO bbCourseMembershipDAO;
    private BbGroupMembershipDAO bbGroupMembershipDAO;
    private GroupTitleService groupTitleService;

    private List<GroupData> groupDataList;

    private GroupExtension groupExtension;
    private Long existingLeaderCourseUserId;
    private User overriddenLeader;
    private String newOverriddenLeader;
    private User feedLeader;

    @DefaultHandler
    public Resolution groupList() {
        return new ForwardResolution("/WEB-INF/jsp/coursesettings.jsp");
    }

    public Resolution editGroup() throws PersistenceException {
        if(groupExtension.getLeaderOverrideCourseUserId() != null) {
            final CourseMembership bbCourseMembership = bbCourseMembershipDAO.getById(groupExtension.getLeaderOverrideCourseUserId());
            if(bbCourseMembership != null) {
                overriddenLeader = bbUserDAO.getById(bbCourseMembership.getUserId());
            }
        }

        if(groupExtension.getLeaderFeedCourseUserId() != null) {
            final CourseMembership bbCourseMembership = bbCourseMembershipDAO.getById(groupExtension.getLeaderFeedCourseUserId());
            if(bbCourseMembership != null) {
                feedLeader = bbUserDAO.getById(bbCourseMembership.getUserId());
            }
        }

        return new ForwardResolution("/WEB-INF/jsp/editgroup.jsp");
    }

    public Resolution saveGroup() throws PersistenceException, ValidationException {
        Id bbCourseId = Id.toId(Course.DATA_TYPE, courseId);
        final Group bbGroup = bbGroupDAO.getById(groupExtension.getInternalGroupId());

        if(newOverriddenLeader != null) {
            Id bbUserId = Id.toId(User.DATA_TYPE, newOverriddenLeader);
            final CourseMembership newBbCourseMembership = bbCourseMembershipDAO.getByCourseIdAndUserId(bbCourseId, bbUserId);

            if(newBbCourseMembership != null) {
                groupExtension.setLeaderOverrideCourseUserId(((PkId) newBbCourseMembership.getId()).getKey());
            }

        }

        Long newLeaderCourseUserId = groupExtension.calculateLeaderCourseUserId();
        if(!EqualityUtil.nullSafeEquals(newLeaderCourseUserId, existingLeaderCourseUserId)) {
            if(existingLeaderCourseUserId != null) {
                Id existingBbCourseMemId = Id.toId(CourseMembership.DATA_TYPE, existingLeaderCourseUserId);
                final CourseMembership existingLeaderCourseMem = bbCourseMembershipDAO.getById(existingBbCourseMemId);
                final GroupMembership existingBbGroupMem = bbGroupMembershipDAO.getByGroupIdAndUserId(bbGroup.getId(), existingLeaderCourseMem.getUserId());
                bbGroupMembershipDAO.delete(existingBbGroupMem.getId());
            }

            if(newLeaderCourseUserId != null) {
                Id newBbCourseUserId = Id.toId(CourseMembership.DATA_TYPE, newLeaderCourseUserId);
                final CourseMembership newBbCourseMembership = bbCourseMembershipDAO.getById(newBbCourseUserId);
                final User user = bbUserDAO.getById(newBbCourseMembership.getUserId());

                String title = groupTitleService.getGroupTitle(groupExtension.getTitle(), groupExtension, ((PkId)bbCourseId).getKey());
                bbGroup.setTitle(title);

                GroupMembership newGroupMembership = new GroupMembership();
                newGroupMembership.setGroupId(bbGroup.getId());
                newGroupMembership.setCourseMembershipId(newBbCourseMembership.getId());
                bbGroupMembershipDAO.createOrUpdate(newGroupMembership);
            } else {
                bbGroup.setTitle(groupExtension.getTitle());
            }
            try {
                bbGroupDAO.createOrUpdate(bbGroup);
            } catch (Exception e) {

            }
        }

        groupExtensionDAO.update(groupExtension);

        return new RedirectResolution(String.format("/CourseSettings.action?course_id=%s", courseId));
    }

    public int getGroupDataListCount() throws PersistenceException {
        return getGroupDataList().size();
    }

    public List<GroupData> getGroupDataList() throws PersistenceException {
        if(groupDataList == null) {
            Id id = PkId.toId(Course.DATA_TYPE, courseId);
            final Collection<Group> bbGroups = bbGroupDAO.getByCourseId(id);

            Map<Long, Group> bbGroupLookup = new HashMap<Long, Group>();

            for(Group bbGroup : bbGroups) {
                long bbGroupId = ((PkId)bbGroup.getId()).getKey();
                bbGroupLookup.put(bbGroupId, bbGroup);
            }

            groupDataList = new ArrayList<GroupData>();

            if(bbGroups.size() > 0) {
                final Collection<GroupExtension> groupExtensions = groupExtensionDAO.getByGroupIds(bbGroupLookup.keySet());

                for (GroupExtension ext : groupExtensions) {
                    GroupData data = new GroupData();
                    data.setGroupExtension(ext);
                    final Group bbGroup = bbGroupLookup.get(ext.getInternalGroupId());
                    data.setBbGroup(bbGroup);
                    if (bbGroup.getSetId() != null) {
                        data.setBbGroupSet(bbGroupLookup.get(((PkId) bbGroup.getSetId()).getKey()));
                    }
                    Long leaderCourseUserId = null;
                    if (ext.isLeaderOverridden()) {
                        leaderCourseUserId = ext.getLeaderOverrideCourseUserId();
                    } else {
                        leaderCourseUserId = ext.getLeaderFeedCourseUserId();
                    }
                    if (leaderCourseUserId != null) {
                        final CourseMembership bbCourseMembership = bbCourseMembershipDAO.getById(leaderCourseUserId);
                        if (bbCourseMembership != null) {
                            final User leader = bbUserDAO.getById(bbCourseMembership.getUserId());
                            data.setLeader(leader);
                        }
                    }

                    groupDataList.add(data);
                }
            }
        }
        return groupDataList;
    }

    @Before(stages = {LifecycleStage.BindingAndValidation})
    public void loadGroup() {
        String groupExternalSystemId = context.getRequest().getParameter("group");
        if(groupExternalSystemId != null && !groupExternalSystemId.isEmpty()) {
            groupExtension = groupExtensionDAO.getByExternalSystemId(groupExternalSystemId);

            if(groupExtension != null) {
                existingLeaderCourseUserId = groupExtension.calculateLeaderCourseUserId();
            }
        }
    }

    @Override
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    @Override
    public ActionBeanContext getContext() {
        return context;
    }

    public String getCourseId() {
        return courseId;
    }

    public void setCourseId(String courseId) {
        this.courseId = courseId;
    }

    public void setCourse_id(String courseId) {
        this.courseId = courseId;
    }

    public BbGroupDAO getBbGroupDAO() {
        return bbGroupDAO;
    }

    @SpringBean
    public void setBbGroupDAO(BbGroupDAO bbGroupDAO) {
        this.bbGroupDAO = bbGroupDAO;
    }

    public GroupExtensionDAO getGroupExtensionDAO() {
        return groupExtensionDAO;
    }

    @SpringBean
    public void setGroupExtensionDAO(GroupExtensionDAO groupExtensionDAO) {
        this.groupExtensionDAO = groupExtensionDAO;
    }

    public BbUserDAO getBbUserDAO() {
        return bbUserDAO;
    }

    @SpringBean
    public void setBbUserDAO(BbUserDAO bbUserDAO) {
        this.bbUserDAO = bbUserDAO;
    }

    public BbCourseMembershipDAO getBbCourseMembershipDAO() {
        return bbCourseMembershipDAO;
    }

    @SpringBean
    public void setBbCourseMembershipDAO(BbCourseMembershipDAO bbCourseMembershipDAO) {
        this.bbCourseMembershipDAO = bbCourseMembershipDAO;
    }

    public BbGroupMembershipDAO getBbGroupMembershipDAO() {
        return bbGroupMembershipDAO;
    }

    @SpringBean
    public void setBbGroupMembershipDAO(BbGroupMembershipDAO bbGroupMembershipDAO) {
        this.bbGroupMembershipDAO = bbGroupMembershipDAO;
    }

    public GroupTitleService getGroupTitleService() {
        return groupTitleService;
    }

    @SpringBean
    public void setGroupTitleService(GroupTitleService groupTitleService) {
        this.groupTitleService = groupTitleService;
    }

    public GroupExtension getGroupExtension() {
        return groupExtension;
    }

    public void setGroupExtension(GroupExtension groupExtension) {
        this.groupExtension = groupExtension;
    }

    public String getGroup() {
        return group;
    }

    public void setGroup(String group) {
        this.group = group;
    }

    public User getOverriddenLeader() {
        return overriddenLeader;
    }

    public void setOverriddenLeader(User overriddenLeader) {
        this.overriddenLeader = overriddenLeader;
    }

    public User getFeedLeader() {
        return feedLeader;
    }

    public void setFeedLeader(User feedLeader) {
        this.feedLeader = feedLeader;
    }

    public String getNewOverriddenLeader() {
        return newOverriddenLeader;
    }

    public void setNewOverriddenLeader(String newOverriddenLeader) {
        this.newOverriddenLeader = newOverriddenLeader;
    }

    public class GroupData {

        private Group bbGroup;
        private Group bbGroupSet;
        private GroupExtension groupExtension;
        private User leader;

        public Group getBbGroup() {
            return bbGroup;
        }

        public void setBbGroup(Group bbGroup) {
            this.bbGroup = bbGroup;
        }

        public Group getBbGroupSet() {
            return bbGroupSet;
        }

        public void setBbGroupSet(Group bbGroupSet) {
            this.bbGroupSet = bbGroupSet;
        }

        public GroupExtension getGroupExtension() {
            return groupExtension;
        }

        public void setGroupExtension(GroupExtension groupExtension) {
            this.groupExtension = groupExtension;
        }

        public User getLeader() {
            return leader;
        }

        public void setLeader(User leader) {
            this.leader = leader;
        }
    }

    public Comparator<GroupData> getTitleComparator() {
        return new TitleComparator();
    }

    public class TitleComparator implements Comparator<GroupData> {
        @Override
        public int compare(GroupData o1, GroupData o2) {
            return o1.getBbGroup().getTitle().compareTo(o2.getBbGroup().getTitle());
        }
    }

    public Comparator<GroupData> getSetComparator() {
        return new SetComparator();
    }

    public class SetComparator implements Comparator<GroupData> {
        @Override
        public int compare(GroupData o1, GroupData o2) {
            return o1.getBbGroupSet() == null ? 1 :
                    o2.getBbGroupSet() == null ? -1 :
                    o1.getBbGroupSet().getTitle().compareTo(o2.getBbGroupSet().getTitle());
        }
    }

    public Comparator<GroupData> getAvailableComparator() {
        return new AvailableComparator();
    }

    public class AvailableComparator implements Comparator<GroupData> {
        @Override
        public int compare(GroupData o1, GroupData o2) {
            return o1.getBbGroup().getIsAvailable() == o2.getBbGroup().getIsAvailable() ? 0 : o1.getBbGroup().getIsAvailable() ? 1 : -1;
        }
    }

    public Comparator<GroupData> getSyncComparator() {
        return new SyncComparator();
    }

    public class SyncComparator implements Comparator<GroupData> {
        @Override
        public int compare(GroupData o1, GroupData o2) {
            return o1.getGroupExtension().isSynced() == o2.getGroupExtension().isSynced() ? 0 : o1.getGroupExtension().isSynced() ? 1 : -1;
        }
    }

    public Comparator<GroupData> getLeaderOverrideComparator() {
        return new LeaderOverrideComparator();
    }

    public class LeaderOverrideComparator implements Comparator<GroupData> {
        @Override
        public int compare(GroupData o1, GroupData o2) {
            return o1.getGroupExtension().isLeaderOverridden() == o2.getGroupExtension().isLeaderOverridden() ? 0 : o1.getGroupExtension().isLeaderOverridden() ? 1 : -1;
        }
    }

    public Comparator<GroupData> getLeaderComparator() {
        return new LeaderComparator();
    }

    public class LeaderComparator implements Comparator<GroupData> {
        @Override
        public int compare(GroupData o1, GroupData o2) {
            return o1.getLeader() == null ? 1 :
                    o2.getLeader() == null ? -1 :
                    String.format("%s %s", o1.getLeader().getGivenName(), o1.getLeader().getFamilyName())
                            .compareTo(String.format("%s %s", o2.getLeader().getGivenName(), o2.getLeader().getFamilyName()));
        }
    }


}
