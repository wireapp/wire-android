package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ArchiveListPage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class ArchiveListPageSteps {

    private final AndroidTestContext context;

    public ArchiveListPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private ArchiveListPage getArchiveListPage() {
        return context.getPage(ArchiveListPage.class);
    }

    @Then("^I see conversation \"(.*)\" in archive list$")
    public void iSeeConversationOnArchiveList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation is not visible in the list.", getArchiveListPage().isArchivedConversationDisplayed(userName));
    }

    @Then("^I do not see conversation \"(.*)\" in archive list$")
    public void iDoNotSeeConversationOnArchiveList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The conversation is still visible in the list.", getArchiveListPage().isArchivedConversationInvisible(userName));
    }

    @When("^I tap on conversation name \"(.*)\" in archive list$")
    public void iTapOnConversationName(String conversationName) {
        conversationName = context.getUsersManager().replaceAliasesOccurrences(conversationName, ClientUsersManager.FindBy.NAME_ALIAS);
        getArchiveListPage().tapArchivedConversationName(conversationName);
    }

    @When("^I long tap on conversation name \"(.*)\" in archive list$")
    public void iLongTapOnConversationName(String conversationName) {
        conversationName = context.getUsersManager().replaceAliasesOccurrences(conversationName, ClientUsersManager.FindBy.NAME_ALIAS);
        getArchiveListPage().longTapArchivedConversationName(conversationName);
    }

    @When("^I tap move out of archive button$")
    public void iTapMoveOutOfArchiveButton() {
        getArchiveListPage().tapMoveOutOfArchiveButton();
    }

    @When("^I see \"(.*)\" toast message on archive list$")
    public void iSeeToastMessageConversationList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Toast message is not visible.", getArchiveListPage().isToastMessageDisplayed(user));
    }
}
