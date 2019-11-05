package au.edu.bond.classgroups.stripes;

import au.edu.bond.classgroups.config.PushCsvFeedDeserialiserConfig;
import au.edu.bond.classgroups.exception.InvalidPasskeyException;
import au.edu.bond.classgroups.exception.InvalidRunIdException;
import au.edu.bond.classgroups.exception.InvalidTaskStateException;
import au.edu.bond.classgroups.logging.TaskLoggerFactory;
import au.edu.bond.classgroups.service.ResourceService;
import au.edu.bond.classgroups.task.TaskExecutor;
import au.edu.bond.classgroups.task.TaskProcessorFactory;
import au.edu.bond.classgroups.logging.TaskLogger;
import au.edu.bond.classgroups.model.Task;
import au.edu.bond.classgroups.service.DirectoryFactory;
import au.edu.bond.classgroups.service.TaskService;
import net.sourceforge.stripes.action.ActionBean;
import net.sourceforge.stripes.action.ActionBeanContext;
import net.sourceforge.stripes.action.Before;
import net.sourceforge.stripes.action.Resolution;
import net.sourceforge.stripes.integration.spring.SpringBean;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by Shane Argo on 3/06/2014.
 */
public class PushCsvAction implements ActionBean {

    private ActionBeanContext context;
    private static Pattern runIdPattern = Pattern.compile("[a-zA-Z0-9]+");

    private DirectoryFactory directoryFactory;
    private TaskService taskService;
    private PushCsvFeedDeserialiserConfig pushCsvFeedDeserialiserConfig;
    private TaskExecutor taskExecutor;
    private ResourceService resourceService;
    private TaskLoggerFactory taskLoggerFactory;

    private TaskProcessorFactory taskProcessorFactory;

    private File pushDir;
    private File groupsFile;
    private File membersFile;

    private String runId;
    private String passkey;

    @Before
    private void verifyPasskey() throws InvalidPasskeyException {
        if (pushCsvFeedDeserialiserConfig == null || pushCsvFeedDeserialiserConfig.getPasskey() == null) {
            throw new InvalidPasskeyException(resourceService.getLocalisationString("bond.classgroups.exception.nopasskeyset"));
        }

        if (passkey == null) {
            throw new InvalidPasskeyException(resourceService.getLocalisationString("bond.classgroups.exception.nopasskeysent"));
        }

        if (!passkey.equals(pushCsvFeedDeserialiserConfig.getPasskey())) {
            throw new InvalidPasskeyException(resourceService.getLocalisationString("bond.classgroups.exception.nopasskeyinvalid"));
        }
    }

    @Before
    private void verifyRunId() throws InvalidRunIdException {
        if(runId == null) {
            throw new InvalidRunIdException(resourceService.getLocalisationString("bond.classgroups.exception.runidrequired"));
        }

        Matcher matcher = runIdPattern.matcher(runId);
        if (!matcher.matches()) {
            throw new InvalidRunIdException(resourceService.getLocalisationString("bond.classgroups.exception.runidalphanumeric"));
        }
    }

    public Resolution pushGroups() throws InvalidPasskeyException {
        try {
            FileUtils.copyInputStreamToFile(context.getRequest().getInputStream(), getGroupsFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Resolution pushMembers() throws InvalidPasskeyException {
        try {
            FileUtils.copyInputStreamToFile(context.getRequest().getInputStream(), getMembersFile());
        } catch (IOException e) {
            e.printStackTrace();
        }

        return null;
    }

    public Resolution execute() throws IOException, InvalidPasskeyException, InvalidTaskStateException {
        Task task = null;
        TaskLogger taskLogger = null;

        try {
            task = taskService.createTask();
            taskLogger = taskLoggerFactory.getLogger(task);

            taskLogger.setTask(task);
            taskLogger.info(resourceService.getLocalisationString("bond.classgroups.info.initiatedpostcsv", runId));

            if (task.getStatus() == Task.Status.SKIPPED) {
                taskLogger.info("bond.classgroups.info.skipping");
                return null;
            }

            FileUtils.moveFile(getGroupsFile(), directoryFactory.getFeedGroupsFile(task));
            FileUtils.moveFile(getMembersFile(), directoryFactory.getFeedMembersFile(task));

            try {
                FileUtils.deleteDirectory(getPushDir());
            } catch (IOException e) {
                taskLogger.info(resourceService.getLocalisationString("bond.classgroups.warning.failedtodeleteincomingdatadirectory"));
            }

            taskService.pendingTask(task);
        } catch (Exception e) {
            if(task != null) {
                if(taskLogger != null) {
                    taskLogger.error("bond.classgroups.error.failedpushcsv", e);
                }
                taskService.failTask(task);
            } else {
                throw e;
            }
        }

        return null;
    }

    private File getPushDir() {
        if (pushDir == null) {
            pushDir = directoryFactory.getHttpPushDir(runId);
        }
        return pushDir;
    }

    private File getGroupsFile() {
        if (groupsFile == null) {
            groupsFile = new File(getPushDir(), "groups");
        }
        return groupsFile;
    }

    private File getMembersFile() {
        if (membersFile == null) {
            membersFile = new File(getPushDir(), "members");
        }
        return membersFile;
    }

    @Override
    public void setContext(ActionBeanContext context) {
        this.context = context;
    }

    @Override
    public ActionBeanContext getContext() {
        return context;
    }

    public TaskProcessorFactory getTaskProcessorFactory() {
        return taskProcessorFactory;
    }

    @SpringBean
    public void setTaskProcessorFactory(TaskProcessorFactory taskProcessorFactory) {
        this.taskProcessorFactory = taskProcessorFactory;
    }

    public DirectoryFactory getDirectoryFactory() {
        return directoryFactory;
    }

    @SpringBean
    public void setDirectoryFactory(DirectoryFactory directoryFactory) {
        this.directoryFactory = directoryFactory;
    }

    public TaskService getTaskService() {
        return taskService;
    }

    @SpringBean
    public void setTaskService(TaskService taskService) {
        this.taskService = taskService;
    }

    public PushCsvFeedDeserialiserConfig getPushCsvFeedDeserialiserConfig() {
        return pushCsvFeedDeserialiserConfig;
    }

    public TaskExecutor getTaskExecutor() {
        return taskExecutor;
    }

    @SpringBean
    public void setTaskExecutor(TaskExecutor taskExecutor) {
        this.taskExecutor = taskExecutor;
    }

    @SpringBean
    public void setPushCsvFeedDeserialiserConfig(PushCsvFeedDeserialiserConfig pushCsvFeedDeserialiserConfig) {
        this.pushCsvFeedDeserialiserConfig = pushCsvFeedDeserialiserConfig;
    }

    public ResourceService getResourceService() {
        return resourceService;
    }

    @SpringBean
    public void setResourceService(ResourceService resourceService) {
        this.resourceService = resourceService;
    }

    public TaskLoggerFactory getTaskLoggerFactory() {
        return taskLoggerFactory;
    }

    @SpringBean
    public void setTaskLoggerFactory(TaskLoggerFactory taskLoggerFactory) {
        this.taskLoggerFactory = taskLoggerFactory;
    }

    public String getRunId() {
        return runId;
    }

    public void setRunId(String runId) {
        this.runId = runId;
    }

    public String getPasskey() {
        return passkey;
    }

    public void setPasskey(String passkey) {
        this.passkey = passkey;
    }

}
