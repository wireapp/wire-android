package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.TeamCreationPage;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;

public class TeamCreationPageSteps {

    private final AndroidTestContext context;

    public TeamCreationPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I see Create a Team Page$")
    public void iSeeCreateATeamPage() {
        context.getPage(TeamCreationPage.class).isCreateATeamPageVisible();
    }

    @When("^I see Team Created success Page$")
    public void iSeeTeamCreatedSuccessPage() {
        context.getPage(TeamCreationPage.class).isCreateATeamSuccessPageVisible();
    }

    @When("^I see text \"(.*)\" on Create a Team Page$")
    public void iTextOnCreateATeamPage(String text) {
        context.getPage(TeamCreationPage.class).isTextVisible(text);
    }

    @When("^I see text \"(.*)\" on Team Created Success Page$")
    public void iTextOnCreateATeamSuccessPage(String text) {
        context.getPage(TeamCreationPage.class).isTextVisible(text);
    }

    @When("^I see link \"(.*)\" on Create a Team Page$")
    public void iLinkOnCreateATeamPage(String text) {
        context.getPage(TeamCreationPage.class).isTextVisible(text);
    }

    @When("^I enter my email (.*) on Create a Team Page$")
    public void iEnterEmail(String email) {
        ClientUser userToRegister = context.getUsersManager().findUserByEmailOrEmailAlias(email);
        email = userToRegister.getEmail();
        userToRegister.setEmail(email);
        context.getPage(TeamCreationPage.class).enterEmailCredentials(email);
    }

    @When("^I enter my invalid email (.*) on Create a Team Page$")
    public void iEnterInvalidEmail(String email) {
        context.getPage(TeamCreationPage.class).enterEmailCredentials(email);
    }

    @When("^I clear the email input field on Create a Team Page$")
    public void iClearTextInputField() {
        context.getPage(TeamCreationPage.class).clearEmailInputField();
    }

    @When("^I see an error informing me that \"(.*)\" on Create a Team Page$")
    public void iSeeInvalidEmailError(String error) {
        assertThat("Error is not visible.", context.getPage(TeamCreationPage.class).isTextVisible(error));
    }

    @When("^I tap continue button on Create a Team Page$")
    public void iTapContinue() {
        context.getPage(TeamCreationPage.class).tapContinue();
    }

    @When("^I tap on Login Link on Create a Team Page$")
    public void iTapLoginLink() {
        context.getPage(TeamCreationPage.class).tapLoginLink();
    }

    @When("^I see Terms of Use alert on Create a Team Page$")
    public void iSeeTOUAlertOnCreateATeamPage() {
        context.getPage(TeamCreationPage.class).isToUAlertVisible();
        context.getPage(TeamCreationPage.class).isToUInfoTextVisible();
        context.getPage(TeamCreationPage.class).isToUCancelButtonVisible();
        context.getPage(TeamCreationPage.class).isToUViewButtonVisible();
        context.getPage(TeamCreationPage.class).isToUContinueButtonVisible();
    }

    @When("^I tap continue button on Terms of Use alert on Create a Team Page$")
    public void iTapContinueOnToUAlert() {
        context.getPage(TeamCreationPage.class).tapContinueOnToUAlert();
    }

    @When("^I enter my first name (.*) on Create a Team Page$")
    public void iEnterFirstName(String name) {
        ClientUser userToRegister = context.getUsersManager().findUserByNameOrNameAlias(name);
        name = userToRegister.getFirstName();
        context.getPage(TeamCreationPage.class).enterFirstNameCredentials(name);
    }

    @When("^I enter my last name (.*) on Create a Team Page$")
    public void iEnterLastName(String name) {
        ClientUser userToRegister = context.getUsersManager().findUserByNameOrNameAlias(name);
        name = userToRegister.getLastName();
        context.getPage(TeamCreationPage.class).enterLastNameCredentials(name);
    }

