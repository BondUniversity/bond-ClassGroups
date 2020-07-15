package au.edu.bond.classgroups.manager;

import au.edu.bond.classgroups.groupext.GroupExtension;
import au.edu.bond.classgroups.groupext.GroupExtensionService;
import au.edu.bond.classgroups.logging.TaskLogger;
import au.edu.bond.classgroups.model.Group;
import au.edu.bond.classgroups.service.*;
import blackboard.persist.Id;
import blackboard.persist.KeyNotFoundException;
import blackboard.persist.PersistenceException;
import blackboard.persist.PkId;
import blackboard.platform.gradebook2.GradebookCustomView;
import blackboard.platform.security.authentication.BbSecurityException;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/**
 * Created by Shane Argo on 19/06/2014.
 */
public class BbSmartViewManager implements SmartViewManager {

    public static final String CUSTOM_VIEW_JSON_PATTERN = "\"searchType\":\"grpmem\",\"formula\":\"1\", \"criteria\": [ {\"fid\":\"1\",\"cid\":\"GM\",\"ctype\":\"GM\",\"cond\":\"eq\",\"value\":\"%s\"}], \"display\":{ \"items\":\"allItem\",\"showhidden\":false}";
    public static final int CUSTOM_VIEW_TITLE_MAX_LENGTH = 64;

    @Autowired
    private GroupExtensionService groupExtensionService;
    @Autowired
    private BbGroupService bbGroupService;
    @Autowired
    private BbGradebookCustomViewService bbGradebookCustomViewService;
    @Autowired
    private BbCourseService bbCourseService;
    @Autowired
    private GroupTitleService groupTitleService;
    @Autowired
    private ResourceService resourceService;

    @Override
    public void syncSmartView(Group group, TaskLogger taskLogger) {
        Id courseId;
        try {
            courseId = bbCourseService.getByExternalSystemId(group.getCourseId()).getId();
        } catch (ExecutionException e) {
            taskLogger.warning(resourceService.getLocalisationString(
                    "bond.classgroups.warning.cantfindcourse",
                    group.getCourseId()));
            return;
        }

        GroupExtension ext = null;
        try {
            ext = groupExtensionService.getGroupExtensionByExternalId(group.getGroupId());
        } catch (ExecutionException e) {
            taskLogger.warning(resourceService.getLocalisationString(
                    "bond.classgroups.warning.cantloadextension", group.getCourseId()));
            return;
        }



        Id groupId = bbGroupService.getIdFromLong(ext.getInternalGroupId());

        GradebookCustomView gradebookCustomView = null;
        if(ext.getCustomViewId() != null) {
            Id customViewId = bbGradebookCustomViewService.getIdFromLong(ext.getCustomViewId());
            try {
                gradebookCustomView = bbGradebookCustomViewService.getById(customViewId, courseId);
            } catch (ExecutionException e) {
                taskLogger.warning(resourceService.getLocalisationString(
                        "bond.classgroups.warning.couldnotfindsmartview",
                        group.getGroupId()));
                return;
            }
        }

        boolean dirty = false;
        if(gradebookCustomView == null) {
//            taskLogger.info(resourceService.getLocalisationString(
//                    "bond.classgroups.info.creatingsmartview", group.getGroupId(), group.getTitle()));
            gradebookCustomView = new GradebookCustomView();

            String alias = String.format("gr_%s", ext.getInternalGroupId());
            Map<String, Id> aliasMap = new HashMap<String, Id>(1);
            aliasMap.put(alias, groupId);

            gradebookCustomView.setViewType(GradebookCustomView.SmartViewType.CUSTOM);
            gradebookCustomView.setCourseId(courseId);
            gradebookCustomView.setHasUserIds(false);
            gradebookCustomView.setJsonText(String.format(CUSTOM_VIEW_JSON_PATTERN, alias));
            gradebookCustomView.setAliases(aliasMap);

            dirty = true;
        }

        String title = resourceService.getLocalisationString("bond.classgroups.pattern.smartview",
                groupTitleService.getGroupTitle(group.getTitle(), ext, ((PkId)courseId).getKey()));
        if(title.length() > CUSTOM_VIEW_TITLE_MAX_LENGTH) {
            title = title.substring(0, CUSTOM_VIEW_TITLE_MAX_LENGTH);
        }
        if(!title.equals(gradebookCustomView.getTitle())) {
            gradebookCustomView.setTitle(title);
            dirty = true;
        }

        if(!dirty) {
            return;
        }

        try {
            bbGradebookCustomViewService.createOrUpdate(gradebookCustomView);
        } catch (BbSecurityException e) {
            taskLogger.error(resourceService.getLocalisationString(
                    "bond.classgroups.error.failedtocreatesmartview", group.getGroupId()), e);
            return;
        } catch (ExecutionException e) {
            taskLogger.error(resourceService.getLocalisationString(
                    "bond.classgroups.error.failedtocreatesmartviewexecution", group.getGroupId()), e);
            return;
        }

        long gradebookCustomViewId = ((PkId)gradebookCustomView.getId()).getKey();
        ext.setCustomViewId(gradebookCustomViewId);
        try {
            groupExtensionService.update(ext, ((PkId) courseId).getKey());
        } catch (RuntimeException e) {
            taskLogger.warning(resourceService.getLocalisationString(
                    "bond.classgroups.warning.failedextupdate", group.getGroupId(), courseId), e);
            return;
        }

    }

    public GroupExtensionService getGroupExtensionService() {
        return groupExtensionService;
    }

    public void setGroupExtensionService(GroupExtensionService groupExtensionService) {
        this.groupExtensionService = groupExtensionService;
    }

    public BbGroupService getBbGroupService() {
        return bbGroupService;
    }

    public void setBbGroupService(BbGroupService bbGroupService) {
        this.bbGroupService = bbGroupService;
    }

    public BbGradebookCustomViewService getBbGradebookCustomViewService() {
        return bbGradebookCustomViewService;
    }

    public void setBbGradebookCustomViewService(BbGradebookCustomViewService bbGradebookCustomViewService) {
        this.bbGradebookCustomViewService = bbGradebookCustomViewService;
    }

    public BbCourseService getBbCourseService() {
        return bbCourseService;
    }

    public void setBbCourseService(BbCourseService bbCourseService) {
        this.bbCourseService = bbCourseService;
    }

    public GroupTitleService getGroupTitleService() {
        return groupTitleService;
    }

    public void setGroupTitleService(GroupTitleService groupTitleService) {
        this.groupTitleService = groupTitleService;
    }

    public ResourceService getResourceService() {
        return resourceService;
    }

    public void setResourceService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }
}
