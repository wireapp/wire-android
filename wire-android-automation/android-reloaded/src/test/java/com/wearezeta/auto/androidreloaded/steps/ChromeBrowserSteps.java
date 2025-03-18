package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.external.ChromePage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

public class ChromeBrowserSteps {

    private final AndroidTestContext context;

    public ChromeBrowserSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I accept Terms in system browser$")
    public void iAcceptTermsInBrowser() {
        context.getPage(ChromePage.class).clickAccept();
        context.getPage(ChromePage.class).clickNoThanks();
    }

    @And("I tap use without an account button if visible")
    public void iTapUseWithoutAnAccountButtonIfVisible() {
        if (context.getPage(ChromePage.class).isChromeLoginVisible()) {
            context.getPage(ChromePage.class).tapUseWithoutAccountButton();
        }
    }
}
