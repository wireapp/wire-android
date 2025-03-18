package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class GetCertificatePage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Get certificate']")
    private WebElement getCertificateButton;

    @AndroidFindBy(xpath = "//*[@text='Remind me later']")
    private WebElement remindMeLaterButton;

    @AndroidFindBy(xpath = "//*[@text='OK']")
    private WebElement okButton;

    public GetCertificatePage(WebDriver driver) {
        super(driver);
    }

    public boolean isGetCertificateButtonVisible() {
        return waitUntilElementVisible(getCertificateButton, getDefaultLookupTimeoutSeconds() * 3);
    }

    public boolean isGetCertificateButtonInvisible() {
        return waitUntilElementInvisible(getCertificateButton, Duration.ofSeconds(getDefaultLookupTimeoutSeconds() * 3));
    }

    public void tapGetCertificateButton() {
        getCertificateButton.isDisplayed();
        getCertificateButton.click();
    }

    public void tapRemindMeLaterButton() {
        remindMeLaterButton.isDisplayed();
        remindMeLaterButton.click();
    }

    public void tapOkButton() {
        okButton.isDisplayed();
        okButton.click();
    }
}
