package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.SelfUserProfilePage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

public class SelfUserProfilePageSteps {

    private final AndroidTestContext context;

    private SelfUserProfilePage getSelfUserProfilePage() {
        return context.getPage(SelfUserProfilePage.class);
    }

    public SelfUserProfilePageSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I see User Profile Page$")
    public void iSeeUserProfilePage() {
        assertThat("User Profile Page is not visible.", getSelfUserProfilePage().isUserProfilePageVisible());
    }

    @When("^I see User Profile Page for account (.*) as my currently active account$")
    public void iSeeUserProfilePageForAccount(String account) {
        account = context.getUsersManager().replaceAliasesOccurrences(account, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("User Profile Page for account is not visible.", getSelfUserProfilePage().isCurrentAccountActive(account));
    }

    @When("^I see change status options$")
    public void iSeeStatusOptions() {
        assertThat("Change Status options are not visible.", getSelfUserProfilePage().iSeeChangeStatusOptions());
    }

    @When("^I see my status is set to \"(.*)\" on User Profile Page$")
    public void iSeeMyStatus(String status) {
        assertThat("Displayed status is not correct.", getSelfUserProfilePage().iSeeCurrentStatus(status));
    }

    @When("^I change my status to available on User Profile Page$")
    public void iChangeStatusToAvailable() {
        getSelfUserProfilePage().changeMyStatusToAvailable();
    }

    @When("^I change my status to busy on User Profile Page$")
    public void iChangeStatusToBusy() {
        getSelfUserProfilePage().changeMyStatusToBusy();
    }

    @When("^I change my status to away on User Profile Page$")
    public void iChangeStatusToAway() {
        getSelfUserProfilePage().changeMyStatusToAway();
    }

    @When("^I change my status from busy to none on User Profile Page$")
    public void iChangeStatusToNone() {
        getSelfUserProfilePage().changeMyStatusFromBusyToNone();
    }

    @When("^I see text \"(.*)\" informing me about my status change$")
    public void iSeeStatusChangeInfoText(String text) {
        assertThat("Text is not displayed.", getSelfUserProfilePage().getTextAlert(), equalTo(text));
    }

    @When("^I see my other account (.*) is listed under other logged in accounts$")
    public void iSeeMyOtherAccount(String account) {
        account = context.getUsersManager().replaceAliasesOccurrences(account, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("The account is not visible in the list.", getSelfUserProfilePage().iSeeMyOtherAccount(account));
    }

    @When("^I don't see any other accounts logged in$")
    public void iDoNotSeeOtherAccounts() {
        getSelfUserProfilePage().isOtherAccountsSectionInvisible();
    }

    @When("^I switch to (.*) account$")
    public void iSwitchAccount(String account) {
        account = context.getUsersManager().replaceAliasesOccurrences(account, ClientUsersManager.FindBy.NAME_ALIAS);
        getSelfUserProfilePage().tapAccount(account);
    }

    @When("^I tap New Team or Account button$")
        public void iTapNewTeamOrAccount() {
        getSelfUserProfilePage().tapNewTeamOrAccountButton();
    }

    @When("^I tap log out button on User Profile Page$")
    public void iTapLogoutButton() {
        getSelfUserProfilePage().iTapLogoutButton();
    }

    @When("^I see alert informing me that I can not login with another account$")
    public void iSeeNotPossibleToAddAnotherAccountAlert() {
        assertThat("Alert is not visible.", getSelfUserProfilePage().isTooManyAccountsAlertVisible());
    }

    @When("^I see alert informing me that I am about to clear my data when I log out$")
    public void iSeeClearDataAlert() {
        assertThat("Alert is not visible.", getSelfUserProfilePage().isClearDataAlertVisible());
    }

    @When("^I see option to \"(.*)\" when I will log out$")
    public void clearDataInfoText(String text) {
        assertThat("Text is not displayed.", getSelfUserProfilePage().getTextLogoutAlert(), equalTo(text));
    }

    @When("^I select checkbox to clear my data$")
    public void clearData() {
        getSelfUserProfilePage().selectClearData();
    }

    @When("^I tap log out button on clear data alert$")
    public void iTapLogoutButtonOnClearDataAlert() {
        context.getPage(SelfUserProfilePage.class).iTapLogoutButton();
    }

    @When("^I tap close button on User Profile Page$")
    public void iTapCloseButtonUserProfilePage() {
        getSelfUserProfilePage().tapCloseButton();
    }

    @When("^I see Legal hold is pending message on my user profile$")
    public void iSeeLegalHoldPending() {
        assertThat("Legal hold pending message is not visible.", getSelfUserProfilePage().isLegalHoldPendingVisible());
    }

    @When("^I tap accept button for legal hold pending request$")
    public void iTapAcceptButtonLegalHoldPending() {
        getSelfUserProfilePage().tapAcceptButtonLegalHoldPending();
    }
}