package au.edu.bond.classgroups.manager;

import au.edu.bond.classgroups.config.Configuration;
import au.edu.bond.classgroups.groupext.GroupExtension;
import au.edu.bond.classgroups.groupext.GroupExtensionService;
import au.edu.bond.classgroups.logging.TaskLogger;
import au.edu.bond.classgroups.model.Group;
import au.edu.bond.classgroups.model.Member;
import au.edu.bond.classgroups.service.*;
import au.edu.bond.classgroups.util.EqualityUtil;
import blackboard.base.FormattedText;
import blackboard.data.ValidationException;
import blackboard.data.course.AvailableGroupTool;
import blackboard.data.course.Course;
import blackboard.data.course.CourseMembership;
import blackboard.data.course.GroupMembership;
import blackboard.data.user.User;
import blackboard.persist.Id;
import blackboard.persist.PersistenceException;
import blackboard.persist.PkId;
import com.alltheducks.configutils.service.ConfigurationService;
import com.google.common.collect.Lists;
import com.google.common.collect.Sets;
import com.google.common.util.concurrent.UncheckedExecutionException;
import org.apache.commons.lang.Validate;
import org.springframework.beans.factory.annotation.Autowired;

import javax.persistence.NoResultException;
import java.util.*;
import java.util.concurrent.ExecutionException;

/**
 * Created by Shane Argo on 4/06/2014.
 */
public class BbGroupManager implements GroupManager {

    @Autowired
    private BbGroupService bbGroupService;
    @Autowired
    private BbCourseService bbCourseService;
    @Autowired
    private GroupExtensionService groupExtensionService;
    @Autowired
    private BbUserService bbUserService;
    @Autowired
    private BbCourseMembershipService bbCourseMembershipService;
    @Autowired
    private ConfigurationService<Configuration> configurationService;
    @Autowired
    private GroupTitleService groupTitleService;
    @Autowired
    private ResourceService resourceService;

