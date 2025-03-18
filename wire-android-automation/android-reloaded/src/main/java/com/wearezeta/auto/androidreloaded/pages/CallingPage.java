package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class CallingPage extends AndroidPage {

    @AndroidFindBy(xpath="//*[@content-desc='Accept call']")
    private WebElement acceptCallButton;

    @AndroidFindBy(xpath="//*[@content-desc='Start audio call']")
    private WebElement startCallButton;

    @AndroidFindBy(xpath="//*[@text='Start a call']")
    private WebElement startCallAlert;

    @AndroidFindBy(xpath="//*[@text='Call']")
    private WebElement startCallButtonAlert;

    @AndroidFindBy(xpath="//*[@content-desc='Unmute call']")
    private WebElement unmuteButton;

    @AndroidFindBy(xpath="//*[@content-desc='Mute call']")
    private WebElement muteButton;

    @AndroidFindBy(xpath="//*[@content-desc='Hang up call']")
    private WebElement hangUpCallButton;

    @AndroidFindBy(xpath = "//*[@content-desc='Turn camera off']")
    private WebElement turnCameraOffButton;

    @AndroidFindBy(xpath = "//*[@content-desc='Turn camera on']")
    private WebElement turnCameraOnButton;

    @AndroidFindBy(xpath = "//*[@content-desc='Drop down arrow']")
    private WebElement minimiseCallButton;

    @AndroidFindBy(xpath = "//*[@text='RETURN TO CALL']")
    private WebElement restoreCallButton;

    @AndroidFindBy(xpath = "//*[@text='Join']")
    private WebElement joinButtonConversation;

    @AndroidFindBy(xpath = "//*[contains(@text,'Calling') or contains(@text,'Incoming call')]")
    private WebElement callNotification1on1;

    @AndroidFindBy(xpath = "//*[contains(@text,'calling') or contains(@text,'Incoming call')]")
    private WebElement callNotificationGroup;

    @AndroidFindBy(className = "android.widget.FrameLayout")
    List<WebElement> videoGrids;

    @AndroidFindBy(xpath = "//*[contains(@text,'Feature unavailable')]")
    private WebElement alertFeatureUnavailable;

    @AndroidFindBy(xpath = "//*[contains(@text,'Upgrade to Enterprise')]")
    private WebElement upgradeToEnterpriseAlert;

    @AndroidFindBy(xpath = "//*[@text='Federated']")
    private WebElement federatedLabel;

    @AndroidFindBy(id = "android:id/content")
    private WebElement fullScreen;

    private final Function<String, String> subTextFeatureUnavailable = subtext -> String.format("//*[contains(@text,'Feature unavailable')]/..//*[@text='%s']", subtext);

    private final Function<String, String> subTextUpgradeToEnterprise = subtext -> String.format("//*[contains(@text,'Upgrade to Enterprise')]/..//*[contains(@text,'%s')]", subtext);

    private final Function<String, By> incomingCallUserName = callerName -> By.xpath(String.format("//*[@text=\"%s\"]/following-sibling::*[@text=\"Ringing…\"]", callerName));

    private final BiFunction<String, String, By> incomingGroupCallUserName = (groupName, userName) -> By.xpath(String.format("//*[contains(@text,'%s')]/following-sibling::*[contains(@text,\"%s\")]", groupName, userName));

    private final Function<String, By> incomingGroupCall = groupName -> By.xpath(String.format("//*[@text=\"%s\"]/following-sibling::*[@text=\"Ringing…\"]", groupName));

    private final Function<String, String> participantTileString = participant -> String.format("//*[@content-desc='Profile picture']/..//*[@text=\"%s\"]", participant);

    private final Function<String, By> participantTileBy = participant -> By.xpath(String.format("//*[@content-desc='Profile picture']/..//*[@text='%s']", participant));

    private final Function<String, String> participantVideoTile = participant -> String.format("//android.widget.TextView[@text=\"%s\"]/../../..//*[@class='android.widget.FrameLayout']", participant);

    private final Function<String, By> connectionTextUserBy = participant -> By.xpath(String.format("//*[@text=\"%s\"]/following-sibling::*[@text='Connecting…']", participant));

    private final Function<String, String> textString = text -> String.format("//*[@text=\"%s\"]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text=\"%s\"]", text));

    public CallingPage(WebDriver driver) {
        super(driver);
    }

    public boolean isTextDisplayed(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public boolean isTextInvisible(String text) {
        return isLocatorInvisible(textBy.apply(text));
    }

    public boolean isIncomingCallFromUserVisible(String user) {
        final By incomingCall = incomingCallUserName.apply(user);
        if (!waitUntilLocatorIsDisplayed(incomingCall, Duration.ofSeconds(5))) {
            getDriver().openNotifications();
            callNotification1on1.click();
        }
        return waitUntilLocatorIsDisplayed(incomingCall, Duration.ofSeconds(20));
    }

    public boolean isIncomingGroupCallVisible(String group ) {
        final By incomingCall = incomingGroupCall.apply(group);
        if (!waitUntilLocatorIsDisplayed(incomingCall, Duration.ofSeconds(5))) {
            getDriver().openNotifications();
            callNotificationGroup.click();
        }
        return waitUntilLocatorIsDisplayed(incomingCall, Duration.ofSeconds(20));
    }

    public boolean isIncomingGroupCallFromUserVisible(String group, String user) {
        final By incomingCall = incomingGroupCallUserName.apply(group, user);
        if (!waitUntilLocatorIsDisplayed(incomingCall, Duration.ofSeconds(5))) {
            getDriver().openNotifications();
            callNotificationGroup.click();
        }
        if (waitUntilLocatorIsDisplayed(incomingCall, Duration.ofSeconds(20))) {
            return waitUntilLocatorIsDisplayed(incomingCall, Duration.ofSeconds(20));
        } else {
            return waitUntilLocatorIsDisplayed(textBy.apply((group)), Duration.ofSeconds(20));
        }
    }

    public void acceptCall() {
        acceptCallButton.isDisplayed();
        acceptCallButton.click();
    }

    public void declineCall() {
        hangUpCallButton.isDisplayed();
        hangUpCallButton.click();
    }

    public void startCall() {
        startCallButton.isDisplayed();
        startCallButton.click();
    }

    public boolean isStartCallAlertDisplayed() {
        return waitUntilElementVisible(startCallAlert);
    }

    public void tapStartCallButtonAlert() {
        startCallButtonAlert.click();
    }

    public boolean isOngoingCallVisible() {
        return waitUntilElementVisible(hangUpCallButton);
    }

    public boolean isOngoingCallInvisible() {
        return waitUntilElementInvisible(hangUpCallButton);
    }

    public boolean isOtherParticipantVisible(String participant) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(participant))));
    }

    public boolean isOtherParticipantInvisible(String participant) {
        final By otherParticipant = textBy.apply(participant);
        return isLocatorInvisible(otherParticipant);
    }

    public boolean isOtherParticipantVideoTileVisible(String participant) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(participantVideoTile.apply(participant))));
    }

    public void tapUnmute() {
        unmuteButton.isDisplayed();
        unmuteButton.click();
    }

    public void turnCameraOn() {
        turnCameraOnButton.isDisplayed();
        turnCameraOnButton.click();
    }

    public void minimiseCall() {
        minimiseCallButton.isDisplayed();
        minimiseCallButton.click();
    }

    public void restoreCall() {
        restoreCallButton.isDisplayed();
        restoreCallButton.click();
    }

    public void tapHangUp() {
        hangUpCallButton.isDisplayed();
        hangUpCallButton.click();
    }

    public boolean isJoinButtonInConversationVisible() {
        return joinButtonConversation.isDisplayed();
    }

    public BufferedImage waitUntilVideoGridContainsQRCode() {
        waitUntilElementContainsQRCode(fullScreen);
        return getElementScreenshot(fullScreen);
    }

    public boolean isFeatureUnavailableAlertVisible() {
        return alertFeatureUnavailable.isDisplayed();
    }

    public boolean isUpgradeToEnterpriseAlertVisible() {
        return upgradeToEnterpriseAlert.isDisplayed();
    }

    public boolean isFeatureUnavailableAlertInvisible() {
        return waitUntilElementInvisible(alertFeatureUnavailable);
    }

    public boolean isSubTextFeatureUnavailableAlertVisible(String text) {
        return getDriver().findElement(By.xpath(subTextFeatureUnavailable.apply(text))).isDisplayed();
    }

    public boolean isSubTextUpgradeToEnterpriseAlertVisible(String text) {
        return getDriver().findElement(By.xpath(subTextUpgradeToEnterprise.apply(text))).isDisplayed();
    }

    // Federation

    public boolean isFederatedLabelVisible() {
        return federatedLabel.isDisplayed();
    }

    public boolean isFederatedLabelInvisible() {
        return isElementInvisible(federatedLabel, Timedelta.ofSeconds(1));
    }
}
