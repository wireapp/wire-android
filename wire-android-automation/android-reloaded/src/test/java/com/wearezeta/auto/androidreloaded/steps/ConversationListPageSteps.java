package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ConversationListPage;
import com.wearezeta.auto.androidreloaded.pages.LoginPage;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.appium.java_client.android.options.UiAutomator2Options;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.*;

public class ConversationListPageSteps {

    private final AndroidTestContext context;

    public ConversationListPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private ConversationListPage getConversationListPage() {
        return context.getPage(ConversationListPage.class);
    }

    private static final Logger log = ZetaLogger.getLog(SSOPageSteps.class.getSimpleName());

    @When("^I wait until I am fully logged in after upgrading from the old app$")
    public void iWaitUntilIAmLoggedInAfterUpgrade() {
        assertThat("Setting up Wire did not succeed.", getConversationListPage().waitUntilSetupIsDone());
        assertThat("Sync is not finished before timeout.", getConversationListPage().waitUntilSyncBarInvisible());
        assertThat("Conversation list should be visible, but is not.", getConversationListPage().waitUntilWelcomeToNewAndroidAlertVisible());
    }

    @When("^I wait until I am fully logged in$")
    public void iWaitUntilIAmLoggedIn() {
        context.getPage(LoginPage.class).waitUntilLoginButtonIsInvisibleAfterTap();
        assertThat("Setting up Wire did not succeed.", getConversationListPage().waitUntilSetupIsDone());
        assertThat("Sync is not finished before timeout.", getConversationListPage().waitUntilSyncBarInvisible());
        getConversationListPage().waitUntilWebsocketPopUpIsInvisible();
    }

    @When("^I accept share data alert$")
    public void iAcceptShareDataAlert() {
        assertThat("Share Data alert is not visible.", getConversationListPage().waitUntilShareDataAlertVisible());
        getConversationListPage().iTapAgreeButtonShareDataAlert();
    }

    @When("^I decline share data alert$")
    public void iDeclineShareDataAlert() {
        final UiAutomator2Options capabilities = new UiAutomator2Options();
        capabilities.getCapability("isCountlyAvailable");
        if (CommonDeviceSteps.isCountlyAvailable().equals("false")) {
            log.info("Fdroid App or bund apk. Skipping step since countly is not available in these apks.");
        } else {
            assertThat("Share Data alert is not visible.", getConversationListPage().waitUntilShareDataAlertVisible());
            getConversationListPage().iTapDeclineButtonShareDataAlert();
        }
    }

    @When("^I do not see the connecting banner displayed$")
    public void iDoNotSeeConnectingBanner() {
        assertThat("Connecting banner did not disappear.", getConversationListPage().waitUntilSyncBarInvisible());
    }

    @When("^I see conversation list$")
    public void iSeeConversationList() {
        assertThat("Conversation list should be visible, but is not.", getConversationListPage().waitUntilConversationListVisible());
    }

    @When("^I do not see conversation list$")
    public void iDoNotSeeConversationList() {
        assertThat("Conversation list is visible although it should not be.", getConversationListPage().isConversationListInvisible());
    }

    @When("^I see Welcome to New Android alert on conversation list$")
    public void iSeeAlertAfterUpgrade() {
        assertThat("Alert is not visible.", getConversationListPage().waitUntilWelcomeToNewAndroidAlertVisible());
    }

    @When("^I see (.*) button on welcome to new android alert$")
    public void iSeeButton(String buttonName) {
        assertThat("Button is not visible.", getConversationListPage().isTextDisplayed(buttonName));
    }

    @When("^I tap (.*) button on welcome to new android alert$")
    public void iTapButtonAlert(String buttonName) {
        getConversationListPage().tapButton(buttonName);
    }

    @When("^I see welcome message$")
    public void iSeeWelcomeMessage() {
        getConversationListPage().isWelcomeMessageVisible();
    }

    @When("^I see introduction message \"(.*)\"$")
    public void iSeeIntroductionMessage(String text) {
        getConversationListPage().isTextDisplayed(text);
    }

    @When("^I tap on menu button on conversation list$")
    public void iOpenMenuButtonOnConversationList() {
        getConversationListPage().tapMenuButton();
    }

    @When("^I tap User Profile Button$")
    public void iTapUserProfileButton() {
        getConversationListPage().tapUserProfileButton();
    }

    @When("^I click close button on New Conversation screen to go back conversation details$")
    public void iClickCloseButton() {
        getConversationListPage().clickCloseButton();
    }

