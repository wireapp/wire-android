package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidDriverBuilder;
import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.NotificationsPage;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class NotificationsPageSteps {

    private final AndroidTestContext context;

    public NotificationsPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private NotificationsPage getNotificationsPage() {
        return context.getPage(NotificationsPage.class);
    }

    private static final Logger log = ZetaLogger.getLog(AndroidDriverBuilder.class.getSimpleName());

    @When("^I open the notification center$")
    public void iOpenNotificationsCenter() {
        getNotificationsPage().openNotificationCenter();
    }

    @When("^I close the notification center$")
    public void iCloseNotificationsCenter() {
        getNotificationsPage().closeNotificationCenter();
    }

    @When("^I wait until the notification popup disappears$")
    public void waitUntilNotificationPopUpIsGone() {
        getNotificationsPage().waitUntilNotificationPopUpIsInvisible();
    }

    @When("^I see the message \"(.*)\" from 1:1 conversation from user (.*) in the notification center$")
    public void iSeeNotificationFromUser1On1(String message, String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Notification from user '%s' is not visible in notification center.", user), getNotificationsPage().isNotificationFromUserIn1On1Visible(user));
        assertThat(String.format("Notification '%s' is not visible in notification center.", message), getNotificationsPage().isNotificationMessageIn1On1Visible(user));
    }

    @When("^I do not see the message \"(.*)\" from 1:1 conversation from user (.*) in the notification center$")
    public void iDoNotSeeNotificationFromUser1On1(String message, String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Notification from user '%s' is visible in notification center.", user), getNotificationsPage().isNotificationFromUserIn1On1Invisible(user));
        assertThat(String.format("Notification '%s' is visible in notification center.", user), getNotificationsPage().isNotificationMessageIn1On1Invisible(message));

    }

    @When("^I see the message \"(.*)\" from user (.*) in group (.*) in the notification center$")
    public void iSeeMessageNotificationFromUserFromGroup(String message, String user, String group) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Message from user is not '%s'.", message) , getNotificationsPage().isGroupNotificationVisible(message));
        assertThat("SenderName is not visible in group notification.", getNotificationsPage().isSenderNameInGroupMessageVisible(user));
        assertThat("GroupName is not visible in notification center.", getNotificationsPage().isGroupNameInNotificationVisible(group));
    }

    @When("^I do not see the message \"(.*)\" from user (.*) in group (.*) in the notification center$")
    public void iDoNotSeeMessageNotificationFromUserFromGroup(String message, String user, String group) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Message from user is visible, but should not be.", getNotificationsPage().isGroupNotificationInvisible(message));
        assertThat("GroupName is visible in notification center.", getNotificationsPage().isGroupNameInNotificationInvisible(group));
        assertThat("SenderName is visible in group notification.", getNotificationsPage().isSenderNameInGroupMessageInvisible(user));
    }

    @When("^I see the mention \"(.*)\" from user (.*) in group (.*) in the notification center$")
    public void iSeeMentionNotificationFromUserFromGroup(String message, String user, String group) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        message = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Message from user is not '%s'.", message) , getNotificationsPage().isGroupNotificationVisible(message));
        assertThat("SenderName is not visible in group notification.", getNotificationsPage().isSenderNameInGroupMessageVisible(user));
        assertThat("GroupName is not visible in notification center.", getNotificationsPage().isGroupNameInNotificationVisible(group));
    }

    @When("^I see ongoing call in group \"(.*)\" in the notification center$")
    public void iSeeOngoingCallNotificationFromGroup(String group) {
        assertThat("OngoingCall is not visible in notification center", getNotificationsPage().isOngoingCallVisible());
        assertThat("GroupName is not visible in notification center.", getNotificationsPage().isGroupNameInNotificationVisible(group));
    }

    @When("^I tap on the message \"(.*)\" from 1:1 conversation from user (.*) in the notification center$")
    public void iTapOnNotificationFromUser1On1(String message, String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        getNotificationsPage().tapOnNotificationFromUserIn1On1(user, message);
    }

    @When("^I tap on the message \"(.*)\" from group conversation from user (.*) in the notification center$")
    public void iTapOnNotificationFromUserFromGroup(String message, String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("SenderName is not visible in group notification.", getNotificationsPage().isSenderNameInGroupMessageVisible(user));
        getNotificationsPage().tapOnNotificationFromUserInGroup(message);
    }

    @When("^I see the message that my Websocket connecting is running$")
    public void iSeeWebSocketMessage() {
        getNotificationsPage().iSeeWebSocketRunningMessage();
    }
}
