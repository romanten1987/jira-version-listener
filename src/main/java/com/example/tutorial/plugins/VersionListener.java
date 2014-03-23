package com.example.tutorial.plugins;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.config.ConstantsManager;
import com.atlassian.jira.config.properties.ApplicationProperties;
import com.atlassian.jira.event.project.AbstractVersionEvent;
import com.atlassian.jira.event.project.VersionCreateEvent;
import com.atlassian.jira.event.project.VersionReleaseEvent;
import com.atlassian.jira.issue.CustomFieldManager;
import com.atlassian.jira.issue.search.SearchProvider;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.jira.template.VelocityTemplatingEngine;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Created by Roman_Ten on 18.03.14.
 */
public class VersionListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(VersionListener.class);
    private final VersionManager versionManager;
    private final EventPublisher eventPublisher;
    private final ConstantsManager constantsManager;
    private final ApplicationProperties applicationProperties;
    private final VelocityTemplatingEngine engine;
    private final SearchProvider searchProvider;
    private final CustomFieldManager customFieldManager;


    public VersionListener(EventPublisher eventPublisher, VersionManager versionManager, ConstantsManager constantsManager, ApplicationProperties applicationProperties, VelocityTemplatingEngine engine, SearchProvider searchProvider, CustomFieldManager customFieldManager) {
        this.eventPublisher = eventPublisher;
        this.versionManager = versionManager;
        this.constantsManager = constantsManager;
        this.applicationProperties = applicationProperties;
        this.engine = engine;
        this.searchProvider = searchProvider;
        this.customFieldManager = customFieldManager;
    }

    public VersionManager getVersionManager() {
        return versionManager;
    }

    public ConstantsManager getConstantsManager() {
        return constantsManager;
    }

    public ApplicationProperties getApplicationProperties() {
        return applicationProperties;
    }

    public VelocityTemplatingEngine getEngine() {
        return engine;
    }

    public SearchProvider getSearchProvider() {
        return searchProvider;
    }

    public CustomFieldManager getCustomFieldManager() {
        return customFieldManager;
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
        sendNotification(versionReleaseEvent);
    }

    @EventListener
    public void onVersionCreateEvent(VersionCreateEvent versionCreateEvent) {
        sendNotification(versionCreateEvent);
    }

    private void sendNotification(AbstractVersionEvent versionReleaseEvent) {
        ReportNoteEmail email = new ReportNoteEmail();
        Report report = new Report(this, versionReleaseEvent);
        if (email.isExistGroup("releaseNote-users")) {
            email.setSubject("VERSION RELEASED " + report.getReportSubject());
            email.setBody(report.generateReport());
            email.sendToGroup();
        } else {
            email.setBody("Group \"releaseNote-users\" not found. Please create this group.");
            email.sendMessageToAdmin(report.getLeadUser());
        }
    }
}
