package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.WelcomePage;
import com.wearezeta.auto.common.CommonSteps;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;

public class WelcomePageSteps {

    private final AndroidTestContext context;

    public WelcomePageSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I see Welcome Page$")
    public void iSeeWelcomePage() {
        context.getPage(WelcomePage.class).isWelcomePageVisible();
    }

    @When("^I see alert informing me that I am about to switch to (.*) backend$")
    public void iSeeBackendAlert(String backend) {
        assertThat("Alert does not show correct backend.", context.getPage(WelcomePage.class).isTextDisplayed(backend));
    }

    @When("^I tap proceed button on custom backend alert$")
    public void iTapProceedButton() {
        context.getPage(WelcomePage.class).tapProceedButton();
        // On some devices screen dims during initialisation. Therefore, tap on proceed button enables the screen again only.
        if (context.getPage(WelcomePage.class).isProceedButtonVisible()) {
            context.getPage(WelcomePage.class).tapProceedButton();
        }
    }

    @When("^I tap login button on Welcome Page$")
    public void iTapLoginButton() {
        context.getPage(WelcomePage.class).tapLoginButton();
    }

    @When("^I tap Create a Team button on Welcome Page$")
    public void iTapCreateATeamButton() {
        context.getPage(WelcomePage.class).tapCreateATeamButton();
    }

    @When("^I do not see Create a Team button on Welcome Page$")
    public void iDoNotSeeCreateATeamButton() {
        assertThat("Create a team button is visible.", context.getPage(WelcomePage.class).isCreateATeamButtonInvisible());
    }

    @When("^I tap Create a Personal Account link on Welcome Page$")
    public void iTapCreateAPersonalAccountButton() {
        context.getPage(WelcomePage.class).tapCreateAPersonalAccountLink();
    }

    @When("^I do not see Create a Personal Account link on Welcome Page$")
    public void iDoNotSeeCreateAPersonalAccountButton() {
        assertThat("Create personal account link is visible.", context.getPage(WelcomePage.class).isCreateAPersonalAccountLinkInvisible());
    }
}
