package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.CertificateUpdatedPage;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;

public class CertificateUpdatedPageSteps {

    private final AndroidTestContext context;

    public CertificateUpdatedPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    @When("^I see certificate updated alert$")
    public void iSeeCertificateUpdated() {
        assertThat("Certificate updated alert not visible", context.getPage(CertificateUpdatedPage.class).isCertificateUpdatedVisible());
    }

    @When("^I tap OK button on E2EI certificate updated alert$")
    public void iTapOKButtonForE2EICertificate() {
        context.getPage(CertificateUpdatedPage.class).tapOKButton();
    }

    @When("^I tap Certificate Details button on E2EI certificate updated alert$")
    public void iTapCertificateDetailsButton() {
        context.getPage(CertificateUpdatedPage.class).tapCertificateDetailsButton();
    }

    @When("^I remember certificate from certificate details")
    public void iRememberCertificateFromDetails() {
        String text = context.getPage(CertificateUpdatedPage.class).getCertificateDetails();
        text = text.substring(0, text.indexOf("-----END CERTIFICATE-----") + 25);
        context.setRememberedCertificate(text);
    }

    @When("^I tap back button on certificate details")
    public void iTapBackFromDetails() {
        context.getPage(CertificateUpdatedPage.class).navigateBack();
    }
}
