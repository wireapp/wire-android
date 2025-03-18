package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ConversationListPage;
import com.wearezeta.auto.androidreloaded.pages.LoginPage;
import com.wearezeta.auto.common.backend.BackendConnections;
import com.wearezeta.auto.common.credentials.Credentials;
import com.wearezeta.auto.common.email.MailboxProvider;
import com.wearezeta.auto.common.email.MessagingUtils;
import com.wearezeta.auto.common.email.handlers.ISupportsMessagesPolling;
import com.wearezeta.auto.common.email.messages.ActivationMessage;
import com.wearezeta.auto.common.misc.Timedelta;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import io.cucumber.java.en.When;
import org.joda.time.DateTime;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class LoginPageSteps {

    private final AndroidTestContext context;

    public LoginPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private LoginPage getLoginPage() {
        return context.getPage(LoginPage.class);
    }

    @When("^I see Login Page$")
    public void iSeeWelcomePage() {
        getLoginPage().isLoginPageVisible();
    }

    @When("^I tap on Forgot Password Link$")
    public void tapForgotPasswordLink() {
        getLoginPage().tapForgotPasswordLink();
    }

    @When("^I tap on Email login tab$")
    public void iTapEmailLoginTab() {
        getLoginPage().tapEmailLoginTab();
    }

    @When("^I tap on SSO login tab$")
    public void iTapSSOLoginTab() {
        getLoginPage().tapSSOLoginTab();
    }

    @When("^I tap login button on Login Page$")
    public void iTapLoginButton() {
        getLoginPage().tapLoginButton();
    }

    @When("^I clear the email input field$")
    public void iClearEmailField() {
        getLoginPage().clearEmailCredentials();
    }

    @When("^I clear the password input field$")
    public void iClearPasswordField() {
        getLoginPage().clearPasswordCredentials();
    }

    @When("^I sign in using my email$")
    public void iSignInUsingMyEmail() {
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getLoginPage().enterEmailCredentials(self.getEmail());
        getLoginPage().enterPassword(self.getPassword());
    }

    @When("^I enter credentials for SOCKS proxy$")
    public void iEnterProxyCredentials() {
        getLoginPage().hideKeyboard();
        getLoginPage().enterProxyUsername("qa");
        getLoginPage().hideKeyboard();
        getLoginPage().enterProxyPassword(Credentials.get("SOCKS_PROXY_PASSWORD"));
    }

    @When("^I sign in using my known email and password$")
    public void iSignInUsingMyKnownEmail() {
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getLoginPage().enterEmailCredentials(self.getEmail());
        getLoginPage().enterPassword(self.getPassword());
    }

    @When("^I sign in using my username$")
    public void iSignInUsingMyUsername() {
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getLoginPage().enterEmailCredentials(self.getUniqueUsername());
        getLoginPage().enterPassword(self.getPassword());
    }

    @When("^I sign in using my email and invalid password \"(.*)\"$")
    public void iSignInUsingMyEmailAndWrongPassword(String password) {
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getLoginPage().enterEmailCredentials(self.getEmail());
        getLoginPage().enterPassword(password);
    }

    @When("^I sign in using my invalid email \"(.*)\" and password$")
    public void iSignInUsingMyWrongEmailAndPassword(String email) {
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getLoginPage().enterEmailCredentials(email);
        getLoginPage().enterPassword(self.getPassword());
    }

    @When("^I tap login button on email Login Page$")
    public void itapLoginButton() {
        getLoginPage().tapLoginButton();
    }

    @When("^I tap show password icon$")
    public void iTapShowPasswordIcon() {
        getLoginPage().tapShowPasswordIcon();
    }

    @When("^I tap hide password icon$")
    public void iTapHidePasswordIcon() {
        getLoginPage().tapHidePasswordIcon();
    }

    @When("^I see my password \"(.*)\" in cleartext$")
    public void iSeePasswordInClearText(String password) {
        password = context.getUsersManager().findUserByPasswordAlias(password).getPassword();
        assertThat("Password is not visible in cleartext.", getLoginPage().getTextPasswordInputField(), containsString(password));
    }

    @When("^I do not see my password \"(.*)\" in cleartext$")
    public void iDoNotSeePasswordInClearText(String password) {
        assertThat("Password is visible in cleartext.", getLoginPage().getTextPasswordInputField(), not(containsString(password)));
    }

    @When("^I see an error informing me that \"(.*)\" on login page$")
    public void iSeeInvalidEmailError(String error) {
        assertThat("Error is not visible.", getLoginPage().isTextVisible(error));
    }

    @When("^I see invalid information alert$")
    public void iSeeInvalidInformationAlert() {
        assertThat("Invalid Information Alert is not visible.", getLoginPage().isInvalidInformationAlertVisible());
    }

    @When("^I see text \"(.*)\" informing me that I used incorrect credentials$")
    public void iSeeIncorrectCredentialsText(String text) {
        assertThat("Text is not displayed.", getLoginPage().getTextAlert(), equalTo(text));
    }

    @When("^I tap OK button on the alert on Login Page$")
    public void iTapOKButton() {
        getLoginPage().tapOKButton();
    }

    @When("^I wait until I am logged in from okta page$")
    public void iWaitUntilIAmLoggedIn() {
        getLoginPage().waitUntilLoginButtonIsInvisible();
        context.getPage(LoginPage.class).waitUntilLoginButtonIsInvisibleAfterTap();
        assertThat("Setting up Wire did not succeed." , context.getPage(ConversationListPage.class).waitUntilSetupIsDone());
    }

    @When("^I( now)? start 2FA verification email monitoring for (.*)$")
    public void iStart2FAVerificationEmailMonitoring(String fromNow, String emailAlias) throws Exception {
        Timedelta rejectMessagesBefore = Timedelta.ofMillis(DateTime.now().toDate().getTime());
        ClientUser user = context.getUsersManager().findUserByEmailOrEmailAlias(emailAlias);
        Map<String, String> expectedHeaders = new HashMap<>();
        expectedHeaders.put("X-Zeta-Purpose", "SecondFactorVerification");
        ISupportsMessagesPolling mbox = MailboxProvider.getInstance(BackendConnections.get(user), user.getEmail());
        if (fromNow == null) {
            context.setRememberedMail(mbox.getMessage(expectedHeaders, ActivationMessage.ACTIVATION_TIMEOUT));
        } else {
            context.setRememberedMail(mbox.getMessage(expectedHeaders, ActivationMessage.ACTIVATION_TIMEOUT, rejectMessagesBefore));
        }
    }

    @When("^I type 2FA verification code from email$")
    public void iTypeVerificationCode() throws Exception {
        if (context.getRememberedMail() == null) {
            throw new RuntimeException("No email remembered. Maybe you forgot the step to monitor for email?");
        }
        final String emailContent = context.getRememberedMail().get();
        final String verificationCode = MessagingUtils.get2FAVerificationCode(emailContent);
        final String emailAddress = MessagingUtils.getRecipientEmailFromHeader(emailContent);
        ClientUser user = context.getUsersManager().findUserByEmailOrEmailAlias(emailAddress);
        user.setVerificationCode(verificationCode);
        getLoginPage().inputVerificationCode(user.getVerificationCode());
    }
}
