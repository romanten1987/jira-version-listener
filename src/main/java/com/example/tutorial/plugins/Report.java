package com.example.tutorial.plugins;

import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.event.project.VersionReleaseEvent;
import com.atlassian.jira.project.Project;
import com.atlassian.jira.project.util.ReleaseNoteManager;
import com.atlassian.jira.project.version.Version;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import webwork.action.Action;
import webwork.action.factory.ActionFactory;

/**
 * Created by Roman_Ten on 21.03.14.
 */
public class Report {
    private static final Logger log = LoggerFactory.getLogger(VersionListener.class);
    public static final String VERSION_RELEASED = "VERSION RELEASED ";
    public static final String RELEASE_NOTE = "ReleaseNote";
    public static final String SUBJECT_TEMPLATE = "%s %s in %s";
    public static final String EMAIL_STYLE = "Email";
    public static final String TEXT_STYLE = "Text";
    private final VersionListener versionListener;
    private final VersionReleaseEvent versionReleaseEvent;
    private final Version version;
    private final Project project;

    public Report(VersionListener versionListener, VersionReleaseEvent versionReleaseEvent) {
        this.versionListener = versionListener;
        this.versionReleaseEvent = versionReleaseEvent;
        this.version = getVersion();
        this.project = version.getProjectObject();
    }


    public String getReportSubject() {
        String versionName = version.getName();
        String projectName = project.getName();
        return String.format(SUBJECT_TEMPLATE, VERSION_RELEASED, versionName, projectName);
    }

    private Version getVersion() {
        long versionId = versionReleaseEvent.getVersionId();
        return versionListener.getVersionManager().getVersion(versionId);
    }

    public String generateReport() {
        ReleaseNoteManager releaseNoteManager = new ReleaseNoteManager(versionListener.getApplicationProperties(), versionListener.getEngine(), versionListener.getConstantsManager(), versionListener.getSearchProvider(), versionListener.getCustomFieldManager());
        String style = EMAIL_STYLE;
        if (!releaseNoteManager.getStyles().containsKey(EMAIL_STYLE)) {
            style = TEXT_STYLE;
        }
        Action action = null;
        try {
            ActionFactory actionFactory = ActionFactory.getActionFactory();
            action = actionFactory.getActionImpl(RELEASE_NOTE);
        } catch (Exception e) {
            log.error(e.getMessage());
        }

        return releaseNoteManager.getReleaseNote(action, style, version, getLeadUser(), project.getGenericValue());
    }

    public User getLeadUser() {
        return project.getProjectLead().getDirectoryUser();
    }
}
