package com.wearezeta.auto.androidreloaded.pages;

import com.wearezeta.auto.common.CommonUtils;
import com.wearezeta.auto.common.misc.Timedelta;
import io.appium.java_client.android.nativekey.AndroidKey;
import io.appium.java_client.android.nativekey.KeyEvent;
import io.appium.java_client.pagefactory.AndroidFindBy;
import org.openqa.selenium.By;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.WebElement;

import java.awt.image.BufferedImage;
import java.time.Duration;
import java.util.List;
import java.util.function.BiFunction;
import java.util.function.Function;

public class ConversationViewPage extends AndroidPage {

    @AndroidFindBy(xpath = "//*[@class='android.widget.TextView']")
    private WebElement anyMessage;

    @AndroidFindBy(xpath = "//*[@class='android.widget.SeekBar']")
    private WebElement audioFileSeekBar;

    @AndroidFindBy(xpath = "//android.view.View[@class='android.view.View'][4]")
    private WebElement audioFileDownloadBottomSheet;

    @AndroidFindBy(xpath = "//*[contains(@content-desc,'audio')]")
    private WebElement playPauseButton;

    @AndroidFindBy(xpath = "//*[@class='android.widget.SeekBar']/following-sibling::*[@class='android.widget.TextView']")
    private WebElement timePlayedAudioFile;

    @AndroidFindBy(xpath = "//*[@class='android.widget.EditText']")
    private WebElement textInputField;

    @AndroidFindBy(xpath = "//*[contains(@text,'Type a message')]")
    private WebElement placeHolderTextInputField;

    @AndroidFindBy(xpath = "//*[@content-desc='Send']")
    private WebElement sendButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Edit the message']/following-sibling::*[@class='android.widget.Button']")
    private WebElement editMessageSendButton;

    @AndroidFindBy(accessibility = "Go back to conversation list")
    private WebElement backButton;

    @AndroidFindBy(xpath="//android.view.View[@content-desc=\"Back button\"]")
    private WebElement backButtonBefore49;

    @AndroidFindBy(xpath = "//*[contains(@text,'REACTIONS')]")
    private WebElement reactionsOption;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Open Message Details']")
    private WebElement messageDetailsOption;

    @AndroidFindBy(xpath = "//*[@text='Show All']")
    private WebElement showAllButton;

