package com.wearezeta.auto.androidreloaded.steps;

import com.wearezeta.auto.androidreloaded.common.AndroidTestContext;
import com.wearezeta.auto.androidreloaded.pages.ConversationViewPage;
import com.wearezeta.auto.common.usrmgmt.ClientUsersManager;
import io.cucumber.java.en.Then;
import io.cucumber.java.en.When;

import java.awt.image.BufferedImage;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

import static org.hamcrest.CoreMatchers.containsString;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

public class ConversationViewPageSteps {

    private final AndroidTestContext context;

    public ConversationViewPageSteps(AndroidTestContext context) {
        this.context = context;
    }

    private ConversationViewPage getConversationViewPage() {
        return context.getPage(ConversationViewPage.class);
    }

    @When("^I see conversation view with \"(.*)\" is in foreground$")
    public void iSeeConversationInForeground(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Conversation title is not correct", getConversationViewPage().isConversationVisible(userName));
    }

    @Then("^I (do not )?see location map container in the conversation view$")
    public void ISeeLocationMapContainer(String shouldNotSee) {
        if (shouldNotSee == null) {
            assertThat("Location map container is not visible.",
                    getConversationViewPage().isLocationMapVisible());
        } else {
            assertThat("Location map container is visible, but should be hidden.",
                    getConversationViewPage().isLocationMapInvisible());
        }
    }

    @When("^I do not see conversation view with \"(.*)\" is in foreground$")
    public void iDoNotSeeConversationInForeground(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Conversation title is not correct", getConversationViewPage().isConversationInvisible(userName));
    }

    @When("^I see group conversation \"(.*)\" is in foreground$")
    public void iSeeGroupConversationInForeground(String groupName) {
        assertThat("Conversation title is not correct", getConversationViewPage().isConversationVisible(groupName));
    }

    @When("^I do not see group conversation \"(.*)\" is in foreground$")
    public void iDoNotSeeGroupConversationInForeground(String groupName) {
        assertThat("Conversation is in foreground.", getConversationViewPage().isConversationInvisible(groupName));
    }

    @When("^I close the conversation view through the back arrow$")
    public void iCloseConversationView() {
        getConversationViewPage().closeConversationView();
    }

    @When("^I see status icon displayed next to the profile picture of user \"(.*)\" in conversation title$")
    public void iSeeStatusIcon1on1Conversation(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Status icon is not displayed.", getConversationViewPage().isStatusOtherUser1on1Visible(user));
    }

    @When("^I see status icon displayed next to the profile picture of user \"(.*)\" in the group conversation$")
    public void iSeeStatusIconForUserMessage(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Status icon is not displayed.", getConversationViewPage().isStatusOtherUserGroupMessageVisible(user));
    }

    @When("^I tap on group conversation title \"(.*)\" to open group details$")
    public void iOpenGroupDetails(String groupName) {
        getConversationViewPage().openConversationDetails(groupName);
    }

