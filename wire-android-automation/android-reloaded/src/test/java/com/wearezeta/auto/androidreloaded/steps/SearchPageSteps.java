package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.SearchPage;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.util.List;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;

public class SearchPageSteps {

    private final AndroidTestContext context;

    public SearchPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private SearchPage getSearchPage() {
        return context.getPage(SearchPage.class);
    }

    private static final Logger log = ZetaLogger.getLog(SearchPageSteps.class.getSimpleName());

    @When("^I clear the input field on Search page$")
    public void iClearInputField() {
        getSearchPage().clearTextInSearch();
    }

    // Empty search section

    @Then("^I see search hint \"(.*)\" on search page$")
    public void iSeeSearchHint(String text) {
        assertThat(String.format("The hint '%s' should not be displayed", text), getSearchPage().isSearchHintVisible(text));
    }

    @Then("^I do not see search hint \"(.*)\" on search page$")
    public void iDoNotSeeSearchHint(String text) {
        assertThat(String.format("The hint '%s' should be displayed", text), getSearchPage().isSearchHintInvisible(text));
    }

    @Then("^I see Learn More link on empty search page$")
    public void iSeeLinkOnSearchPage() {
        assertThat("The button '%s' should not be displayed", getSearchPage().isLearnMoreLinkVisible());
    }

    @Then("^I do not see Learn More link on empty search page$")
    public void iDoNotSeeLinkOnSearchPage() {
        assertThat("The button '%s' should be displayed", getSearchPage().isLearnMoreLinkInvisible());
    }

    // Searching with different values

    @When("^I type user name \"(.*)\" in search field")
    public void iTypeUserNameInSearchField(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        getSearchPage().sendKeysUserNameSearchField(userName);
    }

    @When("^I type unique user name \"(.*)\" in search field")
    public void iTypeUniqueUserNameInSearchField(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        getSearchPage().sendKeysUserNameSearchField(userName);
    }

    @When("^I search user (.*) by handle and domain in Search UI input field$")
    public void iSearchByHandleAndDomain(String userName) {
        ClientUser user = context.getUsersManager().findUserByNameOrNameAlias(userName);
        String domainName = BackendConnections.get(user).getDomain();
        getSearchPage().sendKeysUserNameSearchField(user.getUniqueUsername() + "@" + domainName);
    }

    @When("^I type the first (\\d+) chars? of user name \"(.*)\" in search field$")
    public void iTypeWordInSearchField(int size, String text) {
        text = context.getUsersManager().replaceAliasesOccurrences(text, ClientUsersManager.FindBy.NAME_ALIAS);
        int length = text.length();
        text = (size < length) ? text.substring(0, size) : text;
        getSearchPage().sendKeysUserNameSearchField(text);
    }

    @When("^I type (.*) in search field in search only partially$")
    public void iSearchForUserOnlyPartially(String nameOrEmailOrUniqueUsername) {
        nameOrEmailOrUniqueUsername = context.getUsersManager().replaceAliasesOccurrences(nameOrEmailOrUniqueUsername,
                ClientUsersManager.FindBy.NAME_ALIAS, ClientUsersManager.FindBy.EMAIL_ALIAS, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        // adding spaces to ensure trimming of input
        getSearchPage().sendKeysUserNameSearchField(" " + nameOrEmailOrUniqueUsername.substring(0,
                nameOrEmailOrUniqueUsername.length() - 2) + " ");
    }

    @When("^I search user (.*) by exact handle and domain in search field")
    public void iSearchUserByUsernameAndDomain(String nameAlias) {
        ClientUser user = context.getUsersManager().findUserByNameOrNameAlias(nameAlias);
        String domain = BackendConnections.get(user).getDomain();
        getSearchPage().sendKeysUserNameSearchField("@" + user.getUniqueUsername() + "@" + domain);
    }

    @When("^I search user \"(.*)\" by email in search field")
    public void iSearchUserByEmail(String userName) {
        ClientUser user = context.getUsersManager().findUserByNameOrNameAlias(userName);
        getSearchPage().sendKeysUserNameSearchField(user.getEmail());
    }

    // Search Suggestions Section


    @When("^I see user (.*) in search suggestions list$")
    public void iSeeUserInSearchSuggestionsList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Username is not visible in Search suggestion list.", getSearchPage().isUserInSearchSuggestionsListVisible(userName));
    }

