package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

public class LegalHoldPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Legal hold requested']")
    private WebElement legalHoldModal;

    @AndroidFindBy(xpath = "//*[@text='Legal hold deactivated']")
    private WebElement legalHoldDeactivatedModal;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='OK']")
    private WebElement OkButtonLegalHoldDeactivatedModal;

    @AndroidFindBy(xpath = "//*[contains(@text,'PASSWORD')]/..//*[@class='android.widget.EditText']")
    private WebElement passwordFieldLegalHoldModal;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Accept']")
    private WebElement acceptButtonLegalHold;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Not Now']")
    private WebElement notNowButtonLegalHold;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Invalid password']")
    private WebElement passwordErrorLegalHold;

    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    public LegalHoldPage(WebDriver driver) {
        super(driver);
    }

    public boolean isTextDisplayed(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public boolean isLegalHoldModalVisible() {
        return waitUntilElementVisible(legalHoldModal, getDefaultLookupTimeoutSeconds() * 2);
    }

    public boolean isLegalHoldDeactivatedModalVisible() {
        return waitUntilElementVisible(legalHoldDeactivatedModal);
    }

    public boolean isLegalHoldModalInvisible() {
        return isElementInvisible(legalHoldModal, Timedelta.ofSeconds(1));
    }

    public void enterPasswordLegalHold(String password) {
        passwordFieldLegalHoldModal.sendKeys(password);
    }

    public void clearPasswordLegalHold() {
        passwordFieldLegalHoldModal.clear();
    }

    public boolean isPasswordFieldLegalHoldInvisible() {
        return isElementInvisible(passwordFieldLegalHoldModal, Timedelta.ofSeconds(1));
    }

    public void acceptLegalHoldRequest() {
        waitUntilElementVisible(acceptButtonLegalHold);
        acceptButtonLegalHold.click();
    }

    public void tapOkOnLegalHoldDeactivatedModal() {
        waitUntilElementVisible(OkButtonLegalHoldDeactivatedModal);
        OkButtonLegalHoldDeactivatedModal.click();
    }

    public void delayLegalHoldRequest() {
        waitUntilElementVisible(notNowButtonLegalHold);
        notNowButtonLegalHold.click();
    }

    public boolean isPasswordErrorLegalHoldVisible() {
        return waitUntilElementVisible(passwordErrorLegalHold);
    }

}
