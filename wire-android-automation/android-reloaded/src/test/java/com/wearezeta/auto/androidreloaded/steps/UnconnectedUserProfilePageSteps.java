package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.UnconnectedUserProfilePage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class UnconnectedUserProfilePageSteps {

    private final AndroidTestContext context;

    public UnconnectedUserProfilePageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private UnconnectedUserProfilePage getUnconnectedUserProfilePage() {
        return context.getPage(UnconnectedUserProfilePage.class);
    }

    @Then("^I see unconnected user (.*) profile$")
    public void iSeeUnconnectedUserProfile(String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("%s should be visible", name), getUnconnectedUserProfilePage().isUserProfileVisible(name));
    }

    @Then("^I see username \"(.*)\" on unconnected user profile page$")
    public void iSeeUserNameContactOnSingleUnconnectedUserDetailsPage(String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat(String.format("%s should be visible", name), getUnconnectedUserProfilePage().isUserNameVisible(name));
    }

    @Then("^I see fully qualified username \"(.*)\" on unconnected user profile page$")
    public void iSeeFullyQualifiedUserNameContactOnSingleUnconnectedUserDetailsPage(String name) {
        name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        assertThat(String.format("%s is not displayed", name), getUnconnectedUserProfilePage().isFullyQualifiedUserNameVisible(name));
    }

    // Connecting

    @When("^I tap connect button on unconnected user profile page$")
    public void iTapConnectButton() {
        getUnconnectedUserProfilePage().tapConnectButton();
    }

    @When("^I see text \"(.*)\" on unconnected user profile page$")
    public void iSeeTextOnUnconnectedUserProfilePage(String text) {
        getUnconnectedUserProfilePage().isConnectionTextVisible(text);
    }

    @When("^I see accept button on unconnected user profile page$")
    public void iSeeAcceptButton() {
        getUnconnectedUserProfilePage().isAcceptButtonVisible();
    }

    @When("^I tap accept button on unconnected user profile page$")
    public void iTapAcceptButton() {
        getUnconnectedUserProfilePage().tapAcceptButton();
    }

    @When("^I see ignore button on unconnected user profile page$")
    public void iSeeIgnoreButton() {
        getUnconnectedUserProfilePage().isIgnoreButtonVisible();
    }

    @When("^I tap ignore button on unconnected user profile page$")
    public void iTapIgnoreButton() {
        getUnconnectedUserProfilePage().tapIgnoreButton();
    }

    @When("^I wait until cancel connection request button on unconnected user profile page is visible$")
    public void iSeeCancelButton() {
        getUnconnectedUserProfilePage().waitUntilCancelConnectionRequestButtonVisible();
    }

    @When("^I tap cancel connection request button on unconnected user profile page$")
    public void iTapCancelButton() {
        getUnconnectedUserProfilePage().tapCancelConnectionRequestButton();
    }

    // Federation

    @Then("^I see federated guest icon for user \"(.*)\" on unconnected user profile page$")
    public void iSeeFederatedGuestLabelForUser(String user) {
        assertThat(String.format("Federated Guest label for user '%s' is not visible on user details page.", user), getUnconnectedUserProfilePage().isFederatedLabelVisible());
    }

    @Then("^I do not see federated guest icon for user \"(.*)\" on unconnected user profile page$")
    public void iDoNotSeeFederatedGuestLabelForUser(String user) {
        assertThat(String.format("Federated Guest label for user '%s' is not visible on user details page.", user), getUnconnectedUserProfilePage().isFederatedLabelInvisible());
    }

    @Then("^I see guest icon for user \"(.*)\" on unconnected user profile page$")
    public void iSeeGuestLabelForUser(String user) {
        assertThat(String.format("Federated Guest label for user '%s' is not visible on user details page.", user), getUnconnectedUserProfilePage().isGuestLabelVisible());
    }

    @Then("^I see classified domain label with text \"(.*)\" on unconnected user profile page$")
    public void iSeeClassifiedBannerTextForUser(String text) {
        assertThat("Classified banner does not have correct text on user details page.", getUnconnectedUserProfilePage().isTextDisplayed(text));
    }

    @Then("^I do not see classified domain label with text \"(.*)\" on unconnected user profile page$")
    public void iDoNotSeeClassifiedBannerTextForUser(String text) {
        assertThat("Classified banner does not have correct text on user details page.", getUnconnectedUserProfilePage().isTextInvisible(text));
    }

    // Federation End

    @When("^I tap close button on unconnected user profile page$")
    public void iTapCloseButton() {
        getUnconnectedUserProfilePage().tapCloseButton();
    }
}
