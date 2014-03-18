package com.example.tutorial.plugins;

import com.atlassian.event.api.EventListener;
import com.atlassian.event.api.EventPublisher;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.project.VersionCreateEvent;
import com.atlassian.jira.event.project.VersionReleaseEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import com.atlassian.mail.queue.SingleMailQueueItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.InitializingBean;

import java.util.Collection;
import java.util.List;

/**
 * Created by Roman_Ten on 18.03.14.
 */
public class VersionListener implements InitializingBean, DisposableBean {
    private static final Logger log = LoggerFactory.getLogger(VersionListener.class);
    private final VersionManager versionManager;
    private final EventPublisher eventPublisher;


    public VersionListener(EventPublisher eventPublisher, VersionManager versionManager) {
        this.eventPublisher = eventPublisher;
        this.versionManager = versionManager;

    }

    @EventListener
    public void onVersionCreateEvent(VersionCreateEvent versionCreateEvent) {
        long versionId = versionCreateEvent.getVersionId();
        Version version = versionManager.getVersion(versionId);
        log.info("Version {} has been created at {}.", version.getName(), version.getStartDate());
        sendMail(String.format("Version %s has been created at %s. \n %s", version.getName(), version.getStartDate(), version.getDescription()));
    }

    @EventListener
    public void onVersionReleaseEvent(VersionReleaseEvent versionReleaseEvent) {
        long versionId = versionReleaseEvent.getVersionId();
        Version version = versionManager.getVersion(versionId);
        Collection<Issue> versionList = versionManager.getIssuesWithFixVersion(version);

        log.info("Version {} has been released at {}.", version.getName(), version.getReleaseDate());
        sendMail(String.format("Version %s has been released at %s.", version.getName(), version.getReleaseDate()));
    }

    @Override
    public void destroy() throws Exception {
        eventPublisher.unregister(this);
    }

    @Override
    public void afterPropertiesSet() throws Exception {
        eventPublisher.register(this);
    }

    private void sendMail(String body) {
        Email email = new Email("dlya.derma@gmail.com");
        email.setSubject("JIRA TEST");
        email.setBody(body);
        email.setMimeType("text/plain");
        SingleMailQueueItem singleMailQueueItem = new SingleMailQueueItem(email);
        ComponentAccessor.getMailQueue().addItem(singleMailQueueItem);
    }
}
