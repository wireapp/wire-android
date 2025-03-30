package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidDriverBuilder;
import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.DevicesPage;
import com.wearezeta.auto.androidreloaded.pages.SettingsPage;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.And;
import io.cucumber.java.en.When;

import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class DevicesPageSteps {

    private final AndroidTestContext context;

    public DevicesPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private DevicesPage getDevicesPage() {
        return context.getPage(DevicesPage.class);
    }

    @When("^I see Manage Devices Page$")
    public void iSeeManageDevicesPage() {
        assertThat("Manage devices page is not visible.", getDevicesPage().isManageDevicesPageVisible());
    }

    @When("^I see my current device is listed under devices$")
    public void iSeeMyCurrentDevice() {
        assertThat("Current device is not visible.", getDevicesPage().isCurrentDeviceVisible());
    }

    @When("^I tap on my current device listed under devices$")
    public void iTapOnMyCurrentDevice() {
        getDevicesPage().tapCurrentDevice();
    }

    @When("^I tap on my device \"(.*)\" listed under devices$")
    public void iTapOnMyDevice(String device) {
        getDevicesPage().tapDevice(device);
    }

    @When("^I see my other device \"(.*)\" is listed under other devices section$")
    public void iSeeOtherDevices(String device) {
        assertThat(String.format("Other Device '%s' is not visible in other devices section.", device), getDevicesPage().areOtherDevicesVisible(device));
    }

    @When("^I do not see my other device \"(.*)\" is listed under other devices section$")
    public void iDoNotSeeOtherDevices(String device) {
        assertThat(String.format("Other Device '%s' is visible in other devices section.", device), getDevicesPage().areOtherDevicesInvisible(device));
    }

    @When("^I see my current device has ID and date added displayed$")
    public void iSeeIDAndAddedDate() {
        assertThat("Current device does not show ID and date added.", getDevicesPage().doesDeviceShowIDAndDateAdded());
    }

    // Device Details

    @When("^I tap on verify device button$")
    public void iTapVerifyDeviceButton() {
        getDevicesPage().tapVerifyDeviceButton();
    }

    @When("^I see the device is not verified$")
    public void iSeeDevicesIsNotVerified() {
        assertThat("Device is verified.", getDevicesPage().isDeviceNotVerified());
    }

    @When("^I see the device is verified$")
    public void iSeeDevicesIsVerified() {
        assertThat("Device is not verified.", getDevicesPage().isDeviceVerified());
    }

    @When("^I see my MLS thumbprint is displayed$")
    public void isMLSThumbprintVisible() {
        assertThat("MLS Thumbprint is not visible.", getDevicesPage().isMLSThumbprintVisible());
    }

    @When("^I see last active entry on devices page$")
    public void iSeeLastActiveEntryOnDevicesPage() {
        assertThat("Last active entry is not visible.", getDevicesPage().isLastActiveEntryVisible());
    }

    @When("^I see that my device was used (.*) on last active entry on devices page$")
    public void iSeeWhenDeviceWasUsed(String time) {
        assertThat("Last active entry does not contain the correct time.", getDevicesPage().getTextLastActiveEntry(), containsString(time));
    }

    @When("^I do not see remove device button$")
    public void iDoNotSeeRemoveDeviceButton() {
        assertThat("Remove Device Button is visible. ", getDevicesPage().isRemoveDeviceButtonInvisible());
    }

    @And("I tap on show certificate details button")
    public void iTapOnShowCertificateDetailsButton() {
        getDevicesPage().tapCertificateDetailsButton();
    }

    // Misc

    @When("^I close the devices details screen through the back arrow$")
    public void iCloseDeviceDetailsScreen() {
        getDevicesPage().closeDevicesScreen();
    }
    @When("^I close the devices screen through the back arrow$")
    public void iCloseMyDevicesScreen() {
        getDevicesPage().closeDevicesScreen();
    }

}