    @Override
    public Status syncGroup(final Group group, final TaskLogger taskLogger) {
        Configuration configuration = configurationService.loadConfiguration();
        Id courseId;
        try {
            Course course = bbCourseService.getByExternalSystemId(group.getCourseId());
            courseId = course.getId();
            if (course.isChild()) {
                courseId = bbCourseService.getParentId(courseId);
                String newTitle = resourceService.getLocalisationString(
                        "bond.classgroups.pattern.childgrouppattern",
                        group.getTitle(), course.getCourseId());
                group.setTitle(newTitle);
            }
        } catch (ExecutionException e) {
            taskLogger.warning(resourceService.getLocalisationString(
                    "bond.classgroups.warning.cantfindcourse", group.getCourseId()));
            return Status.ERROR;
        }

        GroupExtension ext = null;
        try {
            ext = groupExtensionService.getGroupExtensionByExternalId(group.getGroupId());
        } catch (UncheckedExecutionException e) {
            if (!(e.getCause() instanceof NoResultException)) {
                throw e;
            }
        } catch (ExecutionException e) {
            taskLogger.warning(resourceService.getLocalisationString(
                    "bond.classgroups.warning.cantloadextension", group.getGroupId(), group.getCourseId()));
            return Status.ERROR;
        }

        if (ext != null && !ext.isSynced()) {
            taskLogger.info(resourceService.getLocalisationString(
                    "bond.classgroups.info.skippingunsyncedgroup",
                    group.getGroupId()));
            return Status.NOSYNC;
        }

        boolean extNew = false;
        // If extension data is found, attempt to load the Bb group it references.
        blackboard.data.course.Group bbGroup = null;
        if (ext != null) {
            if (ext.getInternalGroupId() == null) {
                taskLogger.info(resourceService.getLocalisationString(
                        "bond.classgroups.info.existinggroupdeleted", group.getGroupId()));
            } else {
                try {
                    bbGroup = bbGroupService.getById(ext.getInternalGroupId(), courseId);
                } catch (ExecutionException e) {
                    taskLogger.info(resourceService.getLocalisationString(
                            "bond.classgroups.info.existinggroupmissing", group.getGroupId()), e);
                }
            }
        } else {
            ext = new GroupExtension();
            ext.setExternalSystemId(group.getGroupId());
            extNew = true;
        }
        boolean extDirty = updateFeedLeader(ext, group, courseId, configuration, taskLogger);

        String title = groupTitleService.getGroupTitle(group.getTitle(), ext, ((PkId) courseId).getKey());
        Status status = Status.UNCHANGED;
        // If no Bb group exists (no extension data, or extension data references a deleted group), create one.
        if (bbGroup == null) {
            bbGroup = new blackboard.data.course.Group();

            bbGroup.setCourseId(courseId);
            bbGroup.setTitle(title);

            FormattedText desc = FormattedText.toFormattedText("");
            bbGroup.setDescription(desc);

            bbGroup.setAllowEditToGroup(false);
            bbGroup.setCustomizable(false);
            bbGroup.setGroupSet(false);
            bbGroup.setSelfEnrolledAllowed(false);

            bbGroup.setIsAvailable(calculateAvailability(group, configuration));

            provisionGroupTools(group, bbGroup);

            updateGroupSet(bbGroup, group, configuration, taskLogger);

            status = Status.CREATED;

            if (configuration.getLoggingLevel().equals(Configuration.LoggingLevel.DEBUG)) {
                taskLogger.info(resourceService.getLocalisationString(
                        "bond.classgroups.info.creatinggroup", group.getGroupId(), group.getTitle()));
            }
        } else {
            if (configuration.getLoggingLevel().equals(Configuration.LoggingLevel.DEBUG)) {
                taskLogger.info(resourceService.getLocalisationString(
                        "bond.classgroups.info.updatinggroup", group.getGroupId(), group.getTitle()));
            }

            if (!title.equals(bbGroup.getTitle())) {
                bbGroup.setTitle(title);
                status = Status.UPDATED;
            }

            if (updateGroupSet(bbGroup, group, configuration, taskLogger)) {
                status = Status.UPDATED;
            }

            if (configuration.getAvailabilityMode() == Configuration.AvailabilityMode.UPDATE) {
                boolean available = calculateAvailability(group, configuration);
                if (bbGroup.getIsAvailable() != available) {
                    bbGroup.setIsAvailable(available);
                    status = Status.UPDATED;
                }
            }

            if (configuration.getToolsMode() == Configuration.ToolsMode.READD || configuration.getToolsMode() == Configuration.ToolsMode.SYNC) {
                provisionGroupTools(group, bbGroup);
                status = Status.UPDATED;
            }
        }

        final List<GroupMembership> groupMemberships = bbGroup.getGroupMemberships();
        HashSet<Id> existingGroupMembers = Sets.newHashSetWithExpectedSize(groupMemberships.size());
        for (GroupMembership groupMembership : groupMemberships) {
            existingGroupMembers.add(groupMembership.getCourseMembershipId());
        }

        final Collection<Member> members = group.getMembers();
        final Set<String> feedMembers = Sets.newHashSetWithExpectedSize(members != null ? members.size() : 1);
        if (members != null) {
            for (Member member : members) {
                feedMembers.add(member.getUserId());
            }
        }

        if (group.getLeaderId() != null && !group.getLeaderId().isEmpty()) {
            feedMembers.add(group.getLeaderId());
        }

        Set<Id> memberIds = new HashSet<>();

        for (String feedMember : feedMembers) {
            final User user;
            try {
                user = bbUserService.getByExternalSystemId(feedMember, courseId);
            } catch (ExecutionException e) {
                taskLogger.warning(resourceService.getLocalisationString("bond.classgroups.warning.couldnotloaduser",
                        feedMember, group.getCourseId(), group.getGroupId()), e);
                continue;
            }

            if (user == null) {
                taskLogger.warning(resourceService.getLocalisationString("bond.classgroups.warning.couldnotloaduser",
                        feedMember, group.getCourseId(), group.getGroupId()));
                continue;
            }

            final CourseMembership membership;
            try {
                membership = bbCourseMembershipService.getByCourseIdAndUserId(courseId, user.getId());
            } catch (ExecutionException e) {
                taskLogger.warning(resourceService.getLocalisationString("bond.classgroups.warning.couldnotloadcousemember",
                        feedMember, group.getCourseId(), group.getGroupId()), e);
                continue;
            }

            if (membership == null) {
                taskLogger.warning(resourceService.getLocalisationString("bond.classgroups.warning.couldnotloadcousemember",
                        feedMember, group.getCourseId(), group.getGroupId()));
                continue;
            }

            final Id membershipId = membership.getId();
            final boolean foundInExisting = existingGroupMembers.remove(membershipId);

            if (!foundInExisting) {
                status = Status.UPDATED;
            }

            memberIds.add(membershipId);
        }

        if (!existingGroupMembers.isEmpty()) {
            status = Status.UPDATED;
        }

        if (status == Status.CREATED || status == Status.UPDATED) {
            // Persist the group.
            if (configuration.getLoggingLevel().equals(Configuration.LoggingLevel.DEBUG)) {
                if (status == Status.CREATED) {
                    taskLogger.info(resourceService.getLocalisationString(
                            "bond.classgroups.info.creatinggroupdebug",
                            group.getGroupId(), group.getCourseId(), bbGroup.getTitle(), memberIds.size()));
                } else if (status == Status.UPDATED) {
                    taskLogger.info(resourceService.getLocalisationString(
                            "bond.classgroups.info.updatinggroupdebug",
                            group.getGroupId(), bbGroup.getId().toExternalString(), group.getCourseId(), courseId.toExternalString(), bbGroup.getTitle(), memberIds.size()));
                }
            }

            try {
                bbGroupService.createOrUpdate(bbGroup, memberIds);
            } catch (ExecutionException e) {
                taskLogger.warning(resourceService.getLocalisationString(
                        "bond.classgroups.warning.groupexecution", group.getGroupId()), e);
                return Status.ERROR;
            }
        }

        Long internalId = ((PkId) bbGroup.getId()).getKey();
        if (!internalId.equals(ext.getInternalGroupId())) {
            ext.setInternalGroupId(internalId);
            extDirty = true;
        }

        if (!group.getTitle().equals(ext.getTitle())) {
            ext.setTitle(group.getTitle());
            extDirty = true;
        }

        if (extNew) {
            try {
                groupExtensionService.create(ext);
            } catch (ExecutionException e) {
                taskLogger.warning(resourceService.getLocalisationString(
                        "bond.classgroups.warning.failedextcreate", group.getGroupId(), courseId), e);
                return Status.ERROR;
            }
        } else if (extDirty) {
            try {
                groupExtensionService.update(ext, ((PkId) courseId).getKey());
            } catch (ExecutionException e) {
                taskLogger.warning(resourceService.getLocalisationString(
                        "bond.classgroups.warning.failedextupdate", group.getGroupId(), courseId), e);
                return Status.ERROR;
            }
        }

        return status;
    }

