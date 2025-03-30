package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.Function;

public class SelfUserProfilePage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='User Profile']")
    private WebElement userProfilePageTitle;

    @AndroidFindBy(xpath = "//*[@text='Log out']")
    private WebElement logoutButton;

    @AndroidFindBy(xpath = "//*[@text='AVAILABILITY']/..//android.widget.TextView[@text=\"None\"]")
    private WebElement statusDropdown;

    @AndroidFindBy(xpath = "//*[@text='AVAILABILITY']/..//android.widget.TextView[@text=\"Busy\"]")
    private WebElement statusDropdownBusy;

    @AndroidFindBy(xpath = "//*[@text='Available']")
    private WebElement availableOption;

    @AndroidFindBy(xpath = "//*[@text='Busy']")
    private WebElement busyOption;

    @AndroidFindBy(xpath = "//*[@text='Away']")
    private WebElement awayOption;

    @AndroidFindBy(xpath = "//*[@text='None']")
    private WebElement noneOption;

    //TODO: ask for better locator for alert and alertText or add it
    @AndroidFindBy(xpath = "//*[contains(@text,'You will')]")
    private WebElement infoTextChangeStatus;

    @AndroidFindBy(xpath = "//*[@text='New Team or Add Account']")
    private WebElement tapNewTeamOrAccountButton;

   @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Close your profile\"]")
   private WebElement closeButton;

    @AndroidFindBy(xpath = "//*[contains(@text,'You can only be logged in with')]")
    private WebElement tooManyAccountsAlertText;

    @AndroidFindBy(xpath = "//*[@text='Clear Data?']")
    private WebElement clearDataAlertHeading;

    @AndroidFindBy(xpath = "//*[@class='android.widget.CheckBox']")
    private WebElement checkbox;

    @AndroidFindBy(xpath = "//*[@text='YOUR OTHER ACCOUNTS']")
    private WebElement otherAccountsText;

    @AndroidFindBy(xpath = "//*[@class='android.widget.CheckBox']/..//*[@class='android.widget.TextView']")
    private WebElement infoTextCheckbox;

    @AndroidFindBy(xpath = "//*[@text='Legal hold is pending']")
    private WebElement legalHoldPendingText;

    @AndroidFindBy(xpath = "//*[@text='Accept']")
    private WebElement acceptButtonLegalHoldPending;

    private final Function<String, String> currentStatus = status -> String.format("//*[@text='AVAILABILITY']/..//*[@text='%s']", status);

    private final Function<String, String> currentActiveAccount = account -> String.format("//android.view.View[@content-desc=\"Your profile picture\"]/../..//*[@text=\"%s\"]", account);

    private final Function<String, String> otherAccountNameString = account -> String.format("//*[@text='YOUR OTHER ACCOUNTS']/..//*[@text='%s']", account);

    private final Function<String, By> otherAccountNameBy = account -> By.xpath(String.format("//*[@text='YOUR OTHER ACCOUNTS']/..//*[@text='%s']", account));
    
    public SelfUserProfilePage(WebDriver driver) {
        super(driver);
    }

    public boolean isUserProfilePageVisible() {
        return waitUntilElementVisible(userProfilePageTitle);
    }

    public boolean isCurrentAccountActive(String account) {
        return getDriver().findElement(By.xpath(currentActiveAccount.apply(account))).isDisplayed();
    }

    public boolean iSeeChangeStatusOptions() {
        return waitUntilElementVisible(statusDropdown);
    }

    public boolean iSeeCurrentStatus(String status) {
        return getDriver().findElement(By.xpath(currentStatus.apply(status))).isDisplayed();
    }

    public void changeMyStatusToAvailable() {
        statusDropdown.click();
        availableOption.click();
    }

    public void changeMyStatusToBusy() {
        statusDropdown.click();
        busyOption.click();
    }

    public void changeMyStatusToAway() {
        statusDropdown.click();
        awayOption.click();
    }

    public void changeMyStatusFromBusyToNone() {
        statusDropdownBusy.click();
        noneOption.click();
    }

    public String getTextAlert() {
        return infoTextChangeStatus.getText();
    }

    public boolean iSeeMyOtherAccount(String account) {
        final By locator = otherAccountNameBy.apply(account);
        scrollUntilElementVisible(locator,0,9);
        return waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(1));
    }

    public boolean isOtherAccountsSectionInvisible() {
        return waitUntilElementInvisible(otherAccountsText);
    }

    public void tapAccount(String account) {
        getDriver().findElement(By.xpath(otherAccountNameString.apply(account))).click();
    }

    public void tapNewTeamOrAccountButton() {
        tapNewTeamOrAccountButton.isDisplayed();
        tapNewTeamOrAccountButton.click();
    }

    public void iTapLogoutButton() {
        logoutButton.isDisplayed();
        logoutButton.click();
    }

    public boolean isTooManyAccountsAlertVisible() {
        return tooManyAccountsAlertText.isDisplayed();
    }

    public boolean isClearDataAlertVisible() {
        return clearDataAlertHeading.isDisplayed();
    }

    public String getTextLogoutAlert() {
        infoTextCheckbox.isDisplayed();
        return infoTextCheckbox.getText();
    }

    public void selectClearData() {
        checkbox.isDisplayed();
        checkbox.click();
    }

    public void tapCloseButton() {
        closeButton.isDisplayed();
        closeButton.click();
    }

    public boolean isLegalHoldPendingVisible() {
        return waitUntilElementVisible(legalHoldPendingText);
    }

    public void tapAcceptButtonLegalHoldPending() {
        acceptButtonLegalHoldPending.click();
    }
}
