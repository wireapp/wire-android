package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ConnectedUserProfilePage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

public class ConnectedUserProfilePageSteps {

    private final AndroidTestContext context;

    public ConnectedUserProfilePageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private ConnectedUserProfilePage getConnectedUserProfilePage() {
        return context.getPage(ConnectedUserProfilePage.class);
    }

    @Then("^I see connected user (.*) profile$")
    public void iSeeConnectedUserProfile(String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("%s should be visible", name), getConnectedUserProfilePage().isUserProfileVisible(name));
    }

    @When("^I tap open conversation button on connected user profile page$")
    public void iTapOpenConversationButton() {
        getConnectedUserProfilePage().tapOpenConversationButton();
    }

    @When("^I tap start conversation button on connected user profile page$")
    public void iTapStartConversationButton() {
        getConnectedUserProfilePage().tapStartConversationButton();
    }

    @When("^I tap show more options button on user profile screen$")
    public void iTapShowMoreOptionsButtonOnUserProfileScreen() {
        getConnectedUserProfilePage().tapShowMoreOptionsButton();
    }

    // Notifications

    @When("^I tap notifications button on user profile screen$")
    public void iTapNotificationsButton() {
        getConnectedUserProfilePage().tapNotificationsButton();
    }

    @When("^I see default notification is Everything on user profile screen$")
    public void iSeeDefaultNotificationEverything() {
        assertThat( "Everything is not selected", getConnectedUserProfilePage().isEverythingVisible());
    }

    @When("^I tap notification status of \"(.*)\" on user profile screen$")
    public void iTapGroupNotificationStateStatus(String notification) {
        getConnectedUserProfilePage().tapNotificationOf(notification);
    }

    @When("^I see notification status of \"(.*)\" on user profile screen$")
    public void iSeeGroupNotificationStateStatus(String notification) {
        assertThat("Notification status is not correct.", getConnectedUserProfilePage().isNotificationCorrect(notification));
    }

    // Promoting / Demoting

    @When("^I tap on edit button to change user role$")
    public void itapEditButtonToChangeUserRole() {
        getConnectedUserProfilePage().tapEditButton();
    }

    @When("^I change the user role to admin$")
    public void iChangeUserRoleToAdmin() {
        getConnectedUserProfilePage().changeUserRoleAdmin();
    }

    @When("^I see new role for user is admin$")
    public void iSeeNewRoleForUserIsAdmin() {
        assertThat("User role is not admin.", getConnectedUserProfilePage().isAdminRoleVisible());
    }

    // Block

    @When("^I see Block option$")
    public void iSeeBlockUserButton() {
        assertThat( "Block option is not visible", getConnectedUserProfilePage().isBlockOptionVisible());
    }

    @When("^I do not see Block option$")
    public void iDoNotSeeBlockUserButton() {
        assertThat( "Block option is not visible", getConnectedUserProfilePage().isBlockOptionInvisible());
    }

    @When("^I tap on Block option$")
    public void iTapBlockUserOption() {
        getConnectedUserProfilePage().tapBlockOption();
    }

    @When("^I tap on Unblock option$")
    public void iTapUnblockUserOption() {
        getConnectedUserProfilePage().tapUnblockOption();
    }

    @When("^I tap Block button on alert$")
    public void iTapBlockButtonOnAllert() {
        getConnectedUserProfilePage().tapBlockButtonOnAllert();
    }

    @When("^I see toast message \"(.*)\" in user profile screen$")
    public void iSeeToastMessageInUserProfile(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("System message is not visible", getConnectedUserProfilePage().isToastMessageVisible(user));
    }

    @When("^I see Blocked label$")
    public void iSeeBlockedLabel() {
        assertThat("Blocked label is not visible.", getConnectedUserProfilePage().isBlockedLabelVisible());
    }

    @When("^I do not see Blocked label$")
    public void iDoNotSeeBlockedLabel() {
        assertThat("Blocked label is still visible.", getConnectedUserProfilePage().isBlockedLabelInvisible());
    }

    @When("^I see Unblock User button$")
    public void iSeeUnblockUserButton() {
        assertThat("Unblock User button is not visible.", getConnectedUserProfilePage().isUnblockUserButtonVisible());
    }

    @When("^I do not see Unblock User button$")
    public void iDoNotSeeUnblockUserButton() {
        assertThat("Unblock User button is still visible.", getConnectedUserProfilePage().isUnblockUserButtonInvisible());
    }

    @When("^I tap Unblock User button$")
    public void iTapUnblockUserButton() { getConnectedUserProfilePage().tapUnblockUserButton();
    }

    @When("^I tap Unblock button alert$")
    public void iTapUnblockButtonAlert() { getConnectedUserProfilePage().tapUnblockButtonAlert();
    }

    // Remove from group

    @When("^I see remove from group button$")
    public void iSeeRemoveFromGroupButton() {
        assertThat("Remove button is not visible.", getConnectedUserProfilePage().isRemoveButtonVisible());
    }

    @When("^I see Remove From Group button for service$")
    public void iSeeRemoveFromGroupButtonForService() {
        assertThat("Remove button is not visible.", getConnectedUserProfilePage().isRemoveServiceButtonVisible());
    }

    @When("^I do not see remove from group button$")
    public void iDoNotSeeRemoveFromGroupButton() {
        assertThat("Remove button is not visible.", getConnectedUserProfilePage().isRemoveButtonInvisible());
    }

    @When("^I do not see Remove From Group button for service$")
    public void iDoNotSeeRemoveFromGroupButtonForService() {
        assertThat("Remove button is not visible.", getConnectedUserProfilePage().isRemoveServiceButtonInvisible());
    }

    @When("^I tap remove from group button$")
    public void iTapRemoveFromGroupButton() {
        getConnectedUserProfilePage().tapRemoveFromGroupButton();
    }

    @When("^I tap Remove From Group button for service$")
    public void iTapRemoveFromGroupButtonForService() {
        getConnectedUserProfilePage().tapRemoveServiceFromGroupButton();
    }

    @When("^I see alert asking me if I want to remove user (.*) from group$")
    public void iSeeAlertForRemovingUser(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Alert is not visible.", getConnectedUserProfilePage().isAlertRemoveUserVisible(user));
    }

    @When("^I tap remove button on alert$")
    public void iTapRemoveFromGroupButtonOnALert() {
        getConnectedUserProfilePage().tapRemoveFromGroupButtonOnAlert();
    }

    // Devices

    @When("^I tap on devices tab in connected user profile$")
    public void iOpenDevicesOfUser() {
        getConnectedUserProfilePage().openDevices();
    }

    @When("^I see \"(.*)\" is listed under devices in connected user profile$")
    public void iSeeDevicesOfUser(String device) {
        assertThat(String.format("User Device '%s' not visible.", device), getConnectedUserProfilePage().areDevicesVisible(device));
    }

    @When("^I tap device \"(.*)\" listed under devices in connected user profile$")
    public void iTapFirstDevicesOfUser(String device) {
        getConnectedUserProfilePage().tapDevice(device);
    }


    @When("^I see the verified shield for other users device$")
    public void iSeeVerifiedShieldForOtherUser() {
        assertThat("Device is not verified.", getConnectedUserProfilePage().isVerifiedShieldVisible());
    }

    @When("^I see start conversation button on connected user profile page$")
    public void iSeeStartConversationButton() {
        assertThat("Start Conversation button is not visible on connected user profile page.", getConnectedUserProfilePage().isStartConversationVisible());
    }

    // Banners and Labels

    @Then("^I see federated guest icon for user \"(.*)\" on connected user profile page$")
    public void iSeeFederatedGuestLabelForUser(String user) {
        assertThat(String.format("Federated Guest label for user '%s' is not visible on user details page.", user), getConnectedUserProfilePage().isFederatedLabelVisible());
    }

    @Then("^I do not see federated guest icon for user \"(.*)\" on connected user profile page$")
    public void iDoNotSeeFederatedGuestLabelForUser(String user) {
        assertThat(String.format("Federated Guest label for user '%s' is not visible on user details page.", user), getConnectedUserProfilePage().isFederatedLabelInvisible());
    }

    @Then("^I see classified domain label with text \"(.*)\" on connected user profile page$")
    public void iSeeClassifiedBannerTextForUser(String text) {
        assertThat("Classified banner does not have correct text on user details page.", getConnectedUserProfilePage().isTextDisplayed(text));
    }

    @Then("^I do not see classified domain label with text \"(.*)\" on connected user profile page$")
    public void iDoNotSeeClassifiedBannerTextForUser(String text) {
        assertThat("Classified banner does not have correct text on user details page.", getConnectedUserProfilePage().isTextInvisible(text));
    }

    @When("^I scroll to the (bottom|top) of user profile page$")
    public void IScrollToTheBottom(String direction) {
        if (direction.equalsIgnoreCase("bottom")) {
            getConnectedUserProfilePage().scrollToTheBottom();
        } else {
            getConnectedUserProfilePage().scrollToTheTop();
        }
    }

    // Alerts

    @When("^I see unable to start conversation alert$")
    public void iSeeUnableToStartConversationAlert() {
        getConnectedUserProfilePage().isUnableToStartConversationAlertVisible();
    }

    @When("^I see subtext \"(.*)\" in unable to start conversation alert$")
    public void iSeeUnableToStartConversationAlertSubtext(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Text is not visible in alert.", getConnectedUserProfilePage().isTextContainsDisplayed(user));
    }

    // Misc

    @When("^I close the user profile through the close button$")
    public void iCloseUserProfile() {
        getConnectedUserProfilePage().closeUserProfile();
    }

    @When("^I close the user profile to go back to conversation details$")
    public void iCloseUserProfileBackToConversationDetails() {
        getConnectedUserProfilePage().closeUserProfileConversationDetails();
    }
}