    @When("^I open conversation details for 1:1 conversation with \"(.*)\"$")
    public void iTapConversationDetails(String userName) {
        userName = context.getUsersManager().replaceAliasesOccurrences(userName, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationViewPage().tapConversationDetails(userName);
    }

    @When("^I see a banner informing me that \"(.*)\" in the conversation view$")
    public void iSeeBanner(String text) {
        assertThat(String.format("Banner '%s' is not visible.", text), getConversationViewPage().isBannerVisible(text));
    }

    @When("^I see a audio file download bottom sheet is visible$")
    public void iSeeAudioFileDownloadBottomSheet() {
        assertThat("Audio file bottom sheet is visible.", getConversationViewPage().isAudioBottomSheetVisible());
    }

    @When("^I do not see a banner informing me that \"(.*)\" in the conversation view$")
    public void iDoNotSeeBanner(String text) {
        assertThat(String.format("Banner '%s' is visible although it should not.", text), getConversationViewPage().isBannerInvisible(text));
    }

    @Then("^I see classified domain label with text \"(.*)\" in the conversation view$")
    public void iSeeClassifiedBannerTextForUser(String text) {
        assertThat("Classified banner does not have correct text in conversation view.", getConversationViewPage().isTextDisplayed(text));
    }

    @Then("^I do not see classified domain label with text \"(.*)\" in the conversation view$")
    public void iDoNotSeeClassifiedBannerTextForUser(String text) {
        assertThat("Classified banner does not have correct text in conversation view.", getConversationViewPage().isTextInvisible(text));
    }

    @Then("^I see e2ei verified icon for \"(.*)\" group conversation view$")
    public void iSeeE2eiVerifiedLabelForGroup(String user) {
        assertThat(String.format("Verified label for group '%s' is not visible on group details page.", user), getConversationViewPage().isE2eiLabelVisible());
    }

    @Then("^I see e2ei verified icon for user \"(.*)\" in the 1:1 conversation view$")
    public void iSeeE2eiVerifiedLabelForUser(String user) {
        assertThat(String.format("Verified label for user '%s' is not visible on user details page.", user), getConversationViewPage().isE2eiLabelVisible());
    }

    @When("^I see you left conversation toast message$")
    public void iSeeYouLeftConversationToastMessage() {
        assertThat("Text You left the conversation. is not visible", getConversationViewPage().isYouLeftConvToastMessageVisible());
    }

    @When("^I see system message \"(.*)\" in conversation view$")
    public void iSeeSystemMessageInConversation(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("System message is not visible", getConversationViewPage().isSystemMessageVisible(user));
    }

    @When("^I do not see system message \"(.*)\" in conversation view$")
    public void iDoNotSeeSystemMessageInConversation(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("System message is not visible", getConversationViewPage().isSystemMessageInvisible(user));
    }

    @When("^I see \"(.*)\" system message in conversation$")
    public void iSeeSystemMessage(String mesage) {
        assertThat("System message is not visible", getConversationViewPage().isSystemMessageVisible(mesage));
    }

    @Then("^I see the system message \"(.*)\" only once in the conversation$")
    public void iSeeSystemMessageOnce(String message) {
        assertThat("System messages is displayed more than once.", getConversationViewPage().isOnlyOneSystemMessageDisplayed(message));
    }

    // Proteus verification

    @When("^I see that the conversation is verified$")
    public void iSeeConversationIsVerified() {
        assertThat("Conversation is not verified.", getConversationViewPage().isConversationVerified());
    }

    @When("^I see that the conversation is not verified$")
    public void iSeeConversationIsNotVerified() {
        assertThat("Conversation is verified, but should not be verified.", getConversationViewPage().isConversationNotVerified());
    }

    @When("^I see conversation no longer verified alert$")
    public void iSeeConversationNoLongerVerifiedAlert() {
        assertThat("Conversation no longer verified alert is not visible.", getConversationViewPage().isConversationNoLongerVerifiedAlertVisible());
    }

    @When("^I tap send anyway button on degradation alert$")
    public void tapSendAnywayButtonDegradationAlert() {
        getConversationViewPage().tapSendAnywayButtonDegradationAlert();
    }

    @When("^I tap cancel button on degradation alert$")
    public void tapCancelButtonDegradationAlert() {
        getConversationViewPage().tapCancelButtonDegradationAlert();
    }

    // Messaging

    @When("^I tap on the text input field$")
    public void iTapTextInputField() {
        getConversationViewPage().tapTextInputField();
    }

    @When("^I type the message \"(.*)\" into text input field$")
    public void iTypeMessage(String text) {
        getConversationViewPage().isTextInputFieldVisible();
        getConversationViewPage().sendKeysTextInputField(text);
    }

    @When("^I type the mention \"(.*)\" into text input field$")
    public void iTypeMention(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.FIRSTNAME_ALIAS);
        getConversationViewPage().sendKeysTextInputField(user);
    }

    @When("^I type a generic message with (\\d+) characters into text input field$")
    public void iTypeGenericCharsMessage(int numberOfChars) {
        getConversationViewPage().sendGenericMessageWithChars(numberOfChars);
    }

    @When("^I type a generic message with (\\d+) characters into text input field and remember it before I send it$")
    public void iTypeGenericCharsMessageAndRememberIt(int numberOfChars) {
        String messageToSend = getConversationViewPage().createGenericMessageWithChars(numberOfChars);
        context.setLongRandomGeneratedText(messageToSend);
        getConversationViewPage().sendGenericMessage(messageToSend);
    }

    @When("^I paste the copied text into the text input field$")
    public void iPasteCopiedText() {
        getConversationViewPage().pasteCopiedText();
    }

    @When("^I edit my message to \"(.*)\"$")
    public void iEditMessage(String newMessage) {
        getConversationViewPage().editMessage(newMessage);
    }

    @When("^I tap send button for my edit message$")
    public void iTapEditMessageSend() {
        getConversationViewPage().sendEditedMessage();
    }

    @When("^I see the edited label for the message \"(.*)\"$")
    public void iSeeEditedLabel(String message) {
        getConversationViewPage().isEditedLabelVisible(message);
    }

    @When("^I see the time and date when the message \"(.*)\" was edited$")
    public void iSeeDateAndTimeStampEditedMessage(String message) {
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("d MMM yyyy");
        LocalDateTime currentTime = LocalDateTime.now();
        String date = currentTime.format(formatter);
        if (!getConversationViewPage().getTextEditedLabel(message).contains(date)) {
            formatter = DateTimeFormatter.ofPattern("d");
            currentTime = LocalDateTime.now();
            date = currentTime.format(formatter);
        }
        assertThat("Date is not the same.", getConversationViewPage().getTextEditedLabel(message), containsString(date));
    }

