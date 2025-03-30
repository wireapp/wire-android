package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class UnconnectedUserProfilePage extends AndroidPage {

    private final Function<String, String> userProfile = name -> String.format("//*[@text='User Profile']/../../../..//*[@text=\"%s\"]", name);

    private final Function<String, String> userName = name -> String.format("//android.widget.TextView[@content-desc=\"Profile name, %s\"]", name);

    private final Function<String, String> qualifiedUserName = name -> String.format("//*[@content-desc='Profile picture']/../..//*[@text='%s']", name);

    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    @AndroidFindBy(xpath = "//*[@text='Federated']")
    private WebElement federatedLabel;

    @AndroidFindBy(xpath = "//*[@text='Guest']")
    private WebElement guestLabel;

    @AndroidFindBy(xpath = "//*[@text='Connect']")
    private WebElement connectButton;

    @AndroidFindBy(xpath = "//*[@text='Accept']")
    private WebElement acceptButton;

    @AndroidFindBy(xpath = "//*[@text='Ignore']")
    private WebElement ignoreButton;

    @AndroidFindBy(xpath = "//*[@text='Cancel Request']")
    private WebElement cancelConnectionRequestButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Close\"]")
    private WebElement closeButton;

    public UnconnectedUserProfilePage(WebDriver driver) {
        super(driver);
    }

    public boolean isUserProfileVisible(String name) {
        return getDriver().findElement(By.xpath(userProfile.apply(name))).isDisplayed();
    }

    public boolean isTextDisplayed(String text) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(text))));
    }

    public boolean isTextInvisible(String text) {
        return isLocatorInvisible(textBy.apply(text));
    }

    public boolean isUserNameVisible(String name) {
        return getDriver().findElement(By.xpath(userName.apply(name))).isDisplayed();
    }

    public boolean isFullyQualifiedUserNameVisible(String name) {
        return getDriver().findElement(By.xpath(qualifiedUserName.apply(name))).isDisplayed();
    }

    public boolean isFederatedLabelVisible() {
        return federatedLabel.isDisplayed();
    }

    public boolean isFederatedLabelInvisible() {
        return waitUntilElementInvisible(federatedLabel);
    }

    public boolean isGuestLabelVisible() {
        return guestLabel.isDisplayed();
    }

    public void tapConnectButton() {
        connectButton.isDisplayed();
        connectButton.click();
    }

    public boolean isAcceptButtonVisible() {
        return acceptButton.isDisplayed();
    }

    public void tapAcceptButton() {
        acceptButton.isDisplayed();
        acceptButton.click();
    }

    public boolean isIgnoreButtonVisible() {
        return ignoreButton.isDisplayed();
    }

    public void tapIgnoreButton() {
        ignoreButton.isDisplayed();
        ignoreButton.click();
    }

    public boolean waitUntilCancelConnectionRequestButtonVisible() {
        return waitUntilElementVisible(cancelConnectionRequestButton);
    }

    public void tapCancelConnectionRequestButton() {
        cancelConnectionRequestButton.isDisplayed();
        cancelConnectionRequestButton.click();
    }

    public boolean isConnectionTextVisible(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public void tapCloseButton() {
        closeButton.isDisplayed();
        closeButton.click();
    }
}