    @When("^I do not see user (.*) in search suggestions list$")
    public void iDoNotSeeUserInSearchSuggestionsList(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Username is not visible in Search suggestion list.", getSearchPage().isUserInSearchSuggestionsListInvisible(userName));
    }

    @When("^I select users? (.*) in search suggestions list$")
    public void iSelectUserInSearchSuggestionsList(String userName) {
        List<String> contactNamesList = context.getUsersManager().splitAliases(userName);
        log.info("Selecting " + contactNamesList);
        for (String contactName : contactNamesList) {
            contactName = context.getUsersManager().replaceAliasesOccurrences(contactName.trim(), ClientUsersManager.FindBy.NAME_ALIAS);
            log.info("Selecting in checkbox " + contactName);
            getSearchPage().iSelectUserInSearchSuggestionsList(contactName);
        }
    }

    // Search Result section

    @When("^I see user name \"(.*)\" in Search result list$")
    public void iSeeUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Username is not visible in Search result list.", getSearchPage().isUserNameSearchResultVisible(userName));
    }

    @When("^I do not see user name \"(.*)\" in Search result list$")
    public void iDoNotSeeUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Username is visible in Search result list.", getSearchPage().isUserNameSearchResultInvisible(userName));
    }

    @When("^I see unique user name \"(.*)\" in Search result list$")
    public void iSeeUniqueUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        assertThat("Unique Username is not visible in search result list.", getSearchPage().isUserNameSearchResultVisible(userName));
    }

    @When("^I tap on user name \"(.*)\" in Search result list$")
    public void iTapUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        getSearchPage().tapUserNameSearchResult(userName);
    }

    @When("^I select unique user name \"(.*)\" in Search result list$")
    public void iSelectUserUniqueUserNameInSearchSuggestionsList(String userName) {
        List<String> contactNamesList = context.getUsersManager().splitAliases(userName);
        log.info("Selecting " + contactNamesList);
        for (String contactName : contactNamesList) {
            contactName = context.getUsersManager().replaceAliasesOccurrences(contactName.trim(), ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
            log.info("Selecting in checkbox " + contactName);
            getSearchPage().iSelectUserInSearchSuggestionsList(contactName);
        }
    }

    // Search Result section - Federation

    @When("^I see federated guest label for user name \"(.*)\" in Search result list$")
    public void iSeeFederatedGuestLabelForUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Federated Label for User is not visible.", getSearchPage().isFederatedLabelForUserVisible(userName));
    }

    @When("^I do not see federated guest label for user name \"(.*)\" in Search result list$")
    public void iDoNotSeeFederatedGuestLabelForUserNameSearchResult(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Federated Label for User is visible, but should not.", getSearchPage().isFederatedLabelForUserInvisible(userName));
    }

    @Then("^I see domain (.*) in subtitle for (.*)$")
    public void iSeeDomainInSearchResult(String domain, String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("The contact '%s' does not have the correct domain '%s'", name, domain), getSearchPage().isDomainInSubtitleVisible(domain));
    }

    @Then("^I do not see domain (.*) in subtitle for (.*)$")
    public void iDoNotSeeDomainInSearchResult(String domain, String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("The contact '%s' should not have the domain '%s'", name, domain), getSearchPage().isDomainInSubtitleInvisible(domain));
    }

    // Federation End

    @When("^I tap on user name (.*) found on search page$")
    public void ITapOnUserNameFoundOnSearchPage(String username) {
        username = context.getUsersManager().replaceAliasesOccurrences(username, ClientUsersManager.FindBy.NAME_ALIAS);
        getSearchPage().tapOnUserName(username);
    }

    @When("^I tap create new group button$")
    public void iTapOnCreateNewGroupButton() {
        getSearchPage().tapCreateNewGroupButton();
    }

    @When("^I do not see create new group button$")
    public void iDoNotSeeCreateNewGroupButton() {
        assertThat("Create group button is visible.", getSearchPage().isNewGroupButtonInvisible());
    }

    @When("^I tap Continue button on add participants page$")
    public void iTapContinue() {
        getSearchPage().tapContinue();
    }

    @When("^I tap Add To Group button on add participants page$")
    public void iTapAddToGroup() {
        getSearchPage().tapAddToGroup();
    }

    @When("^I close the search page through X icon$")
    public void iCloseSearchPage() {
        getSearchPage().closeSearchPage();
    }

    @When("^I close the search results page through X icon$")
    public void iCloseSearchResultPage() {
        getSearchPage().closeSearchResultPage();
    }
}
