package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.GroupConversationDetailsPage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class GroupConversationDetailsPageSteps {

    private final AndroidTestContext context;

    public GroupConversationDetailsPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private GroupConversationDetailsPage getGroupConversationDetailsPage() {
        return context.getPage(GroupConversationDetailsPage.class);
    }

    @When("^I see group details page$")
    public void iSeeGroupDetailsPage() {
        assertThat("Group details page is not visible.", getGroupConversationDetailsPage().isGroupDetailsPageVisible());
    }

    @When("^I close the group conversation details through X icon$")
    public void iCloseGroupDetails() {
        getGroupConversationDetailsPage().closeGroupDetailsPage();
    }

    // Context Menu

    @When("^I tap show more options button$")
    public void iTapShowMoreOptionsButton() {
        getGroupConversationDetailsPage().tapShowMoreOptionsButton();
    }

    @When("^I tap leave group button$")
    public void iTapLeaveGroupButton() {
        getGroupConversationDetailsPage().tapLeaveGroupButton();
    }

    @When("^I tap leave group confirm button$")
    public void iTapLeaveGroupConfirmButton() {
        getGroupConversationDetailsPage().tapLeaveGroupConfirmButton();
    }

    @When("^I tap delete group button$")
    public void iTapDeleteGroupButton() {
        getGroupConversationDetailsPage().tapDeleteGroupButton();
    }

    @When("^I tap remove group button$")
    public void iTapRemoveGroupButton() {
        getGroupConversationDetailsPage().tapRemoveGroupButton();
    }

    @When("^I tap move to archive button$")
    public void iTapMoveToArchiveButton() {
        getGroupConversationDetailsPage().tapMoveToArchiveButton();
    }

    @When("^I confirm archive conversation$")
    public void iConfirmArchiveConversation() {
        getGroupConversationDetailsPage().tapConfirmMoveToArchiveButton();
    }

    @When("^I do not see delete group button$")
    public void iDoNotSeeDeleteGroupButton() {
        assertThat("Delete Group button is visible but it should not", getGroupConversationDetailsPage().isDeleteGroupButtonInvisible());
    }

    // Notifications

    @When("^I tap notifications button on group details$")
    public void iTapNotificationsButton() {
        getGroupConversationDetailsPage().tapNotificationsButton();
    }

    @When("^I see default notification is Everything$")
    public void iSeeDefaultNotificationEverything() {
        assertThat( "Everything is not selected", getGroupConversationDetailsPage().isEverythingVisible());
    }

    @When("^I see notification status of \"(.*)\" in group details page$")
    public void iSeeGroupNotificationStateStatus(String notification) {
        assertThat("Notification status is not correct.", getGroupConversationDetailsPage().isNotificationCorrect(notification));
    }

    @When("^I tap notification status of \"(.*)\" in group details page$")
    public void iTapGroupNotificationStateStatus(String notification) {
        getGroupConversationDetailsPage().tapNotificationOf(notification);
    }

    // Clear content

    @When("^I tap clear content button on group details page$")
    public void iTapContentButtonGroupDetailsPage() {
        getGroupConversationDetailsPage().tapClearContentButton();
    }

    @When("^I tap clear content confirm button on group details page$")
    public void iTapClearContentConfirmButton() {
        getGroupConversationDetailsPage().tapClearContentConfirmButton();
    }

    @When("^I see \"(.*)\" toast message on group details page$")
    public void iSeeToasMessageGroupDetailsPage(String message) {
        assertThat("Toast message is not visible.", getGroupConversationDetailsPage().isTextDisplayed(message));
    }

    // Options section

    @When("^I tap on Options tab$")
    public void iOpenOptionsTabGroupDetailsPage() {
        getGroupConversationDetailsPage().tapOnOptionsTab();
    }

    @When("^I see \"(.*)\" as group name$")
    public void iSeeGroupName(String name) {
        assertThat("Group name is not correct.", getGroupConversationDetailsPage().isGroupNameVisible(name));
    }

    @When("^I tap on \"(.*)\" group name$")
    public void iTapOnGroupName(String name) {
        getGroupConversationDetailsPage().tapOnGroupName(name);
    }

    @When("^I change group name to \"(.*)\" as new group name$")
    public void iChangeGroupNameTo(String newName) {
        getGroupConversationDetailsPage().changeGroupName(newName);
    }

    @When("^I see toast message \"(.*)\" on group details page$")
    public void iSeeToastMessageOnGroupDetails(String message) {
        getGroupConversationDetailsPage().isToastMessageDisplayed(message);
    }

    @When("^I see the guests options is \"(.*)\" on conversation details page$")
    public void iSeeGuestOptionsStateConversationDetails(String state) {
        assertThat("Guest options does not show correct state.", getGroupConversationDetailsPage().isGuestsStateCorrect(state));
    }

    @When("^I tap on guest options on conversation details page$")
    public void iTapGuestOptions() {
        getGroupConversationDetailsPage().tapGuestOptions();
    }

    @When("^I see the (.*) switch is at \"(.*)\" state$")
    public void iSeeSwitchState(String toggle, String state) {
        assertThat(String.format("%s switch is not in correct state.", toggle), getGroupConversationDetailsPage().isSwitchStateVisible(toggle, state));
    }

    // Services

    @When("^I tap on the services switch$")
    public void iTapServicesSwitch() {
        getGroupConversationDetailsPage().tapOnServicesSwitch();
    }

    @When("^I tap on disable button on pop up$")
    public void iTapDisableButton() {
        getGroupConversationDetailsPage().tapDisableButton();
    }

    // Self deleting messages

    @When("^I see Self Deleting messages option is in (.*) state$")
    public void iSeeSelfDeletingMessagesStatus(String state) {
        assertThat("Self Deleting Messages are not in the right state for group." , getGroupConversationDetailsPage().getStateSelfDeletingMessagesGroup(state));
    }

    @When("^I tap on Self Deleting messages option for group conversation$")
    public void tapSelfDeletingMessages() {
        getGroupConversationDetailsPage().tapSelfDeletingMessagesOptionGroupDetails();
    }

    @When("^I tap on Self Deleting messages toggle$")
    public void tapSelfDeletingMessagesToggle() {
        getGroupConversationDetailsPage().tapSelfDeletingMessagesToggle();
    }

    @When("^I tap on (.*) timer button for changing the timer for group conversation$")
    public void iTapOnTimerButton(String timer) {
        getGroupConversationDetailsPage().tapOnTimerButton(timer);
    }

    @When("^I tap on Apply button$")
    public void iTapApplyButton() {
        getGroupConversationDetailsPage().tapApplyButton();
    }

    // MLS

    @When("^I see the conversation is using MLS protocol$")
    public void iSeeProtocolMLS() {
        assertThat("Protocol is not as expected.", getGroupConversationDetailsPage().isProtocolMLSDisplayed());
    }

    @When("^I do not see the conversation is using MLS protocol$")
    public void iDoNotSeeProtocolMLS() {
        assertThat("Protocol is not as expected.", getGroupConversationDetailsPage().isProtocolMLSInvisible());
    }

    @When("^I see Cipher Suite entry in group details page$")
    public void iSeeCipherSuite() {
        assertThat("Cipher Suite entry is not displayed.", getGroupConversationDetailsPage().isCipherSuiteDisplayed());
    }

    @When("^I see Last Key Material Update entry in group details page$")
    public void iSeeLastKeyMaterial() {
        assertThat("Last Key Material Update entry is not displayed.", getGroupConversationDetailsPage().isLastKeyMaterialDisplayed());
    }

    @When("^I see Group State entry in group details page$")
    public void iSeeGroupState() {
        assertThat("Group State entry is not displayed.", getGroupConversationDetailsPage().isGroupStateDisplayed());
    }

    @When("^I see Group State status is \"(.*)\" in group details page$")
    public void iSeeGroupStateStatus(String status) {
        assertThat("Group State entry is not correct.", getGroupConversationDetailsPage().isGroupStateStatusCorrect(status));
    }

    // Participants Section

    @When("^I tap on Participants tab$")
    public void iOpenParticipantsTabGroupDetailsPage() {
        getGroupConversationDetailsPage().tapOnParticipantsTab();
    }

    @When("^I tap on Add Participants button$")
    public void iTapAddParticipants() {
        getGroupConversationDetailsPage().tapAddParticipants();
    }

    @When("^I tap on Services tab")
    public void iTapServicesTab() {
        getGroupConversationDetailsPage().tapOnServicesTab();
    }

    @When("^I see user (.*) in participants list$")
    public void iSeeUserInParticipantsList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("User is not visible in participants list.", getGroupConversationDetailsPage().isUserVisible(user));
    }

    @When("^I do not see user (.*) in participants list$")
    public void iDoNotSeeUserInParticipantsList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("User is not visible in participants list.", getGroupConversationDetailsPage().isUserInvisible(user));
    }

    @When("^I see status icon displayed next to user \"(.*)\" avatar on participants list$")
    public void iSeeStatusNextToUserParticipantsList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Status icon is not displaued.", getGroupConversationDetailsPage().isStatusOtherUserVisible(user));
    }

    @When("^I tap on user (.*) in participants list$")
    public void iTapOnUser(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        getGroupConversationDetailsPage().openParticipantProfile(user);
    }

    @When("^I tap on show all participants button$")
    public void iTapShowAllButton() {
        getGroupConversationDetailsPage().tapShowAllParticipants();
    }

    @When("^I close the group participants list through the back arrow$")
    public void iCloseGroupParticipantsList() {
        getGroupConversationDetailsPage().closeGroupParticipantsList();
    }

    @When("^I scroll to the (bottom|top) of group details page$")
    public void IScrollToTheBottom(String direction) {
        if (direction.equalsIgnoreCase("bottom")) {
            getGroupConversationDetailsPage().scrollToTheBottom();
        } else {
            getGroupConversationDetailsPage().scrollToTheTop();
        }
    }
}
