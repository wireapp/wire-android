package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import io.cucumber.java.en.When;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class DevicesPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Your Devices']")
    private WebElement manageDevicesPageHeading;

    @AndroidFindBy(xpath = "//*[@text='CURRENT DEVICE']/..//android.view.View[@resource-id=\"device_item\"]")
    private WebElement currentDevice;

    @AndroidFindBy(xpath = "//*[@content-desc='Device item']")
    private WebElement deviceItem;

    @AndroidFindBy(xpath = "//*[@text='DEVICE KEY FINGERPRINT'][2]/following-sibling::*[@class='android.view.View']")
    private WebElement verifyDeviceToggle;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Not Verified\"))")
    private WebElement deviceNotVerifiedText;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Verified']")
    private WebElement deviceVerifiedText;

    @AndroidFindBy(xpath = "//android.widget.ImageView[@content-desc='Verified']")
    private WebElement verifiedShield;

    @AndroidFindBy(xpath = "//*[@text='Show Certificate Details']")
    private WebElement showCertificateDetailsButton;

    @AndroidFindBy(xpath = "//*[@text='MLS Thumbprint']")
    private WebElement MLSThumbprintEntry;

    @AndroidFindBy(xpath = "//*[@text='LAST ACTIVE']")
    private WebElement lastActiveEntry;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='LAST ACTIVE']/following-sibling::*[@class='android.widget.TextView'][1]")
    private WebElement textLastActiveEntry;

    @AndroidFindBy(xpath = "//android.view.View[@resource-id=\"device_item\"]//following-sibling::android.widget.TextView[contains(@text,'ID') and contains(@text,'Added')]")
    private WebElement currentDeviceDetails;

    @AndroidFindBy(xpath = "//*[@class='android.widget.Button']/preceding-sibling::*[@text='Remove device']")
    private WebElement removeDeviceButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Go back\"]")
    private WebElement closeButtonSettings;

    private final Function<String, By> otherDevices = deviceName -> By.xpath(String.format("//android.view.View[@resource-id=\"device_item\"]//following-sibling::*[contains(@content-desc,'%s')]", deviceName));

    public DevicesPage(WebDriver driver) {
        super(driver);
    }

    public boolean isManageDevicesPageVisible() {
        return manageDevicesPageHeading.isDisplayed();
    }

    public boolean isCurrentDeviceVisible() {
        return currentDevice.isDisplayed();
    }

    public void tapCurrentDevice() {
        currentDevice.click();
    }

    public void tapDevice(String deviceName) {
        getDriver().findElement(otherDevices.apply(deviceName)).click();
    }

    public boolean areOtherDevicesVisible(String deviceName) {
        return getDriver().findElement(otherDevices.apply(deviceName)).isDisplayed();
    }

    public boolean areOtherDevicesInvisible(String deviceName) {
        return isLocatorInvisible(otherDevices.apply(deviceName));
    }

    public boolean doesDeviceShowIDAndDateAdded() {
        return currentDeviceDetails.isDisplayed();
    }

    public boolean isRemoveDeviceButtonInvisible() {
        return isElementInvisible(removeDeviceButton, Timedelta.ofSeconds(1));
    }

    public void closeDevicesScreen() {
        closeButtonSettings.isDisplayed();
        closeButtonSettings.click();
    }

    public void tapVerifyDeviceButton() {
        waitUntilElementVisible(verifyDeviceToggle);
        verifyDeviceToggle.click();

    }

    public boolean isDeviceNotVerified() {
        return waitUntilElementVisible(deviceNotVerifiedText);
    }

    public boolean isDeviceVerified() {
        return waitUntilElementVisible(deviceVerifiedText);
    }

    public boolean isVerifiedShieldVisible() {
        return waitUntilElementVisible(verifiedShield);
    }

    public boolean isMLSThumbprintVisible() {
        return waitUntilElementVisible(MLSThumbprintEntry);
    }

    public boolean isLastActiveEntryVisible() {
        return waitUntilElementVisible(lastActiveEntry);
    }

    public String getTextLastActiveEntry() {
        return textLastActiveEntry.getText();
    }

    public void tapCertificateDetailsButton() {
        showCertificateDetailsButton.isDisplayed();
        showCertificateDetailsButton.click();
    }
}
