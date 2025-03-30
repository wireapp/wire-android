package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CertificateCouldNotBeUpdatedPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[contains(@text,\"Certificate couldn’t be issued\"')]")
    private WebElement title;

    @AndroidFindBy(xpath = "//*[@text='Retry']")
    private WebElement retryButton;

    @AndroidFindBy(xpath = "//*[@text='Cancel']")
    private WebElement cancelButton;

    public CertificateCouldNotBeUpdatedPage(WebDriver driver) {
        super(driver);
    }

    public boolean isTitleVisible() {
        return waitUntilElementVisible(title);
    }

    public void clickRetry() {
        retryButton.click();
    }

    public void clickCancel() {
        cancelButton.click();
    }
}
