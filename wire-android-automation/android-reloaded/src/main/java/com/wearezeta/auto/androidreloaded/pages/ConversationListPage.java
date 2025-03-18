package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.androidreloaded.common.PackageNameHolder;
import com.wearezeta.auto.common.CommonUtils;
import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.time.Duration;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ConversationListPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@resource-id='android:id/statusBarBackground']")
    private WebElement notificationPopUp;

    @AndroidFindBy(xpath = "//*[contains(@text,'service is running')]")
    private WebElement webSocketNotification;

    @AndroidFindBy(xpath = "//*[@text='CONNECTING']")
    private WebElement syncBarConversationList;

    @AndroidFindBy(xpath = "//*[@text='WAITING FOR NETWORK']")
    private WebElement networkBarConversationList;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Consent to share user data']")
    private WebElement headingShareDataAlert;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Agree']")
    private WebElement agreeButtonShareDataAlert;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Decline']")
    private WebElement declineButtonShareDataAlert;

    @AndroidFindBy(xpath = "//*[@text='Setting up Wire']")
    private WebElement settingUpWireText;

    @AndroidFindBy(xpath = "//*[contains(@text,'Welcome')]")
    private WebElement welcomeMessage;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Your profile\"]/following-sibling::*[@resource-id=\"status_indicator\"]")
    private WebElement selfUsersStatusIcon;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Search conversations']")
    private WebElement searchFieldSearchConversation;

    @AndroidFindBy(accessibility = "Search people by name or username")
    private WebElement searchFieldSearchPeople;

    @AndroidFindBy(xpath = "//*[@class='android.widget.EditText']")
    private WebElement searchFieldTextInputField;

    @AndroidFindBy(xpath = "//*[contains(@text,'Welcome To Our New Android App')]")
    private WebElement welcomeToNewAndroidAlert;

    // ToDo: Change locator back to "//*[@text='Conversations']" or "//*[@text='All Conversations']" depending on design decision
    @AndroidFindBy(xpath = "//*[contains(@text,'Conversations')]")
    private WebElement conversationListHeading;

    @AndroidFindBy(xpath = "//*[@content-desc='Main navigation']")
    private WebElement menuButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Your profile']")
    private WebElement userProfileButton;

    @AndroidFindBy(xpath = "//android.view.View[@resource-id=\"User avatar\"]")
    private WebElement userProfileButtonNoPhoto;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Profile picture\"]")
    private WebElement userProfileButtonBefore49;

    @AndroidFindBy(xpath = "//*[@content-desc='Search for people or create a new group']")
    private WebElement startNewConversation;

    @AndroidFindBy(xpath = "//*[@text='Clear Content…']")
    private WebElement clearContentButton;

    @AndroidFindBy(xpath = "//*[@text='Clear content']")
    private WebElement clearContentConfirmButton;

    @AndroidFindBy(xpath = "//*[@text='Block']")
    private WebElement blockOption;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Close new conversation view']")
    private WebElement closeButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Go back to conversation list\"]")
    private WebElement backArrowToConversationList;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Go back to add participants view\"]")
    private WebElement backArrowButtonInsideSearchField;

    @AndroidFindBy(xpath = "//*[@text='Block']")
    private WebElement blockConfirmButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Unblock']")
    private WebElement unblockOption;

    @AndroidFindBy(xpath = "//*[@text='Unblock']")
    private WebElement unblockConfirmButton;

    private final Function<String, String> userConversationNameOnFilterBottomSheet = name -> String.format("//android.widget.TextView[@text=\"%s\"]", name);

    private final Function<String, String> userConversationNameString = name -> String.format("//*[@text='CONVERSATIONS']/..//*[@text=\"%s\"]", name);

    private final Function<String, By> userConversationNameBy = name -> By.xpath(String.format("//*[@text='CONVERSATIONS']/..//*[@text='%s']", name));

    private final Function<String, String> userUnreadConversationNameString = name -> String.format("//*[@text='NEW ACTIVITY']/..//*[@text=\"%s\"]", name);

    private final Function<String, By> userUnreadConversationNameBy = name -> By.xpath(String.format("//*[@text='NEW ACTIVITY']/..//*[@text='%s']", name));

    private final BiFunction<String, String, String> userConversationSubtitle = (user, text) -> String.format("//*[@text=\"%s\"]/following-sibling::*[@text='%s']", user, text);

    private final Function<String, String> unreadIndicator = count -> String.format("//*[@text='NEW ACTIVITY']/..//*[@text='%s']", count);

    private final Function<String, String> userConversationNamePendingLabelString = name -> String.format("//*[@text='%s']/..//android.view.View[@content-desc=\"pending approval of connection request\"]", name);

    private final Function<String, By> userConversationNamePendingLabelBy = name -> By.xpath(String.format("//*[@text='%s']/..//*[@text='pending']", name));

    private final BiFunction<String, String, String> memberIdentifier = (member, identifier) -> String.format("//*[contains(@text,'%s')]/following-sibling::*[@text='%s']", member, identifier);

    private final Function<String, String> userStatusOtherUserIcon = name -> String.format("//android.view.View[@resource-id=\"User avatar\"]/following-sibling::*[@resource-id=\"status_indicator\"]", name);

    private final Function<String, String> convListSearchSuggestionsString = name -> String.format("//android.widget.TextView[@text=\"%s\"]", name);

    private final Function<String, By> convListSearchSuggestionsBy = name -> By.xpath(String.format("//android.widget.TextView[@text='%s']", name));

    private final BiFunction<String, String, String> labelUserConversationList = (user, label) -> String.format("//*[@text='%s']/following-sibling::*[@text='%s']", user, label);

    private final Function<String, String> textString = text -> String.format("//*[@text=\"%s\"]", text);

    private final Function<String, String> textStringContains = text -> String.format("//*[contains(@text,'%s')]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    public ConversationListPage(WebDriver driver) {
        super(driver);
    }

    public void openNotificationCenter() {
        getDriver().openNotifications();
    }

    public void closeNotificationCenter() {
        getDriver().pressKey( new KeyEvent(AndroidKey.BACK));
    }

    public boolean waitUntilWelcomeToNewAndroidAlertVisible() {
        return waitUntilElementVisible(welcomeToNewAndroidAlert);
    }

    public boolean waitUntilConversationListVisible() {
        return waitUntilElementVisible(conversationListHeading, getDefaultLookupTimeoutSeconds() * 2L);
    }

    public void waitUntilWebsocketPopUpIsInvisible() {
        String id = (String) AndroidPage.executeShell(getDriver(), "getprop ro.boot.serialno");
        String trimmedId = id.replace("\n","");
        String packageName = PackageNameHolder.getPackageName();
        log. info("package name: " + packageName);
        log.info("id: " + id);
        if (trimmedId.equals("ce091829205f7a3704") || trimmedId.equals("25181JEGR05249")) {
            openNotificationCenter();
            waitUntilElementVisible(webSocketNotification);
            closeNotificationCenter();
            log.info("Waiting for websocket popup to disappear");
        } else if (packageName.equals("com.wire.android.bund") || packageName.equals("com.wire.android.bund.column3") ) {
            openNotificationCenter();
            waitUntilElementVisible(webSocketNotification);
            closeNotificationCenter();
            log.info("Waiting for websocket popup to disappear");
        }
    }

    public void waitUntilWebsocketNotificationIsDisplayed() {
        final int maxRetries = 20;
        for (int i = 0; i < maxRetries; i++) {
            String output = (String) AndroidPage.executeShell(getDriver(), "dumpsys notification");
            log.info(output);
            if (output.contains("Notification(channel=com.wire.android.persistent_web_socket_channel")) {
                break;
            }
            log.info("waiting for websocket to be available");
            Timedelta.ofSeconds(1).sleep();
        }
    }

    public boolean isConversationListInvisible() {
        return waitUntilElementInvisible(conversationListHeading, Duration.ofSeconds(10));
    }

    public boolean waitUntilSyncBarInvisible() {
        return waitUntilElementInvisible(syncBarConversationList);
    }

    public boolean waitUntilShareDataAlertVisible() {
            log.info("Declining share data alert.");
            return waitUntilElementVisible(headingShareDataAlert, getDefaultLookupTimeoutSeconds() * 2L);
    }

    public void iTapAgreeButtonShareDataAlert() {
         agreeButtonShareDataAlert.click();
    }

    public void iTapDeclineButtonShareDataAlert() {
        waitUntilElementVisible(declineButtonShareDataAlert);
        declineButtonShareDataAlert.click();
    }

    public boolean waitUntilWaitingForNetworkIsInvisible() {
        return waitUntilElementInvisible(networkBarConversationList);
    }

    public boolean waitUntilSetupIsDone() {
        return waitUntilElementInvisible(settingUpWireText, Duration.ofSeconds(30));
    }

    public boolean isWelcomeMessageVisible() {
        return waitUntilElementVisible(welcomeMessage);
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

    public void tapButton(String buttonName) {
        getDriver().findElement(By.xpath(textString.apply(buttonName))).isDisplayed();
        getDriver().findElement(By.xpath(textString.apply(buttonName))).click();
    }

    public void tapMenuButton() {
        waitUntilElementVisible(menuButton);
        menuButton.click();
    }

    public void tapUserProfileButton() {
        // ToDo: Remove userProfileButtonBefore49 once upgrade tests run with 4.9.0 version or higher
        if (isElementInvisible(userProfileButton, Timedelta.ofSeconds(1))) {
            if (isElementInvisible(userProfileButtonBefore49, Timedelta.ofSeconds(1))) {
                userProfileButtonNoPhoto.click();
            } else {
                userProfileButtonBefore49.click();
            }
        } else {
            userProfileButton.click();
        }
    }

    public boolean isConversationDisplayed(String name) {
        return getDriver().findElement(By.xpath(userConversationNameString.apply(name))).isDisplayed();
    }

    public boolean isConversationDisplayedOnFilter(String name) {
        return getDriver().findElement(By.xpath(userConversationNameOnFilterBottomSheet.apply(name))).isDisplayed();
    }

    public boolean isUnreadConversationDisplayed(String name) {
        return getDriver().findElement(By.xpath(userUnreadConversationNameString.apply(name))).isDisplayed();
    }

    public boolean isConversationInvisible(String name) {
        final By locator = userConversationNameBy.apply(name);
        return isLocatorInvisible(locator);
    }

    public boolean isUnreadConversationInvisible(String name) {
        final By locator = userUnreadConversationNameBy.apply(name);
        return isLocatorInvisible(locator);
    }

    public boolean unreadMessagesCount(String count) {
        //FixMe: Add Locator which has 2 regex, like below, or add content-desc for unread indicator label in appcode
        //*[@text='NEW ACTIVITY']/..//*[@text='gm91ykmy']/..//*[@text='1']
        return getDriver().findElement(By.xpath(unreadIndicator.apply(count))).isDisplayed();
    }

    public boolean isConversationPendingStatusVisible(String name) {
        return getDriver().findElement(By.xpath(userConversationNamePendingLabelString.apply(name))).isDisplayed();
    }

    public boolean isConversationPendingStatusInvisible(String name) {
        final By locator = userConversationNamePendingLabelBy.apply(name);
        return isLocatorInvisible(locator);
    }

    public boolean isSubtitleVisible(String user, String text) {
        return getDriver().findElement(By.xpath(userConversationSubtitle.apply(user, text))).isDisplayed();
    }

    public boolean isMemberIdentifierVisible(String member, String identifier) {
        return getDriver().findElement((By.xpath(memberIdentifier.apply(member, identifier)))).isDisplayed();
    }

    public boolean isSelfUserStatusVisible() {
        return waitUntilElementVisible(selfUsersStatusIcon);
    }

    public boolean isStatusOtherUserVisible(String user) {
        return getDriver().findElement((By.xpath(userStatusOtherUserIcon.apply(user)))).isDisplayed();
    }

    public void tapConversationName(final String name) {
        getDriver().findElement(By.xpath(userConversationNameString.apply(name))).click();
    }

    public void tapUnreadConversationName(final String name) {
        getDriver().findElement(By.xpath(userUnreadConversationNameString.apply(name))).click();
    }

    public void longTapConversationName(String name) {
        final WebElement locator = getDriver().findElement(By.xpath(userConversationNameString.apply(name)));
        longTap(locator);
    }

    public void tapStartNewConversation() {
        startNewConversation.isDisplayed();
        startNewConversation.click();
    }

    public void tapBackToConversationList() {
        backArrowToConversationList.isDisplayed();
        backArrowToConversationList.click();
    }

    public void tapClearContentButton() {
        clearContentButton.isDisplayed();
        clearContentButton.click();
    }

    public void clickCloseButton() {
        closeButton.isDisplayed();
        closeButton.click();
    }

    public void tapClearContentConfirmButton() {
        clearContentConfirmButton.isDisplayed();
        clearContentConfirmButton.click();
    }

    public boolean isBlockOptionInvisible() {
        return waitUntilElementInvisible(blockOption);
    }

    public void tapBlockConversationList() {
        blockOption.isDisplayed();
        blockOption.click();
    }

    public void tapBlockConfirmButton() {
        waitUntilElementVisible(blockConfirmButton);
        blockConfirmButton.click();
    }

    public void tapUnblockConversationList() {
        unblockOption.isDisplayed();
        unblockOption.click();
    }

    public void tapUnblockConfirmButton() {
        waitUntilElementVisible(unblockConfirmButton);
        unblockConfirmButton.click();
    }

    // Search

    public void tapSearchPeopleField() {
        searchFieldSearchPeople.isDisplayed();
        searchFieldSearchPeople.click();
    }

    public void tapBackArrowButtonInsideSearchField() {
        backArrowButtonInsideSearchField.isDisplayed();
        backArrowButtonInsideSearchField.click();
    }

    public void tapSearchConversationsField() {
        searchFieldSearchConversation.isDisplayed();
        searchFieldSearchConversation.click();
    }

    public void sendKeysUserNameSearchField(String userName) {
        searchFieldTextInputField.sendKeys(userName);
    }

    public void sendRandomKeysUserNameSearchField() {
        String conversation = CommonUtils.generateRandomString(5);
        searchFieldTextInputField.sendKeys(conversation);
    }

    public boolean isConversationVisibleInSearchResult(String result) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(convListSearchSuggestionsString.apply(result))));
    }

    public boolean isConversationInvisibleInSearchResult(String result) {
        final By locator = convListSearchSuggestionsBy.apply(result);
        return isLocatorInvisible(locator);
    }

    public boolean isLabelForUserVisible(String user, String label) {
        return getDriver().findElement(By.xpath(labelUserConversationList.apply(user, label))).isDisplayed();
    }

    public boolean isLabelForUserInvisible(String user, String label) {
        final By locator = By.xpath(labelUserConversationList.apply(user, label));
        return isLocatorInvisible(locator);
    }
}
