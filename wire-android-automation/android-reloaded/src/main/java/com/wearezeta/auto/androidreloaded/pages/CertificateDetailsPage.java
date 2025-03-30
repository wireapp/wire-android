package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;

public class CertificateDetailsPage extends AndroidPage{

    @AndroidFindBy(xpath = "//*[@text='Certificate Details']")
    private WebElement certificateDetailsScreenTitle;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='More options']")
    private WebElement showMoreOptionsButton;

    @AndroidFindBy(xpath = "//*[@text='Download']")
    private WebElement downloadOption;

    @AndroidFindBy(xpath = "//*[@text='Copy to Clipboard']")
    private WebElement copyOption;

    @AndroidFindBy(xpath = "//*[@text='Saved to Downloads folder']")
    private WebElement certificateDownloadedAlert;

    @AndroidFindBy(xpath = "//*[@text='Certificate copied to clipboard']")
    private WebElement certificateCopiedAlert;

    @AndroidFindBy(xpath = "//*[@text='OK']")
    private WebElement okButton;

    public CertificateDetailsPage(WebDriver driver) {super(driver);}

    public boolean isCertificateDetailsScreenVisible() {
        return waitUntilElementVisible(certificateDetailsScreenTitle);
    }

    public void tapShowMoreCertificateOptionsButton() {
        showMoreOptionsButton.isDisplayed();
        showMoreOptionsButton.click();
    }

    public void tapDownloadCertificateOption() {
        downloadOption.isDisplayed();
        downloadOption.click();
    }

    public void tapCopyCertificateOption() {
        copyOption.isDisplayed();
        copyOption.click();
    }

    public void tapOKButtonCertificate() {
        okButton.isDisplayed();
        okButton.click();
    }
    public boolean isCertificateDownloaded(){ return waitUntilElementVisible(certificateDownloadedAlert);}

    public boolean isCertificateCopied(){ return waitUntilElementVisible(certificateCopiedAlert);}
}
