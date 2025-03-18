package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.CertificateCouldNotBeUpdatedPage;
import com.wearezeta.auto.androidreloaded.pages.GetCertificatePage;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class GetCertificatePageSteps {

    private final AndroidTestContext context;

    public GetCertificatePageSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I see get certificate alert$")
    public void iSeeGetCertificateAlert() {
        context.getPage(GetCertificatePage.class).isGetCertificateButtonVisible();
    }

    @When("^I do not see get certificate alert$")
    public void iDoNotSeeGetCertificateAlert() {
        context.getPage(GetCertificatePage.class).isGetCertificateButtonInvisible();
    }

    @When("^I tap get certificate alert$")
    public void iTapGetCertificateButtonAlert() {
        context.getPage(GetCertificatePage.class).tapGetCertificateButton();
    }

    @When("^I tap remind me later alert$")
    public void iTapRemindMeLaterButtonAlert() {
        context.getPage(GetCertificatePage.class).tapRemindMeLaterButton();
    }

    @When("^I tap OK button on E2EI alert$")
    public void iTapOKButtonAlert() {
        context.getPage(GetCertificatePage.class).tapOkButton();
    }
}