    @Then("^I see conversation \"(.*)\" in (?:conversation list|favorites list|folder list)$")
    public void iSeeConversationOnConversationList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation is not visible in the list.", getConversationListPage().isConversationDisplayed(userName));
    }

    @Then("^I do not see conversation \"(.*)\" in conversation list$")
    public void iDoNotSeeConversationOnConversationList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation is visible in the list, but should be hidden.", getConversationListPage().isConversationInvisible(userName));
    }

    @Then("^I do not see conversation \"(.*)\" in (?:favorites list|folder list)$")
    public void iDoNotSeeConversationOnFavoritesList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation is visible in the list, but should be hidden.", getConversationListPage().isConversationInvisible(userName));
    }

    @Then("^I see unread conversation \"(.*)\" in conversation list$")
    public void iSeeUnreadConversationOnConversationList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation is not visible in the list.", getConversationListPage().isUnreadConversationDisplayed(userName));
    }

    @Then("^I do not see unread conversation \"(.*)\" in conversation list$")
    public void iDoNotSeeUnreadConversationOnConversationList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation is not visible in the list.", getConversationListPage().isUnreadConversationInvisible(userName));
    }

    @Then("^I see conversation \"(.*)\" is having (.*) unread messages in conversation list$")
    public void iSeeConversationHasUnreadMessagesConversationList(String userName, String number) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation does not have the right number of unread messages", getConversationListPage().unreadMessagesCount(number));
    }

    @Then("^I see conversation \"(.*)\" is having pending status in conversation list$")
    public void iSeeConversationAsPendingOnConversationList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation does not have the pending status visible in the list, but should have.", getConversationListPage().isConversationPendingStatusVisible(userName));
    }

    @Then("^I do not see conversation \"(.*)\" is having pending status in conversation list$")
    public void iDoNotSeeConversationAsPendingOnConversationList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation does not have the pending status visible in the list, but should have.", getConversationListPage().isConversationPendingStatusInvisible(userName));
    }

    @Then("^I see subtitle \"(.*)\" of conversation \"(.*)\" in conversation list$")
    public void iSeeSubtitleOfConversation(String text, String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation does not have the expected subtitle visible in the list, but should have.", getConversationListPage().isSubtitleVisible(userName, text));
    }

    @When("^I see (.*) has \"(.*)\" identifier next to his name in conversation list$")
    public void iSeeIdentifierInConversationList(String member, String identifier) {
        member = context.getUsersManager().replaceAliasesOccurrences(member, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Member does not have correct identifier.", getConversationListPage().isMemberIdentifierVisible(member, identifier));
    }

    @When("^I see status icon displayed next to my avatar on conversation list$")
    public void iSeeStatusSelfUser() {
        assertThat("Status icon is not displaued.", getConversationListPage().isSelfUserStatusVisible());
    }

    @When("^I see status icon displayed next to user \"(.*)\" avatar on conversation list$")
    public void iSeeStatus1on1Conversation(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Status icon is not displaued.", getConversationListPage().isStatusOtherUserVisible(user));
    }

    @When("^I tap on conversation name \"(.*)\" in conversation list$")
    public void iTapOnConversationName(String conversationName) {
        conversationName = context.getUsersManager().replaceAliasesOccurrences(conversationName, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationListPage().tapConversationName(conversationName);
    }

    @When("^I tap on unread conversation name \"(.*)\" in conversation list$")
    public void iTapOnUnreadConversationName(String conversationName) {
        conversationName = context.getUsersManager().replaceAliasesOccurrences(conversationName, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationListPage().tapUnreadConversationName(conversationName);
    }

    @When("^I long tap on conversation name \"(.*)\" in (?:conversation list|favorites list|folder list)$")
    public void iLongTapConversationName(String conversationName) {
        conversationName = context.getUsersManager().replaceAliasesOccurrences(conversationName, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationListPage().longTapConversationName(conversationName);
    }

    @When("^I tap clear content button on conversation list$")
    public void iTapClearContentButtonConversationList() {
        getConversationListPage().tapClearContentButton();
    }

    @When("^I tap clear content confirm button on conversation list$")
    public void iTapClearContentConfirmButton() {
        getConversationListPage().tapClearContentConfirmButton();
    }

    @When("^I do not see block option on conversation list$")
    public void iDoNotSeeBlockOption() {
        assertThat("Block option is displayed.", getConversationListPage().isBlockOptionInvisible());
    }

    @When("^I tap block option on conversation list$")
    public void iTapBlockOption() {
        getConversationListPage().tapBlockConversationList();
    }

    @When("^I tap block confirm button on conversation list$")
    public void iTapBlockConfirmButton() {
        getConversationListPage().tapBlockConfirmButton();
    }

    @When("^I tap unblock option on conversation list$")
    public void iTapUnblockOption() {
        getConversationListPage().tapUnblockConversationList();
    }

    @When("^I tap unblock confirm button on conversation list$")
    public void iTapUnblockConfirmButton() {
        getConversationListPage().tapUnblockConfirmButton();
    }

    @When("^I see \"(.*)\" toast message on conversation list$")
    public void iSeeToastMessageConversationList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Toast message is not visible.", getConversationListPage().isTextDisplayed(user));
    }

    @When("^I see toast message containing \"(.*)\" on conversation list$")
    public void iSeeToastMessageIgnoreRequestConversationList(String text) {
        assertThat("Toast message is not visible.", getConversationListPage().isTextContainsDisplayed(text));
    }

    // Search
    @When("^I tap on start a new conversation button$")
    public void iTapStartNewConversation() {
        getConversationListPage().tapStartNewConversation();
    }

    @When("^I tap on search people field$")
    public void iTapSearchPeopleField() {
        getConversationListPage().tapSearchPeopleField();
    }

    @When("^I tap the back arrow inside the search people field$")
    public void iTapBackArrowInSearchPeopleField() {
        getConversationListPage().tapBackArrowButtonInsideSearchField();
    }

    @When("^I tap the back arrow to go back to conversation list$")
    public void iTapBackArrowToGoConversationList() {
        getConversationListPage().tapBackToConversationList();
    }

    @When("^I tap on search conversation field$")
    public void iTapSearchConversationsField() {
        getConversationListPage().tapSearchConversationsField();
    }

    @When("^I type conversation name \"(.*)\" in search field$")
    public void iTypeConvNameInSearchField(String conversation) {
        conversation = context.getUsersManager().replaceAliasesOccurrences(conversation, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationListPage().sendKeysUserNameSearchField(conversation);
    }

    @When("^I type conversation name \"(.*)\" in search field in upper case$")
    public void iTypeConvNameInSearchFieldUpperCase(String conversation) {
        conversation = context.getUsersManager().replaceAliasesOccurrences(conversation, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationListPage().sendKeysUserNameSearchField(conversation.toUpperCase());
    }

    @When("^I type conversation name \"(.*)\" in search field in lower case$")
    public void iTypeConvNameInSearchFieldLowerCase(String conversation) {
        conversation = context.getUsersManager().replaceAliasesOccurrences(conversation, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationListPage().sendKeysUserNameSearchField(conversation.toLowerCase());
    }

    @When("^I type a random conversation name in search field$")
    public void iTypeRandomConvNameInSearchField() {
        getConversationListPage().sendRandomKeysUserNameSearchField();
    }

    @When("^I type unique user name \"(.*)\" on search field in conversation list page")
    public void iTypeUniqueUserNameInSearchField(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        getConversationListPage().sendKeysUserNameSearchField(userName);
    }

    @When("^I type user email \"(.*)\" in search field")
    public void iTypeUserEmailInSearchField(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.EMAIL_ALIAS);
        getConversationListPage().sendKeysUserNameSearchField(userName);
    }

    @When("^I type first (\\d+) chars of user name \"(.*)\" in search field$")
    public void ITypePartialUsernameInSearchFiled(String partialWords, String text) {
        text = context.getUsersManager().replaceAliasesOccurrences(text, ClientUsersManager.FindBy.NAME_ALIAS);
        if (partialWords != null) {
            int partialSize = Integer.parseInt(partialWords.replaceAll("[\\D]", ""));
            int length = text.length();
            text = (partialSize < length) ? text.substring(0, partialSize) : text;
        }
        getConversationListPage().sendKeysUserNameSearchField(text);
    }

    @When("^I type first (\\d+) chars of group name \"(.*)\" in search field$")
    public void ITypePartialGroupNameInSearchFiled(String partialWords, String text) {
        if (partialWords != null) {
            int partialSize = Integer.parseInt(partialWords.replaceAll("[\\D]", ""));
            int length = text.length();
            text = (partialSize < length) ? text.substring(0, partialSize) : text;
        }
        getConversationListPage().sendKeysUserNameSearchField(text);
    }

    @When("^I see conversation name \"(.*)\" in Search result list$")
    public void iSeeUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Conversation name is not visible in Search result list.", getConversationListPage().isConversationVisibleInSearchResult(userName));
    }

    @When("^I do not see conversation name \"(.*)\" in Search result list$")
    public void iDoNotSeeUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Conversation name is visible in Search result list.", getConversationListPage().isConversationInvisibleInSearchResult(userName));
    }

    // Filter

    @Then("^I see conversation name \"(.*)\" on filter bottom sheet header is displayed$")
    public void iSeeConversationOnFilterBottomSheet(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Conversation name is not displayed on the bottom sheet header.", getConversationListPage().isConversationDisplayedOnFilter(userName));
    }

    // Labels

    @When("^I see user (.*) is having the (.*) label on conversation list$")
    public void iSeeLabelUser(String user, String label) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Label '%s' is not visible.", label), getConversationListPage().isLabelForUserVisible(user, label));
    }

    @When("^I do not see user (.*) is having the (.*) label on conversation list$")
    public void iDoNotSeeLabelUser(String user, String label) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("Label '%s' is not visible.", label), getConversationListPage().isLabelForUserInvisible(user, label));
    }
}
