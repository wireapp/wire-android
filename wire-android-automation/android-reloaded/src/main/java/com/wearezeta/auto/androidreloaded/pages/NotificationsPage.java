package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.util.function.BiFunction;
import java.util.function.Function;

public class NotificationsPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@resource-id='android:id/statusBarBackground']")
    private WebElement notificationPopUp;

    @AndroidFindBy(xpath = "//*[@text='Ongoing call…']")
    private WebElement notificationOngoingCall;

    @AndroidFindBy(xpath = "//*[contains(@text,'service is running')]")
    private WebElement webSocketNotification;

    private final BiFunction<String, String, String> notificationFromUserIn1On1String = (user, message) -> String.format("//*[@text='%s']/../../..//*[@text='%s']", user, message);

    private final BiFunction<String, String, By> notificationFromUserIn1On1By = (user, message) -> By.xpath(String.format("//*[@text='%s']/../../..//*[@text='%s']", user, message));

    private final Function<String, String> textString = text -> String.format("//*[contains(@text,'%s')]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[contains(@text,'%s')]", text));
//
//    private final Function<String, String> groupMessageNotificationString = notification -> String.format("//*[contains(@resource-id,'messaging_group_content_container')]//*[@text='%s']", notification);
//
//    private final Function<String, By> groupMessageNotificationBy = notification -> By.xpath(String.format("//*[contains(@resource-id,'messaging_group_content_container')]//*[@text='%s']", notification));
//
//    private final Function<String, String> groupNameNotificationString = groupName -> String.format("//*[contains(@resource-id,'notification_top_line')]//*[@text='%s']", groupName);
//
//    private final Function<String, By> groupNameNotificationBy = groupName -> By.xpath(String.format("//*[contains(@resource-id,'notification_top_line')]//*[@text='%s']", groupName));
//
//    private final Function<String, String> groupMessageSenderNotificationString = notificationSender -> String.format("//*[contains(@resource-id,'messaging_group_content_container')]//*[contains(@text,'%s')]", notificationSender);
//
//    private final Function<String, By> groupMessageSenderNotificationBy = notificationSender -> By.xpath(String.format("//*[contains(@resource-id,'messaging_group_content_container')]//*[contains(@text,'%s')]", notificationSender));

    public NotificationsPage(WebDriver driver) {
        super(driver);
    }

    public void openNotificationCenter() {
        getDriver().openNotifications();
    }

    public void closeNotificationCenter() {
        getDriver().pressKey( new KeyEvent(AndroidKey.BACK));
    }

    public void waitUntilNotificationPopUpIsInvisible() {
        if (isElementPresentAndDisplayed(notificationPopUp)) {
            waitUntilElementInvisible(notificationPopUp);
        }
    }

    public boolean isNotificationFromUserIn1On1Visible(String user) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(user))));
        //return waitUntilElementVisible(getDriver().findElement(By.xpath(notificationFromUserIn1On1String.apply(user, message))));
    }

    public boolean isNotificationMessageIn1On1Visible(String message) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(message))));
    }

    public boolean isNotificationFromUserIn1On1Invisible(String user) {
        final By locator = textBy.apply(user);
        //final By locator = notificationFromUserIn1On1By.apply(user, message);
        return isLocatorInvisible(locator);
    }

    public boolean isNotificationMessageIn1On1Invisible(String message) {
        final By locator = textBy.apply(message);
        //final By locator = notificationFromUserIn1On1By.apply(user, message);
        return isLocatorInvisible(locator);
    }

    public boolean isGroupNotificationVisible(String notification) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(notification))));
        //return waitUntilElementVisible(getDriver().findElement(By.xpath(groupMessageNotificationString.apply(notification))));
    }

    public boolean isGroupNotificationInvisible(String notification) {
        final By locator = textBy.apply(notification);
        //final By locator = groupMessageNotificationBy.apply(notification);
        return isLocatorInvisible(locator);
    }

    public boolean isGroupNameInNotificationVisible(String groupName) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(groupName))));
        //return waitUntilElementVisible(getDriver().findElement(By.xpath(groupNameNotificationString.apply(groupName))));
    }

    public boolean isGroupNameInNotificationInvisible(String groupName) {
        final By locator = textBy.apply(groupName);
        //final By locator = groupNameNotificationBy.apply(groupName);
        return isLocatorInvisible(locator);
    }

    public boolean isSenderNameInGroupMessageVisible(String senderName) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(senderName))));
        //return waitUntilElementVisible(getDriver().findElement(By.xpath(groupMessageSenderNotificationString.apply(senderName))));
    }

    public boolean isSenderNameInGroupMessageInvisible(String senderName) {
        final By locator = textBy.apply(senderName);
        //final By locator = groupMessageSenderNotificationBy.apply(senderName);
        return isLocatorInvisible(locator);
    }

    public boolean isOngoingCallVisible() {
        return waitUntilElementVisible(notificationOngoingCall);
    }

    public void tapOnNotificationFromUserIn1On1(String user, String message) {
        waitUntilElementVisible(getDriver().findElement(By.xpath(notificationFromUserIn1On1String.apply(user, message))));
        getDriver().findElement(By.xpath(notificationFromUserIn1On1String.apply(user, message))).click();
    }

    public void tapOnNotificationFromUserInGroup(String message) {
        waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(message))));
        getDriver().findElement(By.xpath(textString.apply(message))).click();
//      waitUntilElementVisible(getDriver().findElement(By.xpath(groupMessageNotificationString.apply(message))));
//      getDriver().findElement(By.xpath(groupMessageNotificationString.apply(message))).click();
    }

    public boolean iSeeWebSocketRunningMessage() {
        return waitUntilElementVisible(webSocketNotification);
    }
}