    @AndroidFindBy(xpath = "//*[@text='Learn more']")
    private WebElement learnMoreLink;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Delete the message']")
    private WebElement deleteButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Delete for Everyone']")
    private WebElement deleteForEveryoneButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Delete for Me']")
    private WebElement deleteForMeButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Retry']")
    private WebElement retryButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Cancel']")
    private WebElement cancelButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Delete this Message for yourself?']")
    private WebElement deleteForMeText;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Delete for Me']")
    private WebElement deleteForMeConfirmButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Deleted message']")
    private WebElement deletedLabel;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Reply to the message']")
    private WebElement replyOption;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Edit the message']")
    private WebElement editOption;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Copy the message']")
    private WebElement copyOption;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Cancel message reply']")
    private WebElement closeReply;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='You left the conversation.']")
    private WebElement youLeftConvToastMessage;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='All devices of all participants have a valid MLS certificate']")
    private WebElement e2eiVerifiedLabel;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='All of all participants are verified (Proteus)']")
    private WebElement verifiedLabelConversation;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Conversation no longer verified']")
    private WebElement conversationNoLongerVerifiedAlertHeading;

    @AndroidFindBy(accessibility = "Attach new item to conversation")
    private WebElement attachNewFileButton;

    @AndroidFindBy(xpath = "//*[@content-desc='Image message']")
    private WebElement image;

    @AndroidFindBy(xpath = "//*[@content-desc='More options']")
    private WebElement contextMenuImage;

    @AndroidFindBy(xpath = "//*[@text='Download']")
    private WebElement downloadOptionImage;

    @AndroidFindBy(xpath = "//*[@text='Delete']")
    private WebElement deleteOptionImage;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Image message']/following-sibling::*[@text='Tap to download']")
    private WebElement downloadTextAssets;

    @AndroidFindBy(xpath = "//*[contains(@text,'Do you want to open the file')]")
    private WebElement downloadAlert;

    @AndroidFindBy(xpath = "//*[contains(@text,'This will take you to wire://conversation/')]")
    private WebElement alertJoinConversation;

    @AndroidFindBy(xpath = "//*[contains(@text,'This will take you to')]")
    private WebElement linkAlert;

    @AndroidFindBy (xpath = "//*[@text='Open']")
    private WebElement openLinkButton;

    @AndroidFindBy(xpath = "//*[@text='Reset Session']")
    private WebElement resetSessionButton;

    @AndroidFindBy(xpath = "//*[@content-desc='Set timer for self-deleting messages']")
    private WebElement selfDeletingMessagesButton;

    @AndroidFindBy(xpath = "//*[@content-desc=' Self-deleting message']")
    private WebElement selfDeletingMessagesLabel;

    @AndroidFindBy(xpath = "//*[@text='Automatically delete message after:']")
    private WebElement timerOptions;

    @AndroidFindBy(xpath = "//*[@content-desc='Ping']")
    private WebElement pingButton;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Send a ping']")
    private WebElement pingAlert;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Ping']")
    private WebElement pingButtonAlert;

    @AndroidFindBy(xpath = "//*[@text='File']")
    private WebElement attachFileButton;

    @AndroidFindBy(xpath = "//*[@text='Gallery']")
    private WebElement attachPictureButton;

    @AndroidFindBy(xpath = "//*[@text='Audio']")
    private WebElement attachAudioButton;

    @AndroidFindBy(xpath = "//*[@text='Location']")
    private WebElement attachLocationButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Record Audio']")
    private WebElement recordAudioButton;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc='Stop Recording Audio']")
    private WebElement stopRecordingAudioButton;

    @AndroidFindBy(xpath = "//*[@class='android.widget.SeekBar']")
    private WebElement audioRecordingSlider;

    @AndroidFindBy(accessibility = "Send Audio Message")
    private WebElement sendAudioRecording;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text=\"Apply audio filter\"]/preceding-sibling::*[@class='android.widget.CheckBox']")
    private WebElement applyAudioFilterCheckBox;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Play audio\"]")
    private WebElement playButtonAudioRecording;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Pause audio\"]")
    private WebElement pauseButtonAudioRecording;

    @AndroidFindBy(xpath = "//*[@text='Send Anyway']")
    private WebElement sendAnywayButton;

    @AndroidFindBy(xpath="//*[contains(@text,\"Message could not be sent, as the backend of\")]")
    private WebElement errorMessageSendingWhenUnreachable;

    @AndroidFindBy(xpath="//*[contains(@text,\"won't get your message.\")]")
    private WebElement errorMessageSendingWhenOffline;

    @AndroidFindBy(xpath = "//*[contains(@text,'conversation-join')]")
    private WebElement guestLinkConversation;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Receiving images is prohibited']")
    private WebElement receivingImagesProhibitedPlaceholder;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Receiving videos is prohibited']")
    private WebElement receivingVideosProhibitedPlaceholder;

    @AndroidFindBy(xpath = "//android.widget.TextView[@text='Receiving audio messages is prohibited']")
    private WebElement receivingAudioMessagesProhibitedPlaceholder;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Location item\"]")
    private WebElement sharedLocationContainer;

    @AndroidFindBy(xpath = "//android.view.View[@content-desc=\"Filter Conversations\"]")
    private WebElement filterConversationButton;

    @AndroidFindBy(xpath = "//*[@text='Favorites']")
    private WebElement favoritesPageHeading;

    @AndroidFindBy(xpath = "//android.widget.EditText")
    private WebElement folderNameInputField;

    @AndroidFindBy(xpath = "//*[@text='Done']")
    private WebElement doneButtonOnMoveToFolderPage;

    @AndroidFindBy(xpath = "//*[@text='New Folder']")
    private WebElement newFolderButtonOnMoveToFolderPage;

    // Locator below is expecting that the user sends a message first in conversation. Index [2] is for the status icon next to the header in the 1:1 conversation title
    private final Function<String, String> otherUsersStatusIcon1on1 = name -> String.format("(//android.widget.TextView[@text=\"%s\"])[2]/..//*[@resource-id=\"status_indicator\"]", name);

    private final BiFunction<String, String, String> folderToastMessage = (name, folder) -> String.format("//android.widget.TextView[contains(translate(@text, '“”', ''), '%s was moved to %s')]", name, folder);

    private final BiFunction<String, String, String> conversationRemovedToastMessage = (name, folder) -> String.format("//android.widget.TextView[contains(translate(@text, '“”', ''), '%s was removed from %s')]", name, folder);

    private final Function<String, String> systemMessageString = text -> String.format("//android.widget.TextView[contains(@text,\"%s\")]", text);

    private final Function<String, By> systemMessageBy = text -> By.xpath(String.format("//android.widget.TextView[contains(@text,'%s')]", text));

    private final Function <String, String> conversationDetails1On1 = userName -> String.format("//android.view.View[@resource-id=\"User avatar\"]/following-sibling::*[@text='%s']", userName);

    private final Function<String, String> conversationTitleString = userName -> String.format("//*[@content-desc='Start audio call']/../..//*[@text='%s']", userName);

    private final Function<String, By> conversationTitleBy = userName -> By.xpath(String.format("//*[@content-desc='Start audio call']/../..//*[@text='%s']", userName));

    private final Function<String, String> conversationBannerString = text -> String.format("//android.widget.TextView[@text='%s']", text);

    private final Function<String, By> conversationBannerBy = text -> By.xpath(String.format("//android.widget.TextView[@text='%s']", text));

    private final Function<String, String> messageConversationString = text -> String.format("//android.widget.TextView[@text='%s']", text);

    private final Function<String, By> messageConversationBy = text -> By.xpath(String.format("//android.widget.TextView[@text='%s']", text));

    private final Function<String, String> mentionString = text -> String.format("//android.widget.TextView[contains(@text,'%s')]", text);

    private final Function <String, String> editedMessageLabel = text -> String.format("//*[contains(@text,'%s')]/preceding-sibling::*[contains(@text,'Edited on')]", text);

    private final Function<String, String> fileString = name -> String.format("//*[@text='%s']/..//*[@content-desc='Image message']", name);

    private final Function<String, By> fileBy = name -> By.xpath(String.format("//android.widget.TextView[@text='%s']", name));

    private final Function<String, String> fileProhibitedString = name -> String.format("//*[@text='%s']/following-sibling::*[@text='Receiving files is prohibited']", name);

    private final Function<String, By> fileProhibitedBy = name -> By.xpath(String.format("//*[@text='%s']/following-sibling::*[@text='Receiving files is prohibited']", name));

    private final Function<String, String> reactionToSend = reaction -> String.format("//*[@class='android.widget.Button']/preceding-sibling::*[@text='%s']", reaction);

    private final BiFunction<String, Integer, String> reactionAndAmount = (reaction, amount) -> String.format("//*[@text='%s']/following-sibling::*[@text='%s']", reaction, amount);

    private final BiFunction<String, String, String> reactionToUserMessageString = (user, reaction) -> String.format("//*[@text='%s']/..//*[@text='%s']", user, reaction);

    private final BiFunction<String, String, By> reactionToUserMessageBy = (user, reaction) -> By.xpath(String.format("//*[@text='%s']/..//*[@text='%s']", user, reaction));

    private final Function<String, String> replyPreview = message -> String.format("//android.view.View[@content-desc='Cancel message reply']/../following-sibling::*[@text='%s']", message);

    private final BiFunction<String, String, String> replyConversationView = (reply, message) -> String.format("//*[@text='%s']/..//*[@text='%s']", reply, message);

    private final BiFunction<String, String, String> replyConversationViewMention = (reply, mention) -> String.format("//*[@text='%s']/..//*[contains(@text,'%s')]", reply, mention);

    private final Function <String, String> selectedTimer = timer -> String.format("//*[@text='%s']/following-sibling::*[@class='android.view.View']", timer);

    private final Function <String, String> userInMentionList = user -> String.format("//android.view.View[@resource-id=\"User avatar\"]/following-sibling::*[@text='%s']", user);

    private final Function <String, String> pollButton = buttonName -> String.format("//*[@class='android.widget.Button']/preceding-sibling::*[@text='%s']", buttonName);

    private final Function <String, By> pollButtonProgressBar = buttonName -> By.xpath(String.format("//*[@text='%s']/following-sibling::*[@class='android.widget.ProgressBar']", buttonName));

    private final Function <String, String> pollMessage = message -> String.format("//*[contains(@text,'%s')]", message);

    private final Function <String, String> statusIconMessage = user -> String.format("//android.widget.TextView[@text=\"%s\"]/..//*[@resource-id=\"status_indicator\"]", user);

    private final Function<String, String> textString = text -> String.format("//*[@text=\"%s\"]", text);

    private final Function<String, By> textBy = text -> By.xpath(String.format("//*[@text=\"%s\"']", text));

    private final Function<String, String> confirmationAlert = alertText -> String.format("//android.widget.TextView[contains(@text, \"%s\")]", alertText);

    public ConversationViewPage(WebDriver driver) {
        super(driver);
    }

    public void longtapaudioRecordingSlider() {
        if (waitUntilElementVisible(audioRecordingSlider)) {
            try {
                longTapWithActionsAPI(audioRecordingSlider);
            } catch (InterruptedException e) {
                throw new RuntimeException("Failed to long tap audio message button", e);
            }
        } else {
            throw new RuntimeException("Audio message button is not clickable");
        }
    }

    public boolean isConversationVisible(String user) {
        return getDriver().findElement(By.xpath(conversationTitleString.apply(user))).isDisplayed();
    }

    public boolean isLocationMapVisible() {
        return sharedLocationContainer.isDisplayed();
    }

    public boolean isLocationMapInvisible() {
        return waitUntilElementInvisible(sharedLocationContainer);
    }

    public boolean isConversationInvisible(String user) {
        return isLocatorInvisible(conversationTitleBy.apply(user));
    }

    public boolean isBannerInvisible(String text) {
        return isLocatorInvisible(conversationBannerBy.apply(text));
    }

    public void openConversationDetails(String user) {
        getDriver().findElement(By.xpath(conversationTitleString.apply(user))).isDisplayed();
        getDriver().findElement(By.xpath(conversationTitleString.apply(user))).click();
    }

    public void tapConversationDetails(String userName) {
        getDriver().findElement(By.xpath(conversationDetails1On1.apply(userName))).isDisplayed();
        getDriver().findElement(By.xpath(conversationDetails1On1.apply(userName))).click();
    }

    public boolean isBannerVisible(String text) {
        return getDriver().findElement(By.xpath(conversationBannerString.apply(text))).isDisplayed();
    }

    public boolean isTextDisplayed(String text) {
        return getDriver().findElement(By.xpath(textString.apply(text))).isDisplayed();
    }

    public boolean isTextInvisible(String text) {
        return isLocatorInvisible(textBy.apply(text));
    }

    public boolean isPollMessageDisplayed(String text) {
        return getDriver().findElement(By.xpath(pollMessage.apply(text))).isDisplayed();
    }

    public boolean isPollButtonVisible(String buttonName) {
        return getDriver().findElement(By.xpath(pollButton.apply(buttonName))).isDisplayed();
    }

    public void tapPollButton(String buttonName) {
        getDriver().findElement(By.xpath(pollButton.apply(buttonName))).isDisplayed();
        getDriver().findElement(By.xpath(pollButton.apply(buttonName))).click();
    }

    public boolean isPollButtonSelected(String buttonName) {
        return isLocatorInvisible(pollButtonProgressBar.apply(buttonName), Timedelta.ofSeconds(5));
    }

    public boolean isYouLeftConvToastMessageVisible() {
        return waitUntilElementVisible(youLeftConvToastMessage);
    }

    public boolean isSystemMessageVisible(String message) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(systemMessageString.apply(message))));
    }

    public boolean isSystemMessageInvisible(String message) {
        final By locator = systemMessageBy.apply(message);
        return isLocatorInvisible(locator);
    }

    public boolean isOnlyOneSystemMessageDisplayed(String message) {
        final List<WebElement> systemMessages = getDriver().findElements(By.xpath(systemMessageString.apply(message)));
        return systemMessages.size() == 1;
    }

    public boolean isConversationVerified() {
        return waitUntilElementVisible(verifiedLabelConversation);
    }

    public boolean isAudioBottomSheetVisible() {
        return waitUntilElementVisible(audioFileDownloadBottomSheet);
    }


    public boolean isConversationNotVerified() {
        return waitUntilElementInvisible(verifiedLabelConversation);
    }

    public boolean isConversationNoLongerVerifiedAlertVisible() {
        return waitUntilElementVisible(conversationNoLongerVerifiedAlertHeading);
    }

    public void tapSendAnywayButtonDegradationAlert() {
        sendAnywayButton.click();
    }

    public void tapCancelButtonDegradationAlert() {
        cancelButton.click();
    }

    public boolean isErrorMessageSendingUnreachableBackendVisible() {
        return waitUntilElementVisible(errorMessageSendingWhenUnreachable);
    }

    public boolean isErrorParticipantWontGetMessageVisible() {
        return waitUntilElementVisible(errorMessageSendingWhenOffline);
    }

    public boolean isE2eiLabelVisible() {
        return e2eiVerifiedLabel.isDisplayed();
    }

    public void closeConversationView() {
        //ToDo: Remove backButtonBefore49 once upgrade tests run with 4.9.0 version or higher
        if (isElementInvisible(backButton, Timedelta.ofSeconds(1))) {
            backButtonBefore49.click();
        } else {
            backButton.click();
        }
    }

    public boolean isStatusOtherUser1on1Visible(String name) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(otherUsersStatusIcon1on1.apply(name))));
    }

    public boolean isStatusOtherUserGroupMessageVisible(String user) {
        return getDriver().findElement((By.xpath(statusIconMessage.apply(user)))).isDisplayed();
    }

    public boolean isShowAllButtonVisible() {
        return showAllButton.isDisplayed();
    }

    public void tapShowAllButton() {
        showAllButton.click();
    }

    public void tapLearnMoreLink() {
        learnMoreLink.click();
    }

    public boolean isAlertVisible(String text) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(text))));
    }

    public boolean isAlertTextVisible(String text) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(textString.apply(text))));
    }

    public void tapSendAnywayButton() {
        sendAnywayButton.click();
    }

    // Messaging

    public void tapTextInputField() {
        textInputField.isDisplayed();
        textInputField.click();
    }

    public boolean isTextInputFieldVisible() {
        return waitUntilElementVisible(textInputField);
    }

    public void sendKeysTextInputField(String text) {
        textInputField.isDisplayed();
        textInputField.click();
        textInputField.sendKeys(text);
    }

    public void sendGenericMessageWithChars(int numberOfChars) {
        String messageToSent = createGenericMessageWithChars(numberOfChars);
        textInputField.isDisplayed();
        textInputField.click();
        textInputField.sendKeys(messageToSent);
    }

    public void sendGenericMessage(String message) {
        textInputField.isDisplayed();
        textInputField.click();
        textInputField.sendKeys(message);
    }

    public void pasteCopiedText() {
        getDriver().pressKey(new KeyEvent(AndroidKey.PASTE));
    }

    public void editMessage(String text) {
        textInputField.isDisplayed();
        textInputField.clear();
        textInputField.sendKeys(text);
    }

    public void sendEditedMessage() {
        editMessageSendButton.isDisplayed();
        editMessageSendButton.click();
    }

    public boolean isEditedLabelVisible(String message) {
        return getDriver().findElement(By.xpath(editedMessageLabel.apply(message))).isDisplayed();
    }

    public String getTextEditedLabel(String message) {
        return getDriver().findElement(By.xpath(editedMessageLabel.apply(message))).getText();
    }

    public String createGenericMessageWithChars(int numberOfChars) {
        return CommonUtils.generateRandomString(numberOfChars);
    }

    public boolean isUserInMentionList(String user) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(userInMentionList.apply(user))));
    }

    public void tapUserInMentionList(String user) {
        getDriver().findElement(By.xpath(userInMentionList.apply(user))).click();
    }

    public void tapSendButton() throws InterruptedException {
        sendButton.isDisplayed();
        sendButton.click();
        Thread.sleep(500);
    }

    public boolean isMessageDisplayed(String message) {
        final By locator = messageConversationBy.apply(message);
        return waitUntilLocatorIsDisplayed(locator, Duration.ofSeconds(10));
    }

    public boolean isMessageInvisible(String message) {
        final By locator = messageConversationBy.apply(message);
        return isLocatorInvisible(locator);
    }

    public boolean isMentionDisplayed(String mention) {
        return getDriver().findElement(By.xpath(mentionString.apply(mention))).isDisplayed();
    }

    public boolean isGuestLinkVisibleInConversation() {
        return waitUntilElementVisible(guestLinkConversation);
    }

    public void longTapOnMessage(String message) {
        final WebElement locator = getDriver().findElement(By.xpath(messageConversationString.apply(message)));
        longTap(locator);
    }

    public void tapOnLink(String link) {
        final WebElement locator = getDriver().findElement(By.xpath(messageConversationString.apply(link)));
        locator.click();
    }

    public boolean isLinkJoinConversationAlertVisible() {
        return alertJoinConversation.isDisplayed();
    }

    public String getTextLinkAlert() {
        return linkAlert.getText();
    }

    public void tapOnLinkAlert() {
        openLinkButton.click();
    }

    public void scrollToMessage(String message, int widthEndPercent, int heightEndPercent) {
        final By locator = messageConversationBy.apply(message);
        scrollUntilElementVisible(locator, widthEndPercent, heightEndPercent);
    }

    public String getTextMessage() {
        waitUntilElementVisible(anyMessage);
        String conversationMessage = anyMessage.getText();
        log.info(conversationMessage);
        return conversationMessage;
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

    // Self Deleting Messages

    public void tapOnSelfDeletingMessagesButton() {
        waitUntilElementVisible(selfDeletingMessagesButton);
        selfDeletingMessagesButton.click();
    }

    public boolean isSelfDeletingButtonVisible() {
        return selfDeletingMessagesButton.isDisplayed();
    }

    public boolean isSelfDeletingButtonInvisible() {
        return isElementInvisible(selfDeletingMessagesButton, Timedelta.ofSeconds(2));
    }

    public boolean isTimerOptionsInvisible() {
        return isElementInvisible(timerOptions, Timedelta.ofSeconds(2));
    }

    public boolean isSelfDeletingLabelVisible() {
        return waitUntilElementVisible(selfDeletingMessagesLabel);
    }

    public boolean isTimerButtonVisible(String timer) {
        final WebElement timerButton = getDriver().findElement(By.xpath(textString.apply(timer)));
        return timerButton.isDisplayed();
    }

    public boolean isTimerButtonSelected(String timer) {
        final WebElement timerButtonSelected = getDriver().findElement(By.xpath(selectedTimer.apply(timer)));
        return timerButtonSelected.isDisplayed();
    }

    public void tapOnTimerButton(String timer) {
        final WebElement timerButton = getDriver().findElement(By.xpath(textString.apply(timer)));
        timerButton.click();
    }

    public void sendKeysSelfDeletingMessageTextInputField(String message) {
        textInputField.isDisplayed();
        textInputField.click();
        textInputField.sendKeys(message);
    }

    // Pings

    public void tapPingButton() {
        pingButton.click();
    }

    public boolean isPingAlertDisplayed() {
        return waitUntilElementVisible(pingAlert);
    }

    public void tapPingButtonAlert() {
        pingButtonAlert.click();
    }

    public boolean isConfirmationAlertVisible(String alertText) {
        return waitUntilElementClickable(getDriver().findElement(By.xpath(confirmationAlert.apply(alertText))));
    }

    // Reactions

    public boolean isReactionsOptionsVisible() {
        return waitUntilElementVisible(reactionsOption);
    }

    public void tapOnReactionToSend(String reaction) {
        getDriver().findElement(By.xpath(reactionToSend.apply(reaction))).isDisplayed();
        getDriver().findElement(By.xpath(reactionToSend.apply(reaction))).click();
    }

    public boolean isReactionAndCorrectAmountDisplayed(String reaction, Integer amount) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(reactionAndAmount.apply(reaction, amount))));
    }

    public boolean isReactionToUserMessageVisible(String user, String reaction) {
        return getDriver().findElement(By.xpath(reactionToUserMessageString.apply(user, reaction))).isDisplayed();
    }

    public boolean isReactionToUserMessageInvisible(String user, String reaction) {
        return isLocatorInvisible(reactionToUserMessageBy.apply(user, reaction));
    }

    public void tapOnReactionInConversation(String user, String reaction) {
        getDriver().findElement(By.xpath(reactionToUserMessageString.apply(user, reaction))).isDisplayed();
        getDriver().findElement(By.xpath(reactionToUserMessageString.apply(user, reaction))).click();
    }

    // Message Details

    public boolean isMessageDetailsDisplayed() {
        return waitUntilElementVisible(messageDetailsOption);
    }

    public void tapMessageDetails() {
        messageDetailsOption.isDisplayed();
        messageDetailsOption.click();
    }

    // Delete Messages

    public void tapDeleteButton() {
        deleteButton.isDisplayed();
        deleteButton.click();
    }

    public boolean isDeleteOptionsVisible() {
        return deleteForMeButton.isDisplayed();
    }

    public void tapDeleteForEveryoneButton() {
        deleteForEveryoneButton.isDisplayed();
        deleteForEveryoneButton.click();
    }

    public void tapDeleteForMeButton() {
        deleteForMeButton.isDisplayed();
        deleteForMeButton.click();
    }

    public boolean isDeleteForMeTextVisible() {
        return deleteForMeText.isDisplayed();
    }

    public void tapDeleteForMeConfirmButton() {
        deleteForMeConfirmButton.isDisplayed();
        deleteForMeConfirmButton.click();
    }

    public boolean isDeletedLabelVisible() {
        return deletedLabel.isDisplayed();
    }

    // Reply to a message

    public boolean isReplyOptionVisible() {
        return waitUntilElementVisible(replyOption);
    }

    public void tapReplyOption() {
        replyOption.isDisplayed();
        replyOption.click();
    }

    public boolean isCloseReplyVisible() {
        return waitUntilElementVisible(closeReply);
    }

    public boolean isReplyMessagePreviewVisible(String message) {
        return getDriver().findElement(By.xpath(replyPreview.apply(message))).isDisplayed();
    }

    public boolean isReplyToMessageVisible(String reply, String message) {
        return getDriver().findElement(By.xpath(replyConversationView.apply(reply, message))).isDisplayed();
    }

    public boolean isReplyToMentionVisible(String reply, String message) {
        return getDriver().findElement(By.xpath(replyConversationViewMention.apply(reply, message))).isDisplayed();
    }

    // Assets

    public void tapFileSharingButton() {
        attachNewFileButton.isDisplayed();
        attachNewFileButton.click();
    }

    public void tapAttachFile() {
        attachFileButton.click();
    }

    public void tapAttachAudio() {
        attachAudioButton.click();
    }

    public void tapAttachLocation() {
        attachLocationButton.click();
    }

    public void tapAttachPicture() {
        attachPictureButton.click();
    }

    public boolean isImageDisplayed() {
        return waitUntilElementVisible(image);
    }

    public boolean isImageInvisible() {
        return waitUntilElementInvisible(image);
    }

    public void tapImage() {
        image.isDisplayed();
        image.click();
    }

    public void longTapImage() {
        image.isDisplayed();
        longTap(image);
    }

    public boolean isAssetDisplayed(String name) {
        return getDriver().findElement(By.xpath(fileString.apply(name))).isDisplayed();
    }

    public boolean isAudioFileVisible() {
        return waitUntilElementVisible(audioFileSeekBar);
    }

    public boolean isAudioFileInvisible() {
        return waitUntilElementInvisible(audioFileSeekBar);
    }

    public void tapPlayPauseButtonAudioFile(String action) {
        playPauseButton.isDisplayed();
        playPauseButton.click();
    }

    public String getTimeAudioFilePlayed() {
        return timePlayedAudioFile.getText();
    }

   public void tapRecordAudioButton() {
        waitUntilElementClickable(recordAudioButton);
        recordAudioButton.click();
    }

    public void tapStopRecordingAudioButton() {
        stopRecordingAudioButton.click();
    }

    public boolean isStopRecordingAudioButtonInvisible() {
        return waitUntilElementInvisible(stopRecordingAudioButton);
    }

    public boolean isAudioRecordingBeforeSendingVisible() {
        return waitUntilElementVisible(audioRecordingSlider);
    }

    public void tapPlayButtonRecordedAudio() {
        playButtonAudioRecording.click();
    }

    public void tapPauseButtonRecordedAudio() {
        pauseButtonAudioRecording.click();
    }

    public void sendRecordedAudio() {
        sendAudioRecording.click();
    }

    public void applyAudioFilter() {
        applyAudioFilterCheckBox.click();
    }

    public boolean isAudioFilterApplied() {
        return applyAudioFilterCheckBox.getAttribute("checked").equals("true");
    }

    public boolean isAssetInvisible(String name) {
        final By locator = fileBy.apply(name);
        return isLocatorInvisible(locator);
    }

    public void tapAsset(String name) {
        getDriver().findElement(By.xpath(fileString.apply(name))).click();
    }

    public void longtapAsset(String name) {
        final WebElement locator = getDriver().findElement(By.xpath(fileString.apply(name)));
        longTap(locator);
    }

    public boolean isDownloadAlertVisible() {
        return downloadAlert.isDisplayed();
    }

    public boolean isDownloadAlertInvisible() {
        return isElementInvisible(downloadAlert, Timedelta.ofSeconds(1));
    }

    public void tapButtonDownloadAlert(String buttonName) {
        getDriver().findElement(By.xpath(textString.apply(buttonName))).isDisplayed();
        getDriver().findElement(By.xpath(textString.apply(buttonName))).click();
    }

    public boolean isReceivingProhibitedForFileVisible(String name) {
        return getDriver().findElement(By.xpath(fileProhibitedString.apply(name))).isDisplayed();
    }

    public boolean isReceivingProhibitedForImagesVisible() {
        return waitUntilElementVisible(receivingImagesProhibitedPlaceholder);
    }

    public boolean isReceivingProhibitedForVideosVisible() {
        return waitUntilElementVisible(receivingVideosProhibitedPlaceholder);
    }

    public boolean isReceivingProhibitedForAudioMessagesVisible() {
        return waitUntilElementVisible(receivingAudioMessagesProhibitedPlaceholder);
    }

    public boolean isReceivingProhibitedForFileInvisible(String name) {
        return isLocatorInvisible(fileProhibitedBy.apply(name));
    }

    public boolean isContextMenuImageVisible() {
        return contextMenuImage.isDisplayed();
    }

    public void openContextMenuImage() {
        contextMenuImage.isDisplayed();
        contextMenuImage.click();
    }

    public boolean isDownloadOptionImageVisible() {
        return downloadOptionImage.isDisplayed();
    }

    public void tapDownloadImage() {
        downloadOptionImage.isDisplayed();
        downloadOptionImage.click();
    }

    public boolean isDeleteOptionImageVisible() {
        return deleteOptionImage.isDisplayed();
    }

    public void tapDeleteImage() {
        deleteOptionImage.isDisplayed();
        deleteOptionImage.click();
    }

    public boolean isRetryButtonVisible() {
        return retryButton.isDisplayed();
    }

    public void tapRetryButton() {
        retryButton.click();
    }

    public boolean isCancelButtonVisible() {
        return cancelButton.isDisplayed();
    }

    public boolean isReplyButtonInvisible() {
       return isElementInvisible(replyOption, Timedelta.ofSeconds(2));
    }

    public boolean isEditButtonVisible() {
        return waitUntilElementVisible(editOption);
    }

    public boolean isEditButtonInvisible() {
        return isElementInvisible(editOption, Timedelta.ofSeconds(2));
    }

    public void tapEditOption() {
        editOption.isDisplayed();
        editOption.click();
    }

    public boolean isCopyButtonVisible() {
        return waitUntilElementVisible(copyOption);
    }

    public void tapCopyButton() {
        copyOption.click();
    }

    public boolean isCopyButtonInvisible() {
        return isElementInvisible(copyOption, Timedelta.ofSeconds(2));
    }

    public boolean isReactionsButtonInvisible() {
        return isElementInvisible(reactionsOption, Timedelta.ofSeconds(2));
    }

    public void tapCancelButton() {
        cancelButton.isDisplayed();
        cancelButton.click();
    }

    // Reset Session

    public void tapResetSession() {
        resetSessionButton.isDisplayed();
        resetSessionButton.click();
    }

    public BufferedImage getRecentImageScreenshot() {
        waitUntilElementVisible(image);
        return getElementScreenshot(image);
    }

    // Filter

    public boolean isToastMessageDisplayed(String name, String folder) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(folderToastMessage.apply(name, folder))));
    }

    public boolean isRemovedToastMessageDisplayed(String name, String folder) {
        return waitUntilElementVisible(getDriver().findElement(By.xpath(conversationRemovedToastMessage.apply(name, folder))));
    }
    
    public void tapFilterConversationButton() {
        filterConversationButton.isDisplayed();
        filterConversationButton.click();
    }

    public void inputFolderName(String text) {
        waitUntilElementClickable(folderNameInputField);
        folderNameInputField.clear();
        folderNameInputField.sendKeys(text);
    }

    public void tapFilterMenuButton(String buttonName) {
        getDriver().findElement(By.xpath(textString.apply(buttonName))).isDisplayed();
        getDriver().findElement(By.xpath(textString.apply(buttonName))).click();
    }

    public void tapExistingFolder(String buttonName) {
        getDriver().findElement(By.xpath(textString.apply(buttonName))).isDisplayed();
        getDriver().findElement(By.xpath(textString.apply(buttonName))).click();
    }

    public void tapCreateFolderButton(String buttonName) {
        getDriver().findElement(By.xpath(textString.apply(buttonName))).isDisplayed();
        getDriver().findElement(By.xpath(textString.apply(buttonName))).click();
    }

    public void tapDoneButtonOnMoveToFolderPage() {
        waitUntilElementClickable(doneButtonOnMoveToFolderPage);
        doneButtonOnMoveToFolderPage.click();
    }

    public void tapNewFolderButtonOnMoveToFolderPage() {
        waitUntilElementClickable(newFolderButtonOnMoveToFolderPage);
        newFolderButtonOnMoveToFolderPage.click();
    }

    public boolean iSeeFavoritesPageHeading() {
        return waitUntilElementClickable(favoritesPageHeading);
    }

    public List<String> getQRCodeFromRecentImage() {
        return waitUntilElementContainsQRCode(image);
    }
}
