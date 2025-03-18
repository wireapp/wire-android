package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.SSOPage;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class SSOPageSteps {

    AndroidTestContext context;

    public SSOPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private SSOPage getSSOPage() {
        return context.getPage(SSOPage.class);
    }

    private static final Logger log = ZetaLogger.getLog(SSOPageSteps.class.getSimpleName());

    @When("^I type the default SSO code on SSO Login Tab$")
    public void iTypeSSOCodeOnSSOLoginTab() {
        getSSOPage().inputSsoCode(context.getCommonSteps().getSSOCode());
    }

    @And("^I type email to redirect to QA-Fixed-SSO backend on Enterprise Login popup$")
    public void iTypeEmailForQAFixedSSOOnEnterpriseLoginPopup() {
        String domain = context.getCommonSteps().addDomainForQAFixedSSOToStaging();
        getSSOPage().inputSsoCode("joe@" + domain);
        log.info("SSO code: " + "joe@" + domain);
    }

    @When("^I type an invalid SSO code (.*) on SSO Login Tab$")
    public void iTypeInvalidSSOCodeOnSSOLoginTab(String invalidCode) {
        getSSOPage().inputSsoCode(invalidCode);
    }

    @When("^I see error message \"(.*)\" underneath SSO code input field$")
    public void iSeeErrorMessage(String message) {
        assertThat("SSO Error is not visible.", getSSOPage().getTextErrorSSO(), equalTo(message));
    }

    @When("^I clear the SSO Code input field$")
    public void iClearSSOCodeInputField() {
        getSSOPage().clearInputField();
    }

    @When("^I sign in with my credentials on Okta Page$")
    public void iInputCredentials() {
        assertThat("SSO page is not visible.", getSSOPage().waitUntilOktaSignInPageVisible());
        final ClientUser self = context.getUsersManager().getSelfUserOrThrowError();
        getSSOPage().inputUsername(self.getEmail());
        log.info("Email: " + self.getEmail());
        getSSOPage().inputPassword(self.getPassword());
        log.info("Password: " + self.getPassword());
    }

    @When("^I sign in with invalid email \"(.*)\" credentials on Okta Page$")
    public void iSignInWithInvalidEmailCredentials(String email) {
        assertThat("SSO page is not visible.", getSSOPage().waitUntilOktaSignInPageVisible());
        getSSOPage().inputUsername(email);
    }

    @When("^I sign in with invalid password \"(.*)\" credentials on Okta Page$")
    public void iSignInWithInvalidPasswordCredentials(String password) {
        getSSOPage().inputPassword(password);
    }

    @When("^I see error message telling me that I am unable to sign in on Okta Page$")
    public void iSeeOktaError() {
        assertThat("Okta Error is not visible although it should be.", getSSOPage().isOktaErrorVisible());
    }

    @When("^I tap login button on Okta Page$")
    public void iTapLoginButtonOnOktaPage() {
        getSSOPage().tapSignIn();
    }
}
