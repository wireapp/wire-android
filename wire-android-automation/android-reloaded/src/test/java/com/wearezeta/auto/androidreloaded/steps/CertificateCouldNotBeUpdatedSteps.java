package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.CertificateCouldNotBeUpdatedPage;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class CertificateCouldNotBeUpdatedSteps {

    private final AndroidTestContext context;

    public CertificateCouldNotBeUpdatedSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I see error that certificate could not be updated$")
    public void iSeeError() {
        assertThat("", context.getPage(CertificateCouldNotBeUpdatedPage.class).isTitleVisible());
    }

    @When("^I click retry button on error that certificate could not be updated$")
    public void iClickRetry() {
        context.getPage(CertificateCouldNotBeUpdatedPage.class).clickRetry();
    }

    @When("^I click cancel button on error that certificate could not be updated$")
    public void iClickCancel() {
        context.getPage(CertificateCouldNotBeUpdatedPage.class).clickCancel();
    }
}
