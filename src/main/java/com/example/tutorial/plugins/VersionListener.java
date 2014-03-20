package com.example.tutorial.plugins;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.event.project.VersionReleaseEvent;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.ProjectManager;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.template.VelocityTemplatingEngine;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.queue.MailQueueItem;
import com.atlassian.mail.queue.SingleMailQueueItem;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;

import com.atlassian.jira.project.util.ReleaseNoteManager;
import webwork.action.Action;
import webwork.action.factory.ActionFactory;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;

/**
 * Created by Roman_Ten on 18.03.14.
 */
public class VersionListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(VersionListener.class);
    private final VersionManager versionManager;
    private final EventPublisher eventPublisher;
    private final ProjectManager projectManager;
    private final ConstantsManager constantsManager;
    private final ApplicationProperties applicationProperties;
    private final VelocityTemplatingEngine engine;
    private final SearchProvider searchProvider;
    private final CustomFieldManager customFieldManager;


    public VersionListener(EventPublisher eventPublisher, VersionManager versionManager, ProjectManager projectManager, ConstantsManager constantsManager, ApplicationProperties applicationProperties, VelocityTemplatingEngine engine, SearchProvider searchProvider, CustomFieldManager customFieldManager) {
        this.eventPublisher = eventPublisher;
        this.versionManager = versionManager;
        this.projectManager = projectManager;
        this.constantsManager = constantsManager;
        this.applicationProperties = applicationProperties;
        this.engine = engine;
        this.searchProvider = searchProvider;
        this.customFieldManager = customFieldManager;
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    @EventListener
    public void onVersionReleaseEvent(VersionReleaseEvent versionReleaseEvent) {
        long versionId = versionReleaseEvent.getVersionId();
        Version version = versionManager.getVersion(versionId);
        Project project = version.getProjectObject();
        User leadUser = project.getProjectLead().getDirectoryUser();
        String releaseNote = getReleaseNote(project, version, leadUser);
        String versionName = version.getName();
        String projectName = project.getName();
        String emailAddresses = getRecipientAddresses();
        sendMail(emailAddresses, releaseNote);

    }

    private String getRecipientAddresses() {
        UserManager userManager = ComponentAccessor.getUserManager();
        GroupManager groupManager = ComponentAccessor.getGroupManager();
        String result = null;
        if (groupManager.groupExists("releaseNote-users")) {
            StringBuilder emailList = new StringBuilder();
            Group group = userManager.getGroup("releaseNote-users");
            Collection<User> users = groupManager.getUsersInGroup(group);
            for (User user : users) {
                String email = user.getEmailAddress();
                if (isValidEmailAddress(email)) {
                    emailList.append(email).append(",");
                }
            }
            result = emailList.toString();
        } else {
            String email = projectLead.getEmailAddress();
            String errorText = "Group \"releaseNote-users\" not found. Please create this group";
            sendMail(email,errorText);
        }

        return result;

    }

    private String getReleaseNote(Project project, Version version, User user) {
        ReleaseNoteManager releaseNoteManager = new ReleaseNoteManager(applicationProperties, engine, constantsManager, searchProvider, customFieldManager);
        String style = "Email";
        if (!releaseNoteManager.getStyles().containsKey("Email")) {
            style = "Text";
            sendMail(user.getEmailAddress(), "Template 'Email' not found. Please create it. Release note created with 'Text' template now.");
        }
        log.info("Version {} has been released at {}.", version.getName(), version.getReleaseDate());
        Action action = null;
        try {
            ActionFactory actionFactory = ActionFactory.getActionFactory();
            action = actionFactory.getActionImpl("ReleaseNote");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return releaseNoteManager.getReleaseNote(action, style, version, user, project.getGenericValue());
    }



    private void sendMail(String emailList, String body) {
        Email email = new Email(emailList);
        email.setSubject("Version release");
        email.setBody(body);
        email.setMimeType("text/html");
        MailQueueItem mailQueueItem= new SingleMailQueueItem(email);
        ComponentAccessor.getMailQueue().addItem(mailQueueItem);
    }

    private boolean isValidEmailAddress(String email) {
        boolean result = true;
        try {
            InternetAddress emailAddress = new InternetAddress(email);
            emailAddress.validate();
        } catch (AddressException e) {
            result = false;
        }
        return result;
    }
}
