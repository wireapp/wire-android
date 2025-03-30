package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.DelayCertificatePage;
import io.cucumber.java.en.When;

public class DelayCertificatePageSteps {

    private final AndroidTestContext context;

    public DelayCertificatePageSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I tap Remind me later button on E2EI delay certificate alert$")
    public void iTapRemindMeLater() {
        context.getPage(DelayCertificatePage.class).tapRemindMeLaterButton();
    }

    @When("^I tap get certificate button on E2EI delay certificate alert$")
    public void iTapGetCertificateButtonAlert() {
        context.getPage(DelayCertificatePage.class).tapGetCertificateButton();
    }

    @When("^I tap OK button on E2EI delay certificate alert$")
    public void iTapOK() {
        context.getPage(DelayCertificatePage.class).tapOKButton();
    }
}
