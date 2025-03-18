package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.CertificateDetailsPage;
import io.cucumber.java.en.And;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

public class CertificateDetailsPageSteps {

    private final AndroidTestContext context;

    public CertificateDetailsPageSteps(AndroidTestContext context){ this.context = context; }

    @Then("I see certificate details screen")
    public void iSeeCertificateDetailsScreen() { context.getPage(CertificateDetailsPage.class).isCertificateDetailsScreenVisible();
    }

    @When("^I tap show more options button on certificate details screen$")
    public void iTapShowMoreOptionsButtonOnCertificateDetailsScreen() {
        context.getPage(CertificateDetailsPage.class).tapShowMoreCertificateOptionsButton();
    }

    @When("^I tap download option on certificate details screen$")
    public void iTapDownloadOptionOnCertificateDetailsScreen() {
        context.getPage(CertificateDetailsPage.class).tapDownloadCertificateOption();
    }

    @Then("I see certificate downloaded alert")
    public void iSeeCertificateDownloadedAlert() {
        context.getPage(CertificateDetailsPage.class).isCertificateDownloaded();
    }

    @When("I tap copy to clipboard option")
    public void iTapCopyToClipboardOption() {
        context.getPage(CertificateDetailsPage.class).tapCopyCertificateOption();
    }

    @Then("I see certificate copied alert")
    public void iSeeCertificateCopiedAlert() {
        context.getPage(CertificateDetailsPage.class).isCertificateCopied();
    }
}
