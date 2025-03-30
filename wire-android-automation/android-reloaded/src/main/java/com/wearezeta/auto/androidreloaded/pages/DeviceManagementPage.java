package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.AppiumBy;
import io.appium.java_client.android.AndroidDriver;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.hamcrest.Matcher;
import org.openqa.selenium.By;
import org.openqa.selenium.JavascriptExecutor;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class DeviceManagementPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Remove the following device?']")
    private WebElement removeDeviceAlertHeading;

    @AndroidFindBy(xpath = "//*[contains(@text,'Added')]")
    private WebElement deviceInformation;

    @AndroidFindBy(xpath = "//*[contains(@text,'PASSWORD')]/..//*[@class='android.widget.EditText']")
    private WebElement passwordInputField;

    @AndroidFindBy(xpath = "//*[@text='Remove']")
    private WebElement removeButton;

    @AndroidFindBy(xpath = "//*[@text='Invalid password']")
    private WebElement invalidPassWordError;

    public final Function<String, String> trashCanIconFirstDevice = deviceName -> String.format("(//*[@text='%s']/../..//*[@content-desc='Remove device'])[1]", deviceName);

    public final Function<String, String> device = deviceName -> String.format("//*[@text='%s']", deviceName);

    public DeviceManagementPage(WebDriver driver) {
        super(driver);
    }

    public void tapFirstDevice(String deviceName) {
        By locator = By.xpath(trashCanIconFirstDevice.apply(deviceName));
        waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(getDefaultLookupTimeoutSeconds() * 3L));
        WebElement deviceID = getDriver().findElement(locator);
        deviceID.click();
    }

    public boolean isRemoveDeviceAlertVisible() {
        return removeDeviceAlertHeading.isDisplayed();
    }

    public String getTextDeviceDetails() {
        return deviceInformation.getText();
    }

    public void enterPasswordCredentials(String password) {
        passwordInputField.isDisplayed();
        passwordInputField.sendKeys(password);
    }

    public void tapRemoveButton() {
        removeButton.isDisplayed();
        removeButton.click();
    }

    public boolean isInvalidPasswordErrorVisible() {
        return invalidPassWordError.isDisplayed();
    }
}