    @When("^I enter my team name (.*) on Create a Team Page$")
    public void iEnterTeamName(String name) {
        context.getPage(TeamCreationPage.class).enterTeamNameCredentials(name);
    }

    @When("^I enter my password (.*) on Create a Team Page$")
    public void iEnterPassword(String password) {
        ClientUser userToRegister = context.getUsersManager().findUserByPasswordAlias(password);
        password = userToRegister.getPassword();
        context.getPage(TeamCreationPage.class).enterPasswordCredentials(password);
    }

    @When("^I enter my invalid password (.*) on Create a Team Page$")
    public void iEnterInvalidPassword(String password) {
        context.getPage(TeamCreationPage.class).enterPasswordCredentials(password);
    }

    @When("^I clear the password field on Create a Team Page$")
    public void iClearPassword() {
        context.getPage(TeamCreationPage.class).clearPasswordCredentials();
    }

    @When("^I enter my confirm password (.*) on Create a Team Page$")
    public void iEnterConfirmPassword(String password) {
        ClientUser userToRegister = context.getUsersManager().findUserByPasswordAlias(password);
        password = userToRegister.getPassword();
        context.getPage(TeamCreationPage.class).enterConfirmPasswordCredentials(password);
    }

    @When("^I enter my invalid confirm password (.*) on Create a Team Page$")
    public void iEnterInvalidConfirmPassword(String password) {
        context.getPage(TeamCreationPage.class).enterConfirmPasswordCredentials(password);
    }

    @When("^I clear the confirm password field on Create a Team Page$")
    public void iClearConfirmPassword() {
        context.getPage(TeamCreationPage.class).clearConfirmPasswordCredentials();
    }

    @When("^I tap show password icon on Create a Team Page$")
    public void iTapShowPasswordIcon() {
        context.getPage(TeamCreationPage.class).tapShowPasswordIcon();
    }

    @When("^I tap hide password icon on Create a Team Page$")
    public void iTapHidePasswordIcon() {
        context.getPage(TeamCreationPage.class).tapHidePasswordIcon();
    }

    @When("^I type the verification code for user (.*) on Create a Team Page$")
    public void iTypeVerificationCode(String userNameAlias) {
        ClientUser userToRegister = context.getUsersManager().findUserByNameOrNameAlias(userNameAlias);
        String code = BackendConnections.get(userToRegister).getActivationCodeForEmail(userToRegister.getEmail());
        assertThat("Page 'You've got mail' is not visible",
                context.getPage(TeamCreationPage.class).isVerifyCodePageVisible());
        context.getPage(TeamCreationPage.class).inputVerificationCode(code);
    }

    @When("^I tap on resend code link$")
    public void iTapResendCodeLink() {
        context.getPage(TeamCreationPage.class).tapResendCodeLink();
    }

    @When("^I clear the code input field$")
    public void iClearCodeInputField() {
        context.getPage(TeamCreationPage.class).clearCodeInputField();
    }

    @When("^I type the invalid verification code (.*) on Create a Team Page$")
    public void iTypeInvalidVerificationCode(String code) {
        context.getPage(TeamCreationPage.class).inputVerificationCode(code);
    }

    @When("^I tap Get Started button on Team Created Page$")
    public void iTapGetStartedButton() {
        context.getPage(TeamCreationPage.class).tapGetStarted();
    }

    @When("I verify team member (.*) has role (Owner|Admin|Member)$")
    public void IAmTeamOwner(String userName, String role) {
        assertThat(String.format("User %s does not have role %s", userName, role),
                context.getCommonSteps().getTeamRole(userName).toString(),
                equalTo(role.toUpperCase()));
    }

    @When("^I scroll to the (bottom|top) of team creation page$")
    public void IScrollToTheBottom(String direction) {
        if (direction.equalsIgnoreCase("bottom")) {
            context.getPage(TeamCreationPage.class).scrollToTheBottom();
        } else {
            context.getPage(TeamCreationPage.class).scrollToTheTop();
        }
    }
}
