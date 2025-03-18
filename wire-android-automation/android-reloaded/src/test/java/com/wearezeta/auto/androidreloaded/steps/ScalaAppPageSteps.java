package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ScalaAppPage;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Given;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.Optional;

import static org.hamcrest.MatcherAssert.assertThat;


public class ScalaAppPageSteps {

    private final AndroidTestContext context;

    public ScalaAppPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private ScalaAppPage getScalaAppPage() {
        return context.getPage(ScalaAppPage.class);
    }

    @When("^I select staging backend in the old scala app$")
    public void iSelectStagingInScalaApp() {
        getScalaAppPage().openStagingBackend();
    }

    @When("^I accept custom backend alert in the old scala app$")
    public void iAcceptCustomBackendAlert() {
        getScalaAppPage().acceptCustomBackendAlert();
    }

    @Then("^I see custom backend info pill for backend (.*) on welcome page in the old scala app$")
    public void iSeeCustomBackendInfoPillForBackendStagingOnWelcomePage(String customBE) {
        assertThat(String.format("No pill seen for custom BE %s", customBE),
                getScalaAppPage().isCustomBackendPillVisibleForBackend(customBE));
    }

    @Given("^I tap Log in button on Welcome page in the old scala app$")
    public void iTapLoginInScalaApp() {
        getScalaAppPage().tapLogin();
    }

    @When("^I tap Login with Email button on Custom backend welcome page in the old scala app$")
    public void iTapLoginWithEmailButtonOnCustomBackendWelcomePage() {
        getScalaAppPage().tapOnLoginWithEmailButton();
    }

    @Given("^I sign in using my email in the old scala app$")
    public void iSignInUsingMyEmail() {
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getScalaAppPage().setLogin(self.getEmail());
        getScalaAppPage().setPassword(self.getPassword());
        getScalaAppPage().logIn();
    }

    @And("^I accept First Time overlay in the old scala app$")
    public void iAcceptTheOverLay() {
        // Wait longer because of login loading spinner
        getScalaAppPage().tapOkButton(Timedelta.ofSeconds(30));
        assertThat("First Time overlay is still visible after timeout", getScalaAppPage().isOverlayInvisible());
        assertThat("Sync of conversation list not successful", getScalaAppPage().waitUntilConversationListOrAlertLoadedSuccessfully());
    }

    @When("^I accept Help us make Wire better popup in the old scala app$")
    public void iAcceptHelpUsMakeWireBetterPopup() {
        getScalaAppPage().waitUntilAlertVisible();
        getScalaAppPage().tapIAgree();
    }

    @Then("^I see conversation \"(.*)\" in Recent View in the old scala app$")
    public void iSeeConversationInRecentView(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("The conversation '%s' is not visible in the list", userName), getScalaAppPage().isConversationVisible(userName));
    }

    @Then("^I do not see conversation \"(.*)\" in Recent View in the old scala app$")
    public void iDoNotSeeConversationInRecentView(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("The conversation '%s' is visible in the list, but should not be.", userName), getScalaAppPage().isConversationInvisible(userName));
    }

    @When("^I tap on conversation name \"(.*)\" in the old scala app$")
    public void iTapOnContactName(String conversationName) {
        conversationName = context.getUsersManager().replaceAliasesOccurrences(conversationName, ClientUsersManager.FindBy.NAME_ALIAS);
        getScalaAppPage().tapListItem(conversationName);
    }

    @Then("^I see the message \"(.*)\" in the conversation view in the old scala app$")
    public void iSeeMyMessageInTheDialog(String msg) {
        assertThat(String.format("The message does not contain '%s' in the conversation view", msg), getScalaAppPage().waitUntilMessageWithTextVisible(msg));
    }

    @Then("^I see the system message \"(.*)\" in the conversation view in the old scala app$")
    public void iSeeSystemMessageInTheDialog(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("The message does not contain '%s' in the conversation view", userName), getScalaAppPage().waitUntilSystemMessageWithTextVisible(userName));
    }

    @Then("^I see an image in the conversation view in the old scala app$")
    public void iSeeImage() {
        assertThat("No image is displayed.", getScalaAppPage().isImageDisplayed());
    }

    @Then("^I see a file with name \"(.*)\" in the conversation view in the old scala app$")
    public void iSeeFile(String fileName) {
        assertThat("No file is displayed.", getScalaAppPage().isFileDisplayed(fileName.toUpperCase()));
    }

    @When("^I see the play button on the audio message in the conversation view in the old scala app$")
    public void ITSeeButtonOnAudioMessage() {
        assertThat("Play button on audio file is not visible.", getScalaAppPage().isAudioPlayButtonVisible());
    }

    @When("^I tap on text input in the old scala app$")
    public void ITapOnTextInput() {
        getScalaAppPage().tapOnTextInput();
    }

    @When("^I type the message \"(.*)\" and send it by cursor Send button in the old scala app$")
    public void ITypeMessageAndSendIt(String msg) {
        getScalaAppPage().typeAndSendMessage(msg);
    }

    @When("^I see conversation (.*) with unread icon showing (\\d+) new messages in the old scala app")
    public void iSeeXnewMessagesOnConversation(String contact, int amount) {
        assertThat(String.format("Conversation %s does not have %d new messages.", contact, amount), getScalaAppPage().hasStatusPillXNewMessagesIndicator(String.valueOf(amount)));
    }

    @When("^I tap conversation name from top toolbar in the old scala app$")
    public void ITapConversationDetailsBottom() {
        getScalaAppPage().tapTopToolbarTitle();
    }

    @When("^I tap open menu button on Group info page in the old scala app$")
    public void ITapOpenMenuButton() {
        getScalaAppPage().tapMenuButton();
    }

    @When("^I tap Leave group… button on Group conversation options menu in the old scala app$")
    public void ITapLeaveGroup() {
        getScalaAppPage().tapOnLeaveGroupButton();
    }

    @When("^I tap Clear content… button on Group conversation options menu in the old scala app$")
    public void ITapClearContent() {
        getScalaAppPage().tapOnClearContentButton();
    }

    @When("^I tap LEAVE GROUP button on Confirm overlay page in the old scala app$")
    public void ITapLeaveGroupOverlay() {
        getScalaAppPage().tapOnLeaveGroupButtonOverlay();
    }

    @When("^I tap CLEAR CONTENT button on Confirm overlay page in the old scala app$")
    public void ITapClearContentOverlay() {
        getScalaAppPage().tapOnClearContentButtonOverlay();
    }
}