    private void provisionGroupTools(final Group group, final blackboard.data.course.Group bbGroup) {
        Configuration configuration = configurationService.loadConfiguration();
        final List<AvailableGroupTool> remainingTools = Lists.newArrayList(bbGroup.getAvailableTools(true));
        final Collection<String> tools = (group.getTools() != null) ? group.getTools() : configuration.getDefaultTools();
        boolean withForum = false;

        if (tools != null) {
            toolsLoop:
            for (String tool : tools) {
                if (tool.equalsIgnoreCase("discussion_board")) {
                    withForum = true;
                }

                for (int i = 0; i < remainingTools.size(); i++) {
                    final AvailableGroupTool currentTool = remainingTools.get(i);

                    if (currentTool.getApplicationHandle().equals(tool)) {
                        remainingTools.remove(i);
                        continue toolsLoop;
                    }
                }

                bbGroup.addAvailableTool(tool);
            }
        }

        if (configuration.getToolsMode() == Configuration.ToolsMode.SYNC) {
            for (AvailableGroupTool remainingTool : remainingTools) {
                bbGroup.removeAvailableTool(remainingTool.getApplicationHandle());
            }
        }

        bbGroup.setWithForum(withForum);
    }

    private boolean calculateAvailability(final Group group, final Configuration configuration) {
        return group.getAvailable() != null
            ? group.getAvailable()
            : (configuration.getDefaultAvailability() == Configuration.GroupAvailability.AVAILABLE);
    }

