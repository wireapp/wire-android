package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

public class ConnectedUserProfilePage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Open Conversation']")
    private WebElement openConversationButton;

    @AndroidFindBy(xpath = "//*[@text='Start Conversation']")
    private WebElement startConversationButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Open conversation options']")
    private WebElement showMoreOptions;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Notifications']")
    private WebElement notifications;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Everything']")
    private WebElement notificationEverything;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Block']")
    private WebElement blockOption;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Unblock']")
    private WebElement unblockOption;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Block']")
    private WebElement blockButtonAlert;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Notifications']")
    private WebElement notificationsOption;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Remove from group\"))")
    private WebElement removeButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Remove From Group']")
    private WebElement removeServiceButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Remove']")
    private WebElement removeButtonAlert;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Blocked']")
    private WebElement blockedLabel;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Unblock User']")
    private WebElement unblockUserButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Unblock']")
    private WebElement unblockButtonAlert;

    @AndroidFindBy(xpath = "//*[@text='Federated']")
    private WebElement federatedLabel;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='DEVICES']")
    private WebElement devicesTab;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Device item']")
    private WebElement firstDevice;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Go back to conversation details\"]")
    private WebElement closeButtonBackToConversationDetails;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Close\"]")
    private WebElement closeButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Edit role\"]")
    private WebElement editButton;

    @AndroidFindBy(xpath = "//*[@text='Admin']")
    private WebElement adminButton;

    @AndroidFindBy(xpath = "//*[@text='ROLE']/following-sibling::*[@text='Admin']")
    private WebElement userRoleAdminEntry;

    @AndroidFindBy(xpath = "//*[@text='Unable to start conversation']")
    private WebElement unableToStartConversationHeading;

    @AndroidFindBy(xpath = "//android.widget.ImageView[@content-desc='Verified']")
    private WebElement verifiedShield;

    private final Function<String, String> notificationChoice = text -> String.format("//android.widget.TextView[@text='%s']", text);

    private final Function<String, String> toastMessage = text -> String.format("//android.widget.TextView[@text='%s']", text);

    private final Function<String, String> userProfileUser = name -> String.format("//*[@text='User Profile']/../../../..//*[@text=\"%s\"]", name);

    private final Function<String, String> userNameRemoveAlert = name -> String.format("//android.widget.TextView[@text='Remove from group?']/..//*[contains(@text,'%s')]", name);

    private final Function<String, By> devices = deviceName -> By.xpath(String.format("//android.widget.TextView[contains(@text,'Wire gives every device')]/..//*[contains(@content-desc,'%s')]", deviceName));

    private final Function<String, String> textString = text -> String.format("//*[@text=\"%s\"]", text);

    private final Function<String, String> textStringContains = text -> String.format("//*[contains(@text,\"%s\")]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text=\"%s\"]", text));

    public ConnectedUserProfilePage(WebDriver driver) {
        super(driver);
    }

    public void scrollToTheBottom() {
        this.hideKeyboard();
        for (int i = 0; i < 3; ++i) {
            scroll(0,-1);
        }
    }

    public void scrollToTheTop() {
        this.hideKeyboard();
        for (int i = 0; i < 3; ++i) {
            scroll(0,9);
        }
    }

    public boolean isUserProfileVisible(String name) {
        return getDriver().findElement(By.xpath(userProfileUser.apply(name))).isDisplayed();
    }

    public boolean isFederatedLabelVisible() {
        return federatedLabel.isDisplayed();
    }

    public boolean isFederatedLabelInvisible() {
        return waitUntilElementInvisible(federatedLabel);
    }

    public boolean isTextDisplayed(String text) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(text))));
    }

    public boolean isTextContainsDisplayed(String text) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textStringContains.apply(text))));
    }

    public boolean isTextInvisible(String text) {
        return isLocatorInvisible(textBy.apply(text));
    }

    public void tapOpenConversationButton() {
        openConversationButton.isDisplayed();
        openConversationButton.click();
    }

    public void tapStartConversationButton() {
        startConversationButton.isDisplayed();
        startConversationButton.click();
    }

    public void tapShowMoreOptionsButton() {
        waitUntilElementVisible(showMoreOptions);
        showMoreOptions.click();
    }

    public void tapNotificationsButton() {
        notifications.isDisplayed();
        notifications.click();
    }

    public boolean isEverythingVisible() {
        return waitUntilElementVisible(notificationEverything);
    }

    public void tapNotificationOf(String notification) {
        getDriver().findElement(By.xpath(notificationChoice.apply(notification))).click();
    }

    public boolean isNotificationCorrect(String notification) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(notificationChoice.apply(notification))));
    }

    public boolean isBlockOptionVisible() {
        return blockOption.isDisplayed();
    }

    public boolean isBlockOptionInvisible() {
        return waitUntilElementInvisible(blockOption);
    }

    public void tapBlockOption() {
        blockOption.isDisplayed();
        blockOption.click();
    }

    public void tapUnblockOption() {
        unblockOption.isDisplayed();
        unblockOption.click();
    }

    public void tapBlockButtonOnAllert() {
        blockButtonAlert.isDisplayed();
        blockButtonAlert.click();
    }

    public boolean isToastMessageVisible(String message) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(toastMessage.apply(message))));
    }

    public boolean isRemoveButtonVisible() {
        return removeButton.isDisplayed();
    }

    public boolean isRemoveServiceButtonVisible() {
        return removeServiceButton.isDisplayed();
    }

    public boolean isRemoveButtonInvisible() {
        return waitUntilElementInvisible(removeButton);
    }

    public boolean isRemoveServiceButtonInvisible() {
        return waitUntilElementInvisible(removeServiceButton);
    }

    public void tapRemoveFromGroupButton() {
        removeButton.isDisplayed();
        removeButton.click();
    }

    public void tapRemoveServiceFromGroupButton() {
        removeServiceButton.isDisplayed();
        removeServiceButton.click();
    }

    public boolean isAlertRemoveUserVisible(String user) {
        return getDriver().findElement(By.xpath(userNameRemoveAlert.apply(user))).isDisplayed();
    }

    public void tapRemoveFromGroupButtonOnAlert() {
        removeButtonAlert.isDisplayed();
        removeButtonAlert.click();
    }

    public boolean isBlockedLabelVisible() {
        return blockedLabel.isDisplayed();
    }

    public boolean isBlockedLabelInvisible() {
        return waitUntilElementInvisible(blockedLabel);
    }

    public boolean isUnblockUserButtonVisible() {
        return unblockUserButton.isDisplayed();
    }

    public boolean isUnblockUserButtonInvisible() {
        return waitUntilElementInvisible(unblockUserButton);
    }

    public void tapUnblockUserButton() {
        unblockUserButton.isDisplayed();
        unblockUserButton.click();
    }

    public void tapUnblockButtonAlert() {
        unblockButtonAlert.isDisplayed();
        unblockButtonAlert.click();
    }

    public void openDevices() {
        devicesTab.isDisplayed();
        devicesTab.click();
    }

    public boolean areDevicesVisible(String deviceName) {
        return getDriver().findElement(devices.apply(deviceName)).isDisplayed();
    }

    public void tapDevice(String deviceName) {
        getDriver().findElement(devices.apply(deviceName)).click();
    }

    public boolean isVerifiedShieldVisible() {
        return waitUntilElementVisible(verifiedShield);
    }

    public boolean isStartConversationVisible() {
        return waitUntilElementVisible(startConversationButton);
    }

    public void closeUserProfile() {
        closeButton.isDisplayed();
        closeButton.click();
    }

    public void closeUserProfileConversationDetails() {
        closeButtonBackToConversationDetails.isDisplayed();
        closeButtonBackToConversationDetails.click();
    }

    public void tapEditButton() {
        waitUntilElementVisible(editButton);
        editButton.click();
    }

    public void changeUserRoleAdmin() {
        waitUntilElementVisible(adminButton);
        adminButton.click();
    }

    public boolean isAdminRoleVisible() {
        return waitUntilElementVisible(userRoleAdminEntry);
    }

    public boolean isUnableToStartConversationAlertVisible() {
        return waitUntilElementVisible(unableToStartConversationHeading);
    }
}
