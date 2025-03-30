package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ConnectedUserProfilePage;
import com.wearezeta.auto.androidreloaded.pages.DeviceDetailsPage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasItem;

public class DeviceDetailsPageSteps {

    private final AndroidTestContext context;

    public DeviceDetailsPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    @Then("^I see MLS device status is revoked on device details$")
    public void iSeeRevokedShieldInDeviceDetails() {
        assertThat("Revoked shield not visible",
                context.getPage(DeviceDetailsPage.class).isRevokedShieldVisible());
    }

    @Then("^I see E2EI Certificate device status is \"(.*)\" on device details$")
    public void iSeeE2EIStatusInDeviceDetails(String status) {
        assertThat("Status is not visible",
            context.getPage(DeviceDetailsPage.class).isE2eiStatusVisible(status));
    }

    @Then("^I see Get Certificate button on device details$")
    public void iSeeGetCertificateButtonInDeviceDetails() {
        assertThat("Get Certificate button is not visible",
            context.getPage(DeviceDetailsPage.class).isGetCertificateButtonVisible());
    }

    @Then("^I tap on Get Certificate button on device details$")
    public void iTapGetCertificateButtonInDeviceDetails() {
            context.getPage(DeviceDetailsPage.class).iTapGetCertificateButton();
    }

    @Then("^I see Update Certificate button on device details$")
    public void iSeeUpdateCertificateButtonInDeviceDetails() {
        assertThat("Get Certificate button is not visible",
            context.getPage(DeviceDetailsPage.class).isUpdateCertificateButtonVisible());
    }

    @Then("^I tap on Update Certificate button on device details$")
    public void iTapUpdateCertificateButtonInDeviceDetails() {
        context.getPage(DeviceDetailsPage.class).iTapUpdateCertificateButton();
    }

    @Then("^I tap on Show Certificate Details button on device details$")
    public void iTapShowCertificateDetailsButtonInDeviceDetails() {
        context.getPage(DeviceDetailsPage.class).iTapShowCertificateDetailsButton();
    }


}
