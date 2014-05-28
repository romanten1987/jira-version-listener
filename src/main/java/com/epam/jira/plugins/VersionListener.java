package com.epam.jira.plugins;

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
import com.atlassian.jira.user.ApplicationUser;
import com.atlassian.jira.user.util.UserManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

/**
 * Created by Roman_Ten on 18.03.14.
 */
public class VersionListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(VersionListener.class);
    public static final String GROUP_NAME = "releaseNote-users";
    public static final String GROUP_NOT_FOUND = "Group \"releaseNote-users\" not found. Please create this group.";
    public static final String EMAIL = "SpecialVTBR-TBR2AllGuysAndGals@epam.com";
    private final VersionManager versionManager;
    private final EventPublisher eventPublisher;


    public VersionListener(EventPublisher eventPublisher, VersionManager versionManager) {
        this.eventPublisher = eventPublisher;
        this.versionManager = versionManager;
    }

    public VersionManager getVersionManager() {
        return versionManager;
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

    private void sendNotification(AbstractVersionEvent versionEvent) {
        Report report = new Report(this, versionEvent);
        if (report.isVtb()) {
            ReportNoteEmail email = new ReportNoteEmail();
            email.setSubject(report.getReportSubject());
            email.setBody(report.generateReport());
            email.sendTo(EMAIL);
        }
    }
}
