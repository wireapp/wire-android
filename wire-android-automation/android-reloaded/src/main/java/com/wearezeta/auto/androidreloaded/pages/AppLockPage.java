package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class AppLockPage extends AndroidPage {

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Set app lock passcode']")
    private WebElement setupAppLockHeading;

    @AndroidFindBy(xpath = "//android.widget.TextView[contains(@text,'The app will lock itself after 1 minute of inactivity')]")
    private WebElement descriptionAppLock;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='PASSCODE']/..//*[@class='android.widget.EditText']")
    private WebElement passcodeTextField;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Set a passcode']")
    private WebElement setPasscodeButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Enter passcode to unlock Wire']")
    private WebElement appLockHeading;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Unlock']")
    private WebElement unlockButton;

    @AndroidFindBy(xpath = "//*[@text='Check your passcode and try again']")
    private WebElement errorMessage;

    public AppLockPage(WebDriver driver) {
        super(driver);
    }

    public boolean isSetUpAppLockPageVisible() {
        return waitUntilElementVisible(setupAppLockHeading);
    }

    public boolean isDescriptionTextAppLockVisible() {
        return waitUntilElementVisible(descriptionAppLock);
    }

    public void enterPassCode(String passcode) {
        passcodeTextField.sendKeys(passcode);
    }

    public void clearPasswordField() {
        passcodeTextField.clear();
    }

    public void tapSetPasscodeButton() {
        setPasscodeButton.click();
    }

    public boolean isAppLockPageVisible() {
        return waitUntilElementVisible(appLockHeading);
    }

    public void tapUnlockButton() {
        unlockButton.click();
    }

    public boolean isErrorMessageVisible() {
        return waitUntilElementVisible(errorMessage);
    }
}
