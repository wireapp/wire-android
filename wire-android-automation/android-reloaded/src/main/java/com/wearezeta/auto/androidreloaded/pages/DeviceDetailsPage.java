package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class DeviceDetailsPage extends AndroidPage {

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Revoked']")
    private WebElement revokedShield;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Get Certificate']")
    private WebElement getCertificateButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Update Certificate']")
    private WebElement updateCertificateButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Show Certificate Details']")
    private WebElement showCertificateDetailButton;

    private final Function<String, By> e2eiStatus = status -> By.xpath(String.format("//android.widget.TextView[@text='%s']", status));


    public DeviceDetailsPage(WebDriver driver) {
        super(driver);
    }


    public boolean isRevokedShieldVisible() {
        return waitUntilElementVisible(revokedShield);
    }

    public boolean isE2eiStatusVisible(String status) {
        final By locator = e2eiStatus.apply(status);
        return waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(2));
    }

    public boolean isGetCertificateButtonVisible() {
        return waitUntilElementVisible(getCertificateButton);
    }

    public void iTapGetCertificateButton() {
         getCertificateButton.isDisplayed();
         getCertificateButton.click();
    }

    public boolean isUpdateCertificateButtonVisible() {
        return waitUntilElementVisible(updateCertificateButton);
    }

    public void iTapUpdateCertificateButton() {
        updateCertificateButton.isDisplayed();
        updateCertificateButton.click();
    }

    public void iTapShowCertificateDetailsButton() {
        waitUntilElementVisible(showCertificateDetailButton);
        showCertificateDetailButton.click();
    }

}
