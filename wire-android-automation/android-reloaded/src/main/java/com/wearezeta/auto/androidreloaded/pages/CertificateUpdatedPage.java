package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

public class CertificateUpdatedPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Certificate updated']")
    private WebElement certificateUpatedHeader;

    @AndroidFindBy(xpath = "//*[@text='OK']")
    private WebElement okButton;

    @AndroidFindBy(xpath = "//*[@text='Certificate Details']")
    private WebElement certificateDetailsButton;

    @AndroidFindBy(xpath = "//android.widget.ScrollView")
    private WebElement scrollView;

    public CertificateUpdatedPage(WebDriver driver) {
        super(driver);
    }

    public boolean isCertificateUpdatedVisible() {
        return waitUntilElementVisible(certificateUpatedHeader);
    }

    public void tapOKButton() {
        okButton.isDisplayed();
        okButton.click();
    }

    public void tapCertificateDetailsButton() {
        certificateDetailsButton.isDisplayed();
        certificateDetailsButton.click();
    }

    public String getCertificateDetails() {
        return scrollView.getText();
    }
}
