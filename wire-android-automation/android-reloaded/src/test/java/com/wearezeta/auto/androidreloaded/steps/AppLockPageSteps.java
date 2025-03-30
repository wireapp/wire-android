package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.AppLockPage;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class AppLockPageSteps {

    private final AndroidTestContext context;

    public AppLockPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private AppLockPage getAppLockPage() {
        return context.getPage(AppLockPage.class);
    }

    @When("^I see set up app lock page$")
    public void iSeeSetUpAppLockPage() {
       assertThat("Set up app lock page is not displayed.", getAppLockPage().isSetUpAppLockPageVisible());
    }

    @When("^I see description text that app will lock after 1 minute of inactivity$")
    public void iSeeDescriptionText() {
        assertThat("Description text is not displayed.", getAppLockPage().isDescriptionTextAppLockVisible());
    }

    @When("^I enter my passcode \"(.*)\" for app lock$")
    public void iEnterPassCodeAppLock(String passcode) {
        getAppLockPage().enterPassCode(passcode);
    }

    @When("^I clear the password field on app lock page$")
    public void iClearPasswordField() {
        getAppLockPage().clearPasswordField();
    }

    @When("^I tap set passcode button$")
    public void iTapSetPasscodeButton() {
        getAppLockPage().tapSetPasscodeButton();
    }

    @When("^I see app lock page$")
    public void iSeeAppLockPage() {
        assertThat("App lock page is not displayed.", getAppLockPage().isAppLockPageVisible());
    }

    @When("^I tap unlock button on app lock page$")
    public void iTapUnlockButton() {
        getAppLockPage().tapUnlockButton();
    }

    @When("^I see error message on app lock page$")
    public void iSeeErrorAppLockPage() {
        assertThat("Error message is not displayed.", getAppLockPage().isErrorMessageVisible());
    }
}
