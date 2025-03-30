package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.Function;

public class GuestOptionsPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Guests']/following-sibling::*[@class='android.view.View']")
    private WebElement guestSwitch;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Create Link']")
    private WebElement createGuestLinkButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Create password secured link']")
    private WebElement createLinkWithPasswordOption;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Create link without password']")
    private WebElement createLinkWithoutPasswordOption;

    @AndroidFindBy(xpath = "//*[@text='GUEST LINK']/following-sibling::*[contains(@text,'conversation-join')]")
    private WebElement guestLink;

    @AndroidFindBy(xpath = " //android.widget.TextView[@text='Link is password secured']")
    private WebElement guestLinkWithPassword;

    @AndroidFindBy(xpath = "//*[@text='Copy Link']")
    private WebElement copyLinkButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Go back to conversation details\"]")
    private WebElement backButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Enter password']")
    private WebElement setPasswordText;

    @AndroidFindBy(xpath = "//*[contains(@text,'Enter password')]/..//*[@class='android.widget.EditText'][1]")
    private WebElement setPasswordField;

    @AndroidFindBy(xpath = "//*[contains(@text,'CONFIRM PASSWORD')]/following-sibling::*[@class='android.widget.EditText']")
    private WebElement confirmPasswordField;

    public GuestOptionsPage(WebDriver driver) {
        super(driver);
    }

    public void tapBackButtonGuestOptions() {
        backButton.isDisplayed();
        backButton.click();
    }

    public void tapOnGuestsSwitch() {
        guestSwitch.click();
    }

    public void tapCreateGuestLinkButton() {
        waitUntilElementVisible(createGuestLinkButton);
        createGuestLinkButton.click();
    }

    public void iTapCreateLinkWithoutPasswordButton() {
        createLinkWithoutPasswordOption.click();
    }

    public void iTapCreateLinkWithPasswordButton() {
        createLinkWithPasswordOption.click();
    }

    public boolean isGuestLinkCreated() {
        return waitUntilElementVisible(guestLink);
    }

    public boolean isGuestLinkCreatedWithPassword() {
        return waitUntilElementVisible(guestLinkWithPassword);
    }

    public boolean isGuestLinkInvisible() {
        return isElementInvisible(guestLink, Timedelta.ofSeconds(2));
    }

    public void tapCopyLinkButton() {
        copyLinkButton.isDisplayed();
        copyLinkButton.click();
    }

    public boolean isCreatePasswordPageVisible() {
        return waitUntilElementVisible(setPasswordText);
    }

    public void iTypePassword(String password) {
        setPasswordField.sendKeys(password);
    }

    public void iTypeConfirmPassword(String password) {
        confirmPasswordField.sendKeys(password);
    }
}
