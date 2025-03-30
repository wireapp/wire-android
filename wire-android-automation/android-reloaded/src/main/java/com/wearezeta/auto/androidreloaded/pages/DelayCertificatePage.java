package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class DelayCertificatePage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Get certificate']")
    private WebElement getCertificateButton;

    @AndroidFindBy(xpath = "//*[@text='Remind me later']")
    private WebElement remindMeLaterButton;

    @AndroidFindBy(xpath = "//*[@text='OK']")
    private WebElement okButton;

    public DelayCertificatePage(WebDriver driver) {
        super(driver);
    }

    public boolean isGetCertificateButtonVisible() {
        return waitUntilElementVisible(getCertificateButton, getDefaultLookupTimeoutSeconds() * 3);
    }

    public void tapGetCertificateButton() {
        getCertificateButton.isDisplayed();
        getCertificateButton.click();
    }

    public void tapRemindMeLaterButton() {
        remindMeLaterButton.isDisplayed();
        remindMeLaterButton.click();
    }

    public void tapOKButton() {
        okButton.click();
    }
}
