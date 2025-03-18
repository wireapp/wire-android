package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.LoginPage;
import com.wearezeta.auto.androidreloaded.pages.RegistrationPage;
import com.wearezeta.auto.androidreloaded.pages.TeamCreationPage;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import com.wearezeta.auto.common.usrmgmt.NoSuchUserException;
import io.cucumber.java.en.When;

import java.util.logging.Logger;
import static org.hamcrest.MatcherAssert.assertThat;

public class RegistrationPageSteps {

    private final AndroidTestContext context;

    private static final Logger log = ZetaLogger.getLog(RegistrationPageSteps.class.getSimpleName());

    public RegistrationPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private RegistrationPage getRegistrationPage() {
        return context.getPage(RegistrationPage.class);
    }

    @When("^I see Create a Personal Account Page$")
    public void iSeeCreatePersonalAccountPage() {
        getRegistrationPage().isCreateAPersonalAccountHeadingVisible();
    }

    @When("^I see Username Page$")
    public void iSeeUserNamePage() {
        getRegistrationPage().isUserNamePageHeadingVisible();
    }

    @When("^I see Personal Account Created Success Page$")
    public void iSeePersonalAccountCreatedSuccessPage() {
        getRegistrationPage().isCreateAPersonalAccountSuccessPageVisible();
    }

    @When("^I see text \"(.*)\" on Personal Account Created Success Page$")
    public void iTextOnCreateATeamSuccessPage(String text) {
        getRegistrationPage().isTextVisible(text);
    }

    @When("^I see help text underneath Username input field$")
    public void iSeeHelpText() {
        getRegistrationPage().isUserNameHelpTextVisible();
    }

    @When("^I see text \"(.*)\" on Username Page$")
    public void iSeeInfoText(String text) {
        getRegistrationPage().isTextVisible(text);
    }

    @When("^I enter my email (.*) on Create a Personal Account Page$")
    public void iEnterEmail(String email) {
        ClientUser userToRegister = context.getUsersManager().findUserByEmailOrEmailAlias(email);
        email = userToRegister.getEmail();
        getRegistrationPage().enterEmailCredentials(email);
    }

    @When("^I submit my Username (.*) on registration page$")
    public void iSubmitUserName(String name) {
        if (name.contains("Unique")) {
            name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.UNIQUE_USERNAME_ALIAS);
        } else {
            name = context.getUsersManager().replaceAliasesOccurrences(name, ClientUsersManager.FindBy.NAME_ALIAS);
        }
        getRegistrationPage().enterUserName(name);
    }

    @When("^I see Terms of Use alert on Create a Personal Account Page$")
    public void iSeeTOUAlertOnPersonalAccountPage() {
        getRegistrationPage().isToUAlertVisible();
        getRegistrationPage().isToUInfoTextVisible();
        getRegistrationPage().isToUCancelButtonVisible();
        getRegistrationPage().isToUViewButtonVisible();
        getRegistrationPage().isToUContinueButtonVisible();
    }

    @When("^I tap continue button on Terms of Use alert on Create a Personal Account Page$")
    public void iTapContinueOnToUAlert() {
        getRegistrationPage().tapContinue();
    }

    @When("^I enter my first name (.*) on Create a Personal Account Page$")
    public void iEnterFirstName(String name) {
        ClientUser userToRegister = context.getUsersManager().findUserByNameOrNameAlias(name);
        name = userToRegister.getFirstName();
        getRegistrationPage().enterFirstNameCredentials(name);
    }

    @When("^I enter my last name (.*) on Create a Personal Account Page$")
    public void iEnterLastName(String name) {
        ClientUser userToRegister = context.getUsersManager().findUserByNameOrNameAlias(name);
        name = userToRegister.getLastName();
        getRegistrationPage().enterLastNameCredentials(name);
    }

    @When("^I enter my password (.*) on Create a Personal Account Page$")
    public void iEnterPassword(String password) {
        ClientUser userToRegister = context.getUsersManager().findUserByPasswordAlias(password);
        password = userToRegister.getPassword();
        getRegistrationPage().enterPasswordCredentials(password);
    }

    @When("^I enter my confirm password (.*) on Create a Personal Account Page$")
    public void iEnterConfirmPassword(String password) {
        ClientUser userToRegister = context.getUsersManager().findUserByPasswordAlias(password);
        password = userToRegister.getPassword();
        getRegistrationPage().enterConfirmPasswordCredentials(password);
    }

    @When("^I tap show password icon on Create a Personal Account Page$")
    public void iTapShowPasswordIcon() {
        getRegistrationPage().tapShowPasswordIcon();
    }

    @When("^I tap hide password icon on Create a Personal Account Page$")
    public void iTapHidePasswordIcon() {
        getRegistrationPage().tapHidePasswordIcon();
    }

    @When("^I type the verification code for user (.*) on Create a Personal Account Page$")
    public void iTypeVerificationCode(String userNameAlias) {
        ClientUser userToRegister = context.getUsersManager().findUserByNameOrNameAlias(userNameAlias);
        String code = BackendConnections.get(userToRegister).getActivationCodeForEmail(userToRegister.getEmail());
        assertThat("Page 'You've got mail' is not visible",
                getRegistrationPage().isVerifyCodePageVisible());
        getRegistrationPage().inputVerificationCode(code);
    }

    @When("^I tap Get Started button on Personal Account Created Success Page$")
    public void iTapGetStartedButton() {
        getRegistrationPage().tapGetStarted();
    }

    @When("^I see Get Started button on Personal Account Created Success Page$")
    public void IseeGetStartedButton() {
        getRegistrationPage().seeGetStartedButton();
    }

    @When("^I tap confirm button on UserName Page$")
    public void iTapConfirm() {
        getRegistrationPage().tapConfirm();
    }

    @When("^I tap continue button on Create a Personal Account Page$")
    public void iTapContinue() {
        getRegistrationPage().tapContinue();
    }
}
