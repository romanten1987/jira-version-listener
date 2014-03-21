package com.example.tutorial.plugins;

import com.atlassian.crowd.embedded.api.Group;
import com.atlassian.crowd.embedded.api.User;
import com.atlassian.jira.component.ComponentAccessor;
import com.atlassian.jira.mail.Email;
import com.atlassian.jira.security.groups.GroupManager;
import com.atlassian.jira.user.util.UserManager;
import com.atlassian.mail.queue.MailQueueItem;
import com.atlassian.mail.queue.SingleMailQueueItem;

import javax.mail.internet.AddressException;
import javax.mail.internet.InternetAddress;
import java.util.Collection;

/**
 * Created by Roman_Ten on 21.03.14.
 */
public class ReportNoteEmail {
    public static final String MIME_TYPE = "text/html";
    public static final String GROUP_RELEASE_NOTE_USERS = "releaseNote-users";
    private String subject;
    private String to;
    private String body;
    private UserManager userManager = ComponentAccessor.getUserManager();
    private GroupManager groupManager = ComponentAccessor.getGroupManager();


    public void setSubject(String subject) {
        this.subject = subject;
    }

    public void setTo(String to) {
        this.to = to;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public boolean isExistGroup(String groupName) {
        return groupManager.groupExists(groupName);
    }

    public void sendToGroup() {
        setTo(getRecipientAddressesFromGroup());
        sendMail();
    }

    private String getRecipientAddressesFromGroup() {
        StringBuilder emailList = new StringBuilder();
        Group group = userManager.getGroup(GROUP_RELEASE_NOTE_USERS);
        Collection<User> users = groupManager.getUsersInGroup(group);
        for (User user : users) {
            String email = user.getEmailAddress();
            if (isValidEmailAddress(email)) {
                emailList.append(email).append(",");
            }
        }
        return emailList.toString();
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

    public void sendMessageToAdmin(User leadUser) {
        setTo(leadUser.getEmailAddress());
        sendMail();

    }

    private void sendMail() {
        Email email = new Email(to);
        email.setSubject(subject);
        email.setBody(body);
        email.setMimeType(MIME_TYPE);
        MailQueueItem mailQueueItem = new SingleMailQueueItem(email);
        ComponentAccessor.getMailQueue().addItem(mailQueueItem);
    }

}