    private boolean updateFeedLeader(final GroupExtension ext, final Group group, final Id courseId, final Configuration configuration, final TaskLogger taskLogger) {
        User user;
        CourseMembership courseMembership = null;

        if (group.getLeaderId() != null) {
            try {
                user = bbUserService.getByExternalSystemId(group.getLeaderId(), courseId);
            } catch (ExecutionException e) {
                taskLogger.warning(resourceService.getLocalisationString(
                        "bond.classgroups.warning.cantfindleader",
                        group.getLeaderId(), group.getGroupId()), e);
                return false;
            }

            if (user != null) {
                try {
                    courseMembership = bbCourseMembershipService.getByCourseIdAndUserId(courseId, user.getId());
                } catch (ExecutionException e) {
                    taskLogger.error(resourceService.getLocalisationString(
                            "bond.classgroups.error.cantfindleadermemberexecution",
                            group.getLeaderId(), group.getGroupId()), e);
                    return false;
                }
            }
        }

        Id existingFeedId = null;
        if (ext.getLeaderFeedCourseUserId() != null) {
            existingFeedId = Id.toId(CourseMembership.DATA_TYPE, ext.getLeaderFeedCourseUserId());
        }
        Id courseMembershipId = null;
        if (courseMembership != null) {
            courseMembershipId = courseMembership.getId();
        }
        boolean changed = !EqualityUtil.nullSafeEquals(existingFeedId, courseMembershipId);

        if (!changed) {
            return false;
        }

        if (courseMembershipId == null) {
            ext.setLeaderFeedCourseUserId(null);
        } else {
            ext.setLeaderFeedCourseUserId(((PkId) courseMembership.getId()).getKey());
        }

        if (configuration.getLeaderChangedMode() == Configuration.LeaderChangedMode.FEED) {
            ext.setLeaderOverridden(false);
        }

        return true;
    }

    private boolean updateGroupSet(final blackboard.data.course.Group bbGroup, final Group group, final Configuration configuration, final TaskLogger taskLogger) {
        if (bbGroup.getSetId() == null && group.getGroupSet() == null) {
            return false;
        }

        if (group.getGroupSet() == null || group.getGroupSet().isEmpty()) {
            bbGroup.setSetId(null);
            return true;
        }

        blackboard.data.course.Group bbGroupSet = getOrCreateGroupSet(group.getGroupSet(), bbGroup.getCourseId(), configuration, taskLogger);
        if (bbGroupSet != null && !EqualityUtil.nullSafeEquals(bbGroupSet.getId(), bbGroup.getSetId())) {
            bbGroup.setSetId(bbGroupSet.getId());
            return true;
        }

        return false;
    }

    private blackboard.data.course.Group getOrCreateGroupSet(final String title, final Id courseId, final Configuration configuration, final TaskLogger taskLogger) {
        if (title == null || title.isEmpty()) {
            return null;
        }

        blackboard.data.course.Group bbGroupSet = null;

        try {
            bbGroupSet = bbGroupService.getByTitleAndCourseId(title, courseId);
        } catch (ExecutionException ignored) {

        }

        if (bbGroupSet == null) {
            bbGroupSet = new blackboard.data.course.Group();
            bbGroupSet.setGroupSet(true);
            bbGroupSet.setTitle(title);
            bbGroupSet.setCourseId(courseId);

            FormattedText desc = FormattedText.toFormattedText("");
            bbGroupSet.setDescription(desc);

            bbGroupSet.setIsAvailable(configuration.getGroupSetAvailability() == Configuration.GroupAvailability.AVAILABLE);
            bbGroupSet.setAllowEditToGroup(false);
            bbGroupSet.setSelfEnrolledAllowed(false);

            try {
                bbGroupService.createOrUpdate(bbGroupSet);
            } catch (ExecutionException | PersistenceException | ValidationException e) {
                taskLogger.error(resourceService.getLocalisationString(
                        "bond.classgroups.error.groupsetexecution", title, courseId), e);

                return null;
            }
        }

        return bbGroupSet;
    }
}
