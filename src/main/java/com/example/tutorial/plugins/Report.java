package com.example.tutorial.plugins;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.event.project.AbstractVersionEvent;
import com.atlassian.jira.event.project.VersionCreateEvent;
import com.atlassian.jira.event.project.VersionReleaseEvent;
import com.atlassian.jira.issue.Issue;
import com.atlassian.jira.issue.issuetype.IssueType;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.version.Version;
import com.atlassian.jira.project.version.VersionManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Collection;

/**
 * Created by Roman_Ten on 21.03.14.
 */
public class Report {
    private static final Logger log = LoggerFactory.getLogger(VersionListener.class);
    public static final String VERSION_RELEASED = "VERSION RELEASED ";
    public static final String SUBJECT_TEMPLATE = "%s %s in %s";
    private static final String VERSION_CREATED = "VERSION CREATED";
    public static final String PROJECT_NAME = "VTBR-TBR2";
    private final VersionListener versionListener;
    private final AbstractVersionEvent versionEvent;
    private final Version version;
    private final Project project;
    private final VersionManager versionManager;

    public Report(VersionListener versionListener, AbstractVersionEvent versionEvent) {
        this.versionListener = versionListener;
        this.versionEvent = versionEvent;
        this.version = getVersion();
        this.project = version.getProjectObject();
        this.versionManager = versionListener.getVersionManager();
    }


    public String getReportSubject() {
        String versionName = version.getName();
        String projectName = project.getName();
        String event = "";
        if (versionEvent instanceof VersionReleaseEvent) {
            event = VERSION_RELEASED;
        } else if (versionEvent instanceof VersionCreateEvent) {
            event = VERSION_CREATED;
        }
        return String.format(SUBJECT_TEMPLATE, event, versionName, projectName);
    }

    private Version getVersion() {
        long versionId = versionEvent.getVersionId();
        return versionListener.getVersionManager().getVersion(versionId);
    }

    public String generateReport() {
        StringBuilder report = new StringBuilder();
        String jiraURL = ComponentAccessor.getApplicationProperties().getString("jira.baseurl");
        Collection<Issue> issues = versionManager.getIssuesWithFixVersion(version);
        report.append(String.format("<h1><a href=\"%s/browse/%s/fixforversion/%d\">%s</a></h1><p>%s</p>", jiraURL, project.getKey(), version.getId(), version.getName(), version.getDescription()));
        for (IssueType issueType : project.getIssueTypes()) {
            StringBuilder text = new StringBuilder();
            for (Issue issue : issues) {
                if (issue.getIssueTypeId().equals(issueType.getId())) {
                    text.append("<li><a href=\"").append(jiraURL).append("/browse/").append(issue.getKey()).append("\">").append(issue.getKey()).append("</a> - ").append(issue.getSummary()).append("</li>");
                }

            }
            if (!text.toString().equals("")) {
                report.append("<h2>").append(issueType.getName()).append("</h2><ul>").append(text.toString()).append("</ul>");
            }
        }
        report.insert(0, String.format("<html><title>Release Notes - %s - %s</title><body>", project.getName(), version.getName())).append("</body></html>");

        return report.toString();
    }

    public User getLeadUser() {
        return project.getProjectLead().getDirectoryUser();
    }

    public boolean isVtb() {
        return project.getName().equals(PROJECT_NAME);
    }
}
