package com.wearezeta.auto.androidreloaded.pages;

import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;
import com.wearezeta.auto.common.misc.Timedelta;

import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class GroupConversationDetailsPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@text='Conversation Details']")
    private WebElement conversationDetailsHeading;

    @AndroidFindBy(xpath = "//*[@class='android.widget.EditText']")
    private WebElement groupNameInputField;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='OK']")
    private WebElement OKButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Open conversation options']")
    private WebElement showMoreOptions;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Show more options\"]")
    private WebElement showMoreOptionsBefore49;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='OPTIONS']")
    private WebElement optionsTab;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='PARTICIPANTS']")
    private WebElement participantsTab;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='SERVICES']")
    private WebElement servicesTab;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Add participants']")
    private WebElement addParticipantsButton;

    @AndroidFindBy(xpath = "//*[@text='Guests']")
    private WebElement guestOptionsEntry;

    @AndroidFindBy(xpath = "//*[@text='Services']/following-sibling::*[@class='android.view.View']")
    private WebElement servicesSwitch;

    @AndroidFindBy(xpath = "//*[@text='Self-deleting messages']")
    private WebElement selfDeletingMessagesOptionGroupDetails;

    @AndroidFindBy(xpath = "//*[@text='Enforce message deletion']/following-sibling::*[@class='android.view.View']")
    private WebElement selfDeletingMessagesToggle;

    @AndroidFindBy(xpath = "//*[@text='Apply']")
    private WebElement applyButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Disable']")
    private WebElement disableButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text=\"Delete Group\"]")
    private WebElement deleteGroup;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Leave Group']")
    private WebElement leaveGroupButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Leave']")
    private WebElement leaveGroupConfirmButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Remove']")
    private WebElement removeGroupButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Move to Archive']")
    private WebElement moveToArchiveButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Archive']")
    private WebElement confirmMoveToArchiveButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Notifications']")
    private WebElement notifications;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Everything']")
    private WebElement notificationEverything;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Calls, mentions and replies']")
    private WebElement notificationMentionsReplies;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Nothing']")
    private WebElement notificationNothing;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Clear Content…']")
    private WebElement clearContentButton;

    @AndroidFindBy(xpath = "//*[@text='Clear content']")
    private WebElement clearContentConfirmButton;

    @AndroidFindBy(xpath = "//android.widget.ImageView[@content-desc='Go back']")
    private WebElement goBack;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Check mark']")
    private WebElement checkMark;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Cipher Suite\"))")
    private WebElement cipherSuiteEntry;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Last Key Material Update\"))")
    private WebElement lastKeyMaterialEntry;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Group State\"))")
    private WebElement groupStateEntry;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Show all participants\"))")
    private WebElement showAllParticipantsButton;

    @AndroidFindBy(uiAutomator = "new UiScrollable(new UiSelector().scrollable(true)).scrollIntoView(new UiSelector().textContains(\"Protocol\"))")
    private WebElement protocolEntry;

    @AndroidFindBy(xpath = "//*[@text='Protocol']/following-sibling::*[@text='MLS']")
    private WebElement textProtocolMLS;

    @AndroidFindBy(xpath = "//*[@content-desc='Back button']")
    private WebElement backButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Close conversation details']")
    private WebElement closeConversationDetailsIcon;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Close button\"]")
    private WebElement closeConversationDetailsIconBefore49;

    private final Function<String, String> notificationChoice = text -> String.format("//android.widget.TextView[@text='%s']", text);

    private final Function<String, String> toastMessage = message -> String.format("//android.widget.TextView[@text='%s']", message);

    private final Function<String, String> groupName = text -> String.format("//android.widget.TextView[@text='%s']", text);

    private final BiFunction<String, String, String> switchStates = (toggle, state) -> String.format("//*[@text='%s']/following-sibling::*[@text='%s']", toggle, state);

    private final Function<String, String> guestOptionsState = state -> String.format("//*[@text='Guests']/following-sibling::*[@text='%s']", state);

    private final Function<String, String> selfDeletingState = state -> String.format("//*[@text='Self-deleting messages']/following-sibling::*[@text='%s']", state);

    private final Function<String, String> groupStateStatus = status -> String.format("//*[@text='Group State']/following-sibling::*[@text='%s']", status);

    private final Function<String, String> userParticipantsListMembersSectionString = user -> String.format("//*[contains(@text,'GROUP MEMBERS')]/..//*[@text=\"%s\"]", user);

    private final Function<String, By> userParticipantsListMembersSectionBy = user -> By.xpath(String.format("//*[contains(@text,'GROUP MEMBERS')]/..//*[@text=\"%s\"]", user));

    private final Function<String, String> userStatusOtherUserIcon = name -> String.format("//android.widget.TextView[@text=\"%s\"]/..//*[@resource-id=\"status_indicator\"]", name);

    private final Function<String, String> textString = text -> String.format("//*[@text='%s']", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text='%s']", text));

    public GroupConversationDetailsPage(WebDriver driver) {
        super(driver);
    }

    public boolean isGroupDetailsPageVisible() {
        return conversationDetailsHeading.isDisplayed();
    }

    public boolean isTextDisplayed(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
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

    public boolean isTextInvisible(String text) {
        return isLocatorInvisible(textBy.apply(text));
    }

    public void tapShowMoreOptionsButton() {
        //ToDo: Remove showMoreOptions49 once upgrade tests run with 4.9.0 version or higher
        if (isElementInvisible(showMoreOptions, Timedelta.ofSeconds(2))) {
            showMoreOptionsBefore49.click();
        } else {
            showMoreOptions.click();
        }
    }

    public void closeGroupDetailsPage() {
        //ToDo: Remove closeConversationDetailsIconBefore49 once upgrade tests run with 4.9.0 version or higher
        if (isElementInvisible(closeConversationDetailsIcon, Timedelta.ofSeconds(1))) {
            closeConversationDetailsIconBefore49.click();
        } else {
            closeConversationDetailsIcon.click();
        }
    }

    // Options section

    public void tapOnOptionsTab() {
        waitUntilElementVisible(optionsTab);
        optionsTab.click();
    }

    public boolean isGroupNameVisible(String text) {
        return getDriver().findElement(By.xpath(groupName.apply(text))).isDisplayed();
    }

    public void tapOnGroupName(String text) {
        getDriver().findElement(By.xpath(groupName.apply(text))).click();
    }

    public void changeGroupName(String newName) {
        groupNameInputField.clear();
        groupNameInputField.sendKeys(newName);
        OKButton.click();
    }

    public boolean isToastMessageDisplayed(String message) {
        return getDriver().findElement(By.xpath(toastMessage.apply(message))).isDisplayed();
    }

    public boolean isDeleteGroupButtonInvisible() {
        return waitUntilElementInvisible(deleteGroup);
    }

    public void tapDeleteGroupButton() {
        deleteGroup.isDisplayed();
        deleteGroup.click();
    }

    public void tapLeaveGroupButton() {
        leaveGroupButton.isDisplayed();
        leaveGroupButton.click();
    }

    public void tapLeaveGroupConfirmButton() {
        leaveGroupConfirmButton.isDisplayed();
        leaveGroupConfirmButton.click();
    }

    public void tapRemoveGroupButton() {
        removeGroupButton.isDisplayed();
        removeGroupButton.click();
    }

    public void tapMoveToArchiveButton() {
        moveToArchiveButton.isDisplayed();
        moveToArchiveButton.click();
    }

    public void tapConfirmMoveToArchiveButton() {
        confirmMoveToArchiveButton.isDisplayed();
        confirmMoveToArchiveButton.click();
    }

    public boolean isNotificationCorrect(String notification) {
        return getDriver().findElement(By.xpath(notificationChoice.apply(notification))).isDisplayed();
    }

    public boolean isEverythingVisible() {
        return waitUntilElementVisible(notificationEverything);
    }

    public void tapNotificationOf(String notification) {
        getDriver().findElement(By.xpath(notificationChoice.apply(notification))).click();
    }

    public void tapNotificationsButton() {
        notifications.isDisplayed();
        notifications.click();
    }

    public boolean isSwitchStateVisible(String toggle, String state) {
        return getDriver().findElement(By.xpath(switchStates.apply(toggle, state))).isDisplayed();
    }

    public boolean isGuestsStateCorrect(String state) {
        return getDriver().findElement(By.xpath(guestOptionsState.apply(state))).isDisplayed();
    }

    public void tapGuestOptions() {
        guestOptionsEntry.isDisplayed();
        guestOptionsEntry.click();
    }

    public void tapOnServicesSwitch() {
        servicesSwitch.click();
    }

    public void tapDisableButton() {
        disableButton.isDisplayed();
        disableButton.click();
    }

    public boolean getStateSelfDeletingMessagesGroup(String state) {
        if (isElementInvisible(selfDeletingMessagesOptionGroupDetails, Timedelta.ofSeconds(1))) {
            scrollToTheBottom();
        }
        return getDriver().findElement(By.xpath(selfDeletingState.apply(state))).isDisplayed();
    }

    public void tapSelfDeletingMessagesOptionGroupDetails() {
        selfDeletingMessagesOptionGroupDetails.click();
    }

    public void tapSelfDeletingMessagesToggle() {
        waitUntilElementVisible(selfDeletingMessagesToggle);
        selfDeletingMessagesToggle.click();
    }

    public void tapOnTimerButton(String timer) {
        final WebElement timerButton = getDriver().findElement(By.xpath(textString.apply(timer)));
        timerButton.click();
    }

    public void tapApplyButton() {
        applyButton.isDisplayed();
        applyButton.click();
    }

    public boolean isProtocolMLSDisplayed() {
        //scroll to protocol
        protocolEntry.isDisplayed();
        return textProtocolMLS.isDisplayed();
    }

    public boolean isProtocolMLSInvisible() {
        return isElementInvisible(textProtocolMLS, Timedelta.ofSeconds(2));
    }

    public boolean isCipherSuiteDisplayed() {
        return cipherSuiteEntry.isDisplayed();
    }

    public boolean isLastKeyMaterialDisplayed() {
        return lastKeyMaterialEntry.isDisplayed();
    }

    public boolean isGroupStateDisplayed() {
        return groupStateEntry.isDisplayed();
    }

    public boolean isGroupStateStatusCorrect(String status) {
        return getDriver().findElement(By.xpath(groupStateStatus.apply(status))).isDisplayed();
    }

    public void tapClearContentButton() {
        clearContentButton.isDisplayed();
        clearContentButton.click();
    }

    public void tapClearContentConfirmButton() {
        clearContentConfirmButton.isDisplayed();
        clearContentConfirmButton.click();
    }

    // Participants section

    public void tapOnParticipantsTab() {
        participantsTab.isDisplayed();
        participantsTab.click();
    }

    public void tapOnServicesTab() {
        servicesTab.isDisplayed();
        servicesTab.click();
    }

    public void tapAddParticipants() {
        addParticipantsButton.isDisplayed();
        addParticipantsButton.click();
    }

    public void scrollToUser(String user, int widthEndPercent, int heightEndPercent) {
        final By locator = userParticipantsListMembersSectionBy.apply(user);
        scrollUntilElementVisible(locator, widthEndPercent, heightEndPercent);
    }

    public boolean isUserVisible(String user) {
        final By locator = userParticipantsListMembersSectionBy.apply(user);
        if (isLocatorInvisible(locator, Timedelta.ofSeconds(2))) {
            log.info("User not visible. Scrolling.");
            scrollToTheBottom();
        }
        return waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(5));
    }

    public boolean isUserInvisible(String user) {
        return isLocatorInvisible(userParticipantsListMembersSectionBy.apply(user));
    }

    public boolean isStatusOtherUserVisible(String user) {
        return getDriver().findElement((By.xpath(userStatusOtherUserIcon.apply(user)))).isDisplayed();
    }

    public void openParticipantProfile(String user) {
        getDriver().findElement(By.xpath(userParticipantsListMembersSectionString.apply(user))).isDisplayed();
        getDriver().findElement(By.xpath(userParticipantsListMembersSectionString.apply(user))).click();
    }

    public void tapShowAllParticipants() {
        showAllParticipantsButton.isDisplayed();
        showAllParticipantsButton.click();
    }

    public void closeGroupParticipantsList() {
        backButton.isDisplayed();
        backButton.click();

    }
}