    @When("^I see user \"(.*)\" in mention list$")
    public void iSeeUserInMentionList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationViewPage().isUserInMentionList(user);
    }

    @When("^I select user \"(.*)\" from mention list$")
    public void iSelectUserFromMentionList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationViewPage().tapUserInMentionList(user);
    }

    @When("^I see that my long generic sent message is not the same as my remembered message$")
    public void iSeeGenericMessageNotTheSame() {
        String actualMessage = context.getLongRandomGeneratedText();
        String finalMessage = getConversationViewPage().getTextMessage();
        assertThat("Message is the same as sent message.", finalMessage, not(equalTo(actualMessage)));
    }

    @When("^I tap send button$")
    public void iTapSendButton() throws InterruptedException {
        getConversationViewPage().tapSendButton();
    }

    @When("^I see the message \"(.*)\" in current conversation$")
    public void iSeeMessage(String message) {
        assertThat("Message is not displayed.", getConversationViewPage().isMessageDisplayed(message));
    }

    @When("^I see guest link is displayed in the current conversation$")
    public void iSeeGuestLinkIsDisplayedInConversation() {
        assertThat("Guest link is not displayed.", getConversationViewPage().isGuestLinkVisibleInConversation());
    }

    @When("^I see the self deleting message hint \"(.*)\" in current conversation$")
    public void iSeeSelfDeletingMessageHint(String message) {
        message = context.getUsersManager().replaceAliasesOccurrences(message, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Message is not displayed.", getConversationViewPage().isMessageDisplayed(message));
    }

    @When("^I do not see the self deleting message hint \"(.*)\" in current conversation$")
    public void iDoNotSeeSelfDeletingMessageHint(String message) {
        message = context.getUsersManager().replaceAliasesOccurrences(message, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Message is not displayed.", getConversationViewPage().isMessageInvisible(message));
    }

    @When("^I see the self deleting message button in current conversation$")
    public void iSeeSelfDeletingMessageButton() {
        assertThat("Self deleting messages button is not displayed.", getConversationViewPage().isSelfDeletingButtonVisible());
    }

    @When("^I do not see the self deleting message button in current conversation$")
    public void iDoNotSeeSelfDeletingMessageButton() {
        assertThat("Self deleting messages button is displayed.", getConversationViewPage().isSelfDeletingButtonInvisible());
    }

    @When("^I see the last mention is \"(.*)\" in current conversation$")
    public void iSeeMention(String mention) {
        mention = context.getUsersManager().replaceAliasesOccurrences(mention, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Message is not displayed.", getConversationViewPage().isMentionDisplayed(mention));
    }

    @When("^I do not see the message \"(.*)\" in current conversation$")
    public void iDoNotSeeMessage(String message) {
        assertThat("Message is displayed but it should not.", getConversationViewPage().isMessageInvisible(message));
    }

    @When("^I see a message is displayed in the conversation view$")
    public void iSeeAMessageInConversation() {
        // This assertion checks that we see any message in the conversation view
        assertThat("Sent message is not displayed in the conversation view.", getConversationViewPage().getTextMessage(), not(""));
        // When there is no message at all in the conversation view, then the locator for grabbing the text message will pass as the text in the text input field is the same as when a message is sent.
        // Therefore, the second assertion will verify that the text we found is not the one in the text input field.
        assertThat("No message is displayed in the conversation view.", getConversationViewPage().getTextMessage(), not(containsString("Type a message")));
    }

    @When("^I do not see the remembered message displayed in the conversation view$")
    public void iDoNotSeeAMessageInConversation() {
        String message = context.getLongRandomGeneratedText();
        assertThat("Sent message is displayed in the conversation view.", getConversationViewPage().getTextMessage(), not(containsString(message)));
    }

    @When("^I long tap on the message \"(.*)\" in current conversation$")
    public void iLongTapMessage(String message) {
        getConversationViewPage().longTapOnMessage(message);
    }

    @When("^I tap on the link \"(.*)\" in current conversation$")
    public void iTapOnLink(String link) {
        getConversationViewPage().tapOnLink(link);
    }

    @When("^I see an alert informing me that I will be forwarded to \"(.*)\" in my browser")
    public void iSeeLinkAlert(String link) {
        assertThat("Alert does not contain correct link or is not displayed.", getConversationViewPage().getTextLinkAlert(), containsString(link));
    }

    @When("^I tap open button on the link alert$")
    public void iOpenLinkOnAlert() {
        getConversationViewPage().tapOnLinkAlert();
    }

    @When("^I scroll up to the message \"(.*)\"$")
    public void iScrollUpToMessage(String message) {
        getConversationViewPage().scrollToMessage(message, 0, 9);
    }

    @When("^I scroll down to the message \"(.*)\"$")
    public void iScrollDownToMessage(String message) {
        getConversationViewPage().scrollToMessage(message, 0, -1);
    }

    // Polls

    @When("^I see the poll for \"(.*)\" is displayed in the conversation$")
    public void isPollMessageDisplayed(String pollMessage) {
        assertThat("Poll message is not displayed.", getConversationViewPage().isPollMessageDisplayed(pollMessage));
    }

    @When("^I see the button \"(.*)\" in the poll$")
    public void iSeePollButton(String buttonName) {
        getConversationViewPage().isPollButtonVisible(buttonName);
    }

    @When("^I tap the button \"(.*)\" in the poll$")
    public void iTapPollButton(String buttonName) {
        getConversationViewPage().tapPollButton(buttonName);
    }

    @When("^I see the button \"(.*)\" is selected$")
    public void iSeePollButtonSelected(String buttonName) {
        getConversationViewPage().isPollButtonSelected(buttonName);
    }

    // Self Deleting Messages

    @When("^I tap on self deleting messages button$")
    public void iTapOnSelfDeletingMessagesButton() {
        getConversationViewPage().tapOnSelfDeletingMessagesButton();
    }

    @When("^I do not see self deleting timer options$")
    public void iDoNotSeeTimerOptions() {
        assertThat("Timer options are  visible.", getConversationViewPage().isTimerOptionsInvisible());
    }

    @When("^I see (.*) timer button$")
    public void iSeeTimerButton(String timer) {
        assertThat(String.format("'%s' timer button is not displayed.", timer), getConversationViewPage().isTimerButtonVisible(timer));
    }

    @When("^I see self deleting message label in text input field$")
    public void iSeeSelfDeletingLabel() {
        assertThat(" Self deleting label is not displayed.", getConversationViewPage().isSelfDeletingLabelVisible());
    }

    @When("^I see (.*) timer button is currently selected$")
    public void iSeeTimerButtonSelected(String timer) {
        assertThat(String.format("'%s' timer button is not selected.", timer), getConversationViewPage().isTimerButtonSelected(timer));
    }

    @When("^I tap on (.*) timer button$")
    public void iTapOnTimerButton(String timer) {
        getConversationViewPage().tapOnTimerButton(timer);
    }

    @When("^I type the self deleting message \"(.*)\" into text input field$")
    public void iTypeSelfDeletingMessage(String message) {
        getConversationViewPage().sendKeysSelfDeletingMessageTextInputField(message);
    }

    // Pings

    @When("^I tap on ping button$")
    public void iTapPingButton() {
        getConversationViewPage().tapPingButton();
    }

    @When("^I see ping alert$")
    public void iSeePingAlert() {
        assertThat("Start call alert is not displayed.", getConversationViewPage().isPingAlertDisplayed());
    }

    @When("^I tap on ping button alert$")
    public void iTapPingButtonAlert() {
        getConversationViewPage().tapPingButtonAlert();
    }

    // Reactions

    @When("^I see reactions options$")
    public void iSeeReactionsOptions() {
        assertThat("Reactions option is not visible.", getConversationViewPage().isReactionsOptionsVisible());
    }

    @When("^I do not see reactions options$")
    public void iDoNotSeeReactionsOptions() {
        assertThat("Reactions option is visible.", getConversationViewPage().isReactionsButtonInvisible());
    }

    @When("^I tap on (.*) icon$")
    public void iTapOnReaction(String reaction) {
        getConversationViewPage().tapOnReactionToSend(reaction);
    }

    @When("^I tap on \"(.*)\" from user (.*) message$")
    public void iTapOnReactionToUserMessage(String reaction, String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        getConversationViewPage().tapOnReactionInConversation(user, reaction);
    }

    @When("^I see a \"(.*)\" from (\\d+) users? as reaction to my message$")
    public void iSeeReactionAndAmount(String reaction, Integer amount) {
        assertThat("Correct Reaction and amount is not displayed.", getConversationViewPage().isReactionAndCorrectAmountDisplayed(reaction, amount));
    }

    @When("^I see a \"(.*)\" from (\\d+) users? as reaction to user (.*) message$")
    public void iSeeReactionAndAmountToUserMessage(String reaction, Integer amount, String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Correct Reaction and amount is not displayed.", getConversationViewPage().isReactionAndCorrectAmountDisplayed(reaction, amount));
        assertThat("Reaction is not displayed for the correct user message.", getConversationViewPage().isReactionToUserMessageVisible(user, reaction));
    }

    @When("^I do not see a \"(.*)\" to user (.*) message$")
    public void iDoNotSeeReactionAndAmountToUserMessage(String reaction, String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Reaction is not displayed for the correct user message.", getConversationViewPage().isReactionToUserMessageInvisible(user, reaction));
    }

    // Message Details

    @When("^I see message details option$")
    public void iSeeMessageDetailsOption() {
        assertThat("Message Details option is not displayed.", getConversationViewPage().isMessageDetailsDisplayed());
    }

    @When("^I tap on message details option$")
    public void iTapOnMessageDetailsOption() {
        getConversationViewPage().tapMessageDetails();
    }

    // Delete Messages

    @When("^I tap delete button$")
    public void iTapDeleteButton() {
        getConversationViewPage().tapDeleteButton();
    }

    @When("^I see delete options$")
    public void iSeeDeleteOptions() {
        assertThat("Delete options menu is not visible", getConversationViewPage().isDeleteOptionsVisible());
    }

    @When("^I tap delete for everyone button$")
    public void iTapDeleteForEveryoneButton() {
        getConversationViewPage().tapDeleteForEveryoneButton();
    }

    @When("^I tap delete for me button$")
    public void iTapDeleteForMeButton() {
        getConversationViewPage().tapDeleteForMeButton();
    }

    @When("^I see delete for me text$")
    public void iSeeDeleteForMeText() {
        assertThat("Delete for me text is not visible", getConversationViewPage().isDeleteForMeTextVisible());
    }

    @When("^I tap delete for me confirm button$")
    public void iTapDeleteForMeConfirmButton() {
        getConversationViewPage().tapDeleteForMeConfirmButton();
    }

    @When("^I see deleted label$")
    public void iSeeDeletedLabel() {
        assertThat("Delete label is not visible", getConversationViewPage().isDeletedLabelVisible());
    }

    // Replies

    @When("^I see reply option$")
    public void iSeeReplyOption() {
        assertThat("Reply Option is not visible.", getConversationViewPage().isReplyOptionVisible());
    }

    @When("^I tap reply option$")
    public void iTapReply() {
        getConversationViewPage().tapReplyOption();
    }

    @When("^I see the message \"(.*)\" as preview in message input field$")
    public void iSeeReplyPreview(String message) {
        assertThat("Reply is not displayed in message input field.", getConversationViewPage().isCloseReplyVisible());
        assertThat("Reply preview does not contain message.", getConversationViewPage().isReplyMessagePreviewVisible(message));
    }

    @When("^I see the message \"(.*)\" as a reply to message \"(.*)\" in conversation view$")
    public void iSeeReplyToMessage(String reply, String originalMessage) {
        assertThat("Reply is not displayed in conversation view.", getConversationViewPage().isReplyToMessageVisible(reply, originalMessage));
    }

    @When("^I see the message \"(.*)\" as a reply to my mention \"(.*)\" in conversation view$")
    public void iSeeReplyToMention(String reply, String mention) {
        mention = context.getUsersManager().replaceAliasesOccurrences(mention, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Reply is not displayed in conversation view.", getConversationViewPage().isReplyToMentionVisible(reply, mention));
    }

    // Assets

    @When("^I tap file sharing button$")
    public void iTapAttachNewFileButton() {
        getConversationViewPage().tapFileSharingButton();
    }

    @When("^I see sharing option for (.*) is displayed$")
    public void iSeeSharingOptionFor(String option) {
        getConversationViewPage().isTextDisplayed(option);
    }

    @When("^I do not see sharing option for (.*) is displayed$")
    public void iDoNotSeeSharingOptionFor(String option) {
        getConversationViewPage().isTextInvisible(option);
    }

    @When("^I tap on Attach File option$")
    public void iTapAttachFile() {
        getConversationViewPage().tapAttachFile();
    }

    @When("^I tap on Attach Picture option$")
    public void iTapAttachPicture() {
        getConversationViewPage().tapAttachPicture();
    }

    @When("^I tap on Attach Audio option$")
    public void iTapAttachAudio() {
        getConversationViewPage().tapAttachAudio();
    }

    @When("^I tap on Attach Location option$")
    public void iTapAttachLocation() {
        getConversationViewPage().tapAttachLocation();
    }

    @When("^I see an image in the conversation view$")
    public void iSeeImageInConversation() {
        assertThat("Image is not visible", getConversationViewPage().isImageDisplayed());
    }

    @When("^I see an image with QR code \"(.*)\" in the conversation view$")
    public void iSeeImageInConversation(String qrCode) {
        BufferedImage actualImage = getConversationViewPage().getRecentImageScreenshot();
        context.addAdditionalScreenshots(actualImage);

        assertThat("Could not find correct QR code",
                getConversationViewPage().getQRCodeFromRecentImage(),
                hasItem(qrCode));
    }

    @When("^I do not see an image in the conversation view$")
    public void iDoNotSeeImageInConversation() {
        assertThat("Image is visible", getConversationViewPage().isImageInvisible());
    }

    @When("^I tap on the image$")
    public void iTapImage() {
        getConversationViewPage().tapImage();
    }

    @When("^I longtap on the image$")
    public void iLongTapImage() {
        getConversationViewPage().longTapImage();
    }

    @When("^I see context menu for images$")
    public void iSeeContextMenuImages() {
        assertThat("Context Menu is not visible.", getConversationViewPage().isContextMenuImageVisible());
    }

    @When("^I open context menu for images$")
    public void iOpenContextMenuImages() {
        getConversationViewPage().openContextMenuImage();
    }

    @When("^I see a file with name \"(.*)\" in the conversation view$")
    public void iSeeFileInConversation(String name) {
        assertThat("File is not visible", getConversationViewPage().isAssetDisplayed(name));
    }

    @When("^I do not see a file with name \"(.*)\" in the conversation view$")
    public void iDoNotSeeFileInConversation(String name) {
        assertThat("File is not visible", getConversationViewPage().isAssetInvisible(name));
    }

    @When("^I tap on the file with name \"(.*)\" in the conversation view$")
    public void iTapOnFileFileInConversation(String name) {
        getConversationViewPage().tapAsset(name);
    }

    @When("^I longtap on the file with name \"(.*)\" in the conversation view$")
    public void iLongtapOnFileFileInConversation(String name) {
        getConversationViewPage().longtapAsset(name);
    }

    @When("^I see download alert for files$")
    public void iSeeDownloadAlert() {
        assertThat("Download Alert is not visible.", getConversationViewPage().isDownloadAlertVisible());
    }

    @When("^I do not see download alert for files$")
    public void iDoNotSeeDownloadAlert() {
        assertThat("Download Alert is not visible.", getConversationViewPage().isDownloadAlertInvisible());
    }

    @When("^I see (.*) button on download alert$")
    public void iSeeButtonAlert(String buttonName) {
        assertThat(String.format("Button '%s' is not visible.", buttonName), getConversationViewPage().isTextDisplayed(buttonName));
    }

   @When("^I see (.*?)?(?: button)? on filter bottom sheet$")
    public void iSeeFilterMenu(String buttonName) {
        assertThat(String.format("Button '%s' is not visible.", buttonName), getConversationViewPage().isTextDisplayed(buttonName));
    }

    @When("^I do not see (.*?)?(?: button)? on filter bottom sheet$")
    public void iDoNotSeeButton(String buttonName) {
        assertThat(String.format("Button '%s' is visible.", buttonName), getConversationViewPage().isTextInvisible(buttonName));
    }

    @When("^I tap (.*) button on download alert$")
    public void iTapButtonAlert(String buttonName) {
        getConversationViewPage().tapButtonDownloadAlert(buttonName);
    }

    @When("^I tap (.*) button on filter bottom sheet$")
    public void iTapButtonOnFilterMenu(String buttonName) {
        getConversationViewPage().tapFilterMenuButton(buttonName);
    }

    @When("^I get navigated to Favorites page and I see Favorites page heading$")
    public void iSeeFavoritesPageHeading() {
        assertThat("Favorites Page is not displayed", getConversationViewPage().iSeeFavoritesPageHeading());
    }

    @When("^I get navigated to move to folder page and I see (.*) page heading$")
    public void iSeeMoveToFolderPageHeading(String pageHeading) {
        assertThat("Move to Folder Page is not displayed", getConversationViewPage().isTextDisplayed(pageHeading));
    }

    @When("^I get navigated to page (.*) and I see (.*) as the page heading$")
    public void iSeePageHeading(String pageName, String pageHeading) {
        assertThat(String.format("Expected page heading '%s' is not displayed on page '%s'", pageHeading, pageName), getConversationViewPage().isTextDisplayed(pageHeading));
    }

    @When("^I get navigated to new folder page I see (.*) as page heading$")
    public void iSeeNewFolderPage(String pageHeading) {
        assertThat("New Folder page is not displayed as page title", getConversationViewPage().isTextDisplayed(pageHeading));
    }

    @When("^I see (.*) button$")
    public void iSeeButton(String button) {
        assertThat(String.format("'%s' button is not displayed", button), getConversationViewPage().isTextDisplayed(button));
    }

    @When("^I tap New Folder button on move to folder page$")
    public void iTapNewFolderButton() {
        getConversationViewPage().tapNewFolderButtonOnMoveToFolderPage();
    }

    @When("^I see receiving of files is prohibited for file \"(.*)\" in conversation view")
    public void iSeeReceivingProhibitedForFile(String fileName) {
        assertThat("Receiving of files is not prohibited.", getConversationViewPage().isReceivingProhibitedForFileVisible(fileName));
    }

    @When("^I see receiving of images is prohibited in conversation view")
    public void iSeeReceivingProhibitedForImage() {
        assertThat("Receiving of files is not prohibited.", getConversationViewPage().isReceivingProhibitedForImagesVisible());
    }

    @When("^I see receiving of video is prohibited in conversation view")
    public void iSeeReceivingProhibitedForVideo() {
        assertThat("Receiving of files is not prohibited.", getConversationViewPage().isReceivingProhibitedForVideosVisible());
    }

    @When("^I see receiving of audio messages is prohibited in conversation view")
    public void iSeeReceivingProhibitedForAudio() {
        assertThat("Receiving of files is not prohibited.", getConversationViewPage().isReceivingProhibitedForAudioMessagesVisible());
    }

    @When("^I do not see receiving of files is prohibited for file \"(.*)\" in conversation view")
    public void iDoNotSeeReceivingProhibitedForFile(String fileName) {
        assertThat("Receiving of files is not prohibited.", getConversationViewPage().isReceivingProhibitedForFileInvisible(fileName));
    }

    // Audio Files

    @When("^I see an audio file in the conversation view$")
    public void iSeeAudioFileInConversation() {
        assertThat("File is not visible", getConversationViewPage().isAudioFileVisible());
    }

    @When("^I long press on audio slider button$")
    public void IlongPressAudioSlider() {
        getConversationViewPage().longtapaudioRecordingSlider();
    }

    @When("^I do not see an audio file in the conversation view$")
    public void iDoNotSeeAudioFileInConversation() {
        assertThat("File is not visible", getConversationViewPage().isAudioFileInvisible());
    }

    @When("^I tap (play|pause) button on the audio file$")
    public void iTapPlayPauseAudioFile(String action) {
        getConversationViewPage().tapPlayPauseButtonAudioFile(action);
    }

    @When("^I see the time played in the audio file is (.*)$")
    public void iSeeTimeAudioFile(String time) {
        assertThat("Time in audio file is not correct.", getConversationViewPage().getTimeAudioFilePlayed(), equalTo(time));
    }

    @When("^I see the time played in the audio file is not (.*)$")
    public void iSeeTimeAudioFileIsNot(String time) {
        assertThat("Time in audio file is not correct.", getConversationViewPage().getTimeAudioFilePlayed(), not(equalTo(time)));
    }

    @When("^I tap on start recording audio button$")
    public void iTapStartRecordingAudioButton() {
        getConversationViewPage().tapRecordAudioButton();
    }

    @When("^I tap on stop recording audio button$")
    public void iTapStopRecordingAudioButton() {
        getConversationViewPage().tapStopRecordingAudioButton();
    }

    @When("^I see that my audio message was recorded$")
    public void iSeeAudioWasRecorded() {
        assertThat("Stop recording button is still visible.", getConversationViewPage().isStopRecordingAudioButtonInvisible());
        assertThat("Audio message was not recorded.", getConversationViewPage().isAudioRecordingBeforeSendingVisible());
    }

    @When("^I tap on play button on recorded audio message$")
    public void iTapPlayButtonRecordedAudio() {
        getConversationViewPage().tapPlayButtonRecordedAudio();
    }

    @When("^I tap on pause button on recorded audio message$")
    public void iTapPauseButtonRecordedAudio() {
        getConversationViewPage().tapPauseButtonRecordedAudio();
    }

    @When("^I send my recorded audio message$")
    public void iSendRecordedAudio() {
        getConversationViewPage().sendRecordedAudio();
    }

    @When("^I tap on apply audio filter checkbox$")
    public void iTapAudioFilter() {
        getConversationViewPage().applyAudioFilter();
    }

    @When("^I see audio filter is applied$")
    public void iSeeAudioFilterIsApplied() {
        assertThat("Audio filter is not applied.", getConversationViewPage().isAudioFilterApplied());
    }

    // Message interactions

    @When("^I do not see reply option$")
    public void iDoNotSeeReplyOptionImage() {
        assertThat("Reply Option is  visible.", getConversationViewPage().isReplyButtonInvisible());
    }

    @When("^I see edit option$")
    public void iSeeEditOption() {
        assertThat("Edit Option is  visible.", getConversationViewPage().isEditButtonVisible());
    }

    @When("^I do not see edit option$")
    public void iDoNotSeeEditOption() {
        assertThat("Edit Option is  visible.", getConversationViewPage().isEditButtonInvisible());
    }

    @When("^I tap on edit option$")
    public void iTapEditOption() {
        getConversationViewPage().tapEditOption();
    }

    @When("^I see copy option$")
    public void iSeeCopyOptionImage() {
        assertThat("Copy Option is  visible.", getConversationViewPage().isCopyButtonVisible());
    }

    @When("^I do not see copy option$")
    public void iDoNotSeeCopyOptionImage() {
        assertThat("Copy Option is  visible.", getConversationViewPage().isCopyButtonInvisible());
    }

    @When("^I tap on copy option$")
    public void tapCopyOption() {
        getConversationViewPage().tapCopyButton();
    }

    @When("^I see download option$")
    public void iSeeDownloadOptionImage() {
        assertThat("Download Option is not visible.", getConversationViewPage().isDownloadOptionImageVisible());
    }

    @When("^I tap download option$")
    public void iTapDownloadOptionImage() {
        getConversationViewPage().tapDownloadImage();
    }

    @When("^I see delete option$")
    public void iSeeDeleteOptionImage() {
        assertThat("Delete Option is not visible.", getConversationViewPage().isDeleteOptionImageVisible());
    }

    @When("^I tap delete option$")
    public void iTapDeleteOptionImage() {
        getConversationViewPage().tapDeleteImage();
    }

    @When("^I see \"(.*)\" toast message on file details page$")
    public void iSeeToastMessage(String text) {
        assertThat("Toast message is not visible.", getConversationViewPage().isTextDisplayed(text));
    }

    @When("^I see Retry button in current conversation$")
    public void iSeeRetryButton() {
        getConversationViewPage().isRetryButtonVisible();
    }

    @When("^I tap on Retry button in current conversation$")
    public void iTapRetryButton() {
        getConversationViewPage().tapRetryButton();
    }

    @When("^I see Cancel button in current conversation$")
    public void iSeeCancelButton() {
        getConversationViewPage().isCancelButtonVisible();
    }

    @When("^I tap on Cancel button in current conversation$")
    public void iTapCancelButton() {
        getConversationViewPage().tapCancelButton();
    }

    // Reset Session

    @When("^I tap on reset session button$")
    public void iTapResetSession() {
        getConversationViewPage().tapResetSession();
    }

    // Toast messages

    @When("^I see \"(.*)\" toast message in conversation view$")
    public void iSeeToastMessageConversationList(String user) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("Toast message is not visible.", getConversationViewPage().isTextDisplayed(user));
    }

    // Misc

    @When("^I scroll to the (bottom|top) of conversation view$")
    public void IScrollToTheBottom(String direction) {
        if (direction.equalsIgnoreCase("bottom")) {
            getConversationViewPage().scrollToTheBottom();
        } else {
            getConversationViewPage().scrollToTheTop();
        }
    }

    @When("^I see Learn more link is displayed in conversation view$")
    public void iSeeLearnMoreLinkConversation() {
        getConversationViewPage().isTextDisplayed("Learn more");
    }

    @When("^I tap Learn more link in conversation view$")
    public void iTapLearnMoreLinkConversation() {
        getConversationViewPage().tapLearnMoreLink();
    }

    @When("^I see Show All button$")
    public void iSeeShowAllButton() {
        getConversationViewPage().isShowAllButtonVisible();
    }

    @When("^I tap Show All button$")
    public void iTapShowAllButton() {
        getConversationViewPage().tapShowAllButton();
    }

    @When("^I see alert \"(.*)\" in conversation view$")
    public void iSeeAlert(String text) {
        assertThat("Alert does not contain correct text or is not displayed.", getConversationViewPage().isAlertVisible(text));
    }

    @When("^I see text in alert \"(.*)\" in conversation view$")
    public void iSeeAlertWithText(String text) {
        assertThat("Alert does not contain correct text or is not displayed.", getConversationViewPage().isAlertTextVisible(text));
    }

    @When("^I tap send anyway button$")
    public void iTapSendAnywayButton() {
        getConversationViewPage().tapSendAnywayButton();
    }

    // Filter

    @When("^I see toast message \"(.*)\" was moved to \"(.*)\" in conversation view$")
    public void iSeeFolderToastMessage(String user, String folder) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("System message is not visible", getConversationViewPage().isToastMessageDisplayed(user, folder));
    }

    @When("^I see toast message \"(.*)\" was removed from \"(.*)\" in conversation view$")
    public void iSeeConversationRemovedToastMessage(String user, String folder) {
        user = context.getUsersManager().replaceAliasesOccurrences(user, ClientUsersManager.FindBy.NAME_ALIAS);
        assertThat("System message is not visible", getConversationViewPage().isRemovedToastMessageDisplayed(user, folder));
    }

    @When("^I tap filter conversation button$")
    public void iTapFilterConversationButton() {
        getConversationViewPage().tapFilterConversationButton();
    }

    @When("^I tap (.*) button on new folder page$")
    public void iTapCreateFolderButton(String buttonName) {getConversationViewPage().tapCreateFolderButton(buttonName);}

    @When("^I tap Done button on move to folder page$")
    public void iTapDoneButton() {getConversationViewPage().tapDoneButtonOnMoveToFolderPage();}

    @When("^I enter (.*) as folder name$")
    public void iEnterFolderName(String text) { getConversationViewPage().inputFolderName(text);}

    @When("^I see folder (.*) on folder bottom sheet$")
    public void iSeeFolder(String folderName) {
        assertThat(String.format("Folder '%s' is not visible.", folderName), getConversationViewPage().isTextDisplayed(folderName));
    }

    @Then("^I get navigated to folder page and I see (.*) as the page heading$")
    public void iSeeFolderPage(String folderPage) {
        assertThat(String.format("Folder page '%s' is not visible.", folderPage), getConversationViewPage().isTextDisplayed(folderPage));
    }

    @When("^I tap existing folder (.*) on folder bottom sheet$")
    public void tapExistingFolder(String folderName) {
        getConversationViewPage().tapExistingFolder(folderName);
    }

    @When("^I see folders bottom sheet with (.*) as the header title$")
    public void iSeeFolderBottomSheet(String text) {
        assertThat(String.format("Folders is not visible at the bottom sheet header.", text),getConversationViewPage().isTextDisplayed(text));
    }

    // Federation

    @Then("^I see message could not be sent due to backends not reachable error in conversation view$")
    public void iSeeMessageCouldNotBeSentUnreachableError() {
        assertThat("Error is not displayed", getConversationViewPage().isErrorMessageSendingUnreachableBackendVisible());
    }

    @Then("^I do not see message could not be sent due to backends not reachable error in conversation view$")
    public void iDoNotSeeMessageCouldNotBeSentUnreachableError() {
        assertThat("Error is not displayed", !getConversationViewPage().isErrorMessageSendingUnreachableBackendVisible());
    }

    @Then("^I see confirmation alert with text \"(.*)\" in conversation view$")
    public void iSeeConfirmationAlert(String alertText) {
        assertThat("The confirmation alert with text '%s' in conversation view is not visible.", getConversationViewPage().isConfirmationAlertVisible(alertText));
    }

    @Then("^I see participants will not receive your message error in conversation view$")
    public void iSeeParticipantsWillNotGetMessageError() {
        assertThat("Error is not displayed", getConversationViewPage().isErrorParticipantWontGetMessageVisible());
    }
}