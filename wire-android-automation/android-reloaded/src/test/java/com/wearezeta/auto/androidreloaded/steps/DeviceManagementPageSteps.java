package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.DeviceManagementPage;
import com.wearezeta.auto.common.log.ZetaLogger;
import com.wearezeta.auto.common.usrmgmt.ClientUser;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.When;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.logging.Logger;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

public class DeviceManagementPageSteps {

    private final AndroidTestContext context;

    public DeviceManagementPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private static final Logger log = ZetaLogger.getLog(DeviceManagementPageSteps.class.getSimpleName());

    @When("^I select first device (.*) on device removal page$")
    public void iSelectDevice(String device) {
        context.getPage(DeviceManagementPage.class).tapFirstDevice(device);
    }

    @When("^I see remove device alert$")
    public void iSeeRemoveDeviceAlert() {
        context.getPage(DeviceManagementPage.class).isRemoveDeviceAlertVisible();
    }

    @When("^I see device name (.*) on the alert$")
    public void iSeeDeviceNameOnAlert(String deviceName) {
        assertThat("Device name is not visible.", context.getPage(DeviceManagementPage.class).getTextDeviceDetails(), containsString(deviceName));
    }

    @When("^I see device ID on the alert$")
    public void iSeeDeviceIDOnAlert() {
        assertThat("ID is not visible.", context.getPage(DeviceManagementPage.class).getTextDeviceDetails(), containsString("ID"));
    }

    @When("^I see date of device creation on the alert$")
    public void iSeeDateAndTimeStampOnAlert() {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy");
        LocalDateTime currentTime = LocalDateTime.now();
        String date = currentTime.format(formatter);
        if (!context.getPage(DeviceManagementPage.class).getTextDeviceDetails().contains(date)) {
            formatter = DateTimeFormatter.ofPattern("d");
            currentTime = LocalDateTime.now();
            date = currentTime.format(formatter);
        }
        assertThat("Date is not the same.", context.getPage(DeviceManagementPage.class).getTextDeviceDetails(),
                containsString(date));
    }

    @When("^I enter my password (.*) on remove device alert$")
    public void iEnterPassWordOnAlert(String password) {
        ClientUser userToRegister = context.getUsersManager().findUserByPasswordAlias(password);
        password = userToRegister.getPassword();
        context.getPage(DeviceManagementPage.class).enterPasswordCredentials(password);
    }

    @When("^I enter my invalid password (.*) on remove device alert$")
    public void iEnterInvalidPassWordOnAlert(String password) {
        context.getPage(DeviceManagementPage.class).enterPasswordCredentials(password);
    }

    @When("^I tap Remove button on remove device alert$")
    public void iTapRemoveOnAlert() {
        context.getPage(DeviceManagementPage.class).tapRemoveButton();
    }

    @When("^I see invalid password error$")
    public void iSeeInvalidPasswordError() {
        assertThat("Invalid password error is not visible.", context.getPage(DeviceManagementPage.class).isInvalidPasswordErrorVisible());
    }
}
