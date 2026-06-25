/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.tests.core.pages

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertFalse
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.findElementOrNull
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertEquals
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

@Suppress("LargeClass")
data class ConversationViewPage(private val device: UiDevice) {
    private val fileSavedToastPrefix = "The file "
    private val fileSavedToastMessage = "was saved successfully to the Downloads folder"

    private fun displayedUserName(userName: String) = UiSelectorParams(text = userName)
    private val typeMessageField = UiSelectorParams(description = " Type a message")
    private val sentQRImage = UiSelectorParams(description = "Image message")

    private val sharedLocationContainer = UiSelectorParams(description = "Location item")
    private val attachNewFileButton = UiSelectorParams(description = "Add attachment")
    private val audioSeekBar = UiSelectorParams(className = "android.widget.SeekBar")
    private val audioInitialTime = UiSelectorParams(text = "00:00")
    private val playAudioButton = UiSelectorParams(description = "Play audio")
    private val recordAudioButton = UiSelectorParams(description = "Record Audio")
    private val stopRecordingAudioButton = UiSelectorParams(description = "Stop Recording Audio")
    private val sendAudioRecordingButton = UiSelectorParams(description = "Send Audio Message")
    private val applyAudioFilterCheckboxIndex = 0

    private val startCallButton = UiSelectorParams(description = "Start audio call")
    private val pauseAudioButton = UiSelectorParams(description = "Pause audio")
    private val downloadButton = UiSelectorParams(text = "Download")

    private val modalTextLocator = UiSelectorParams(textContains = "save it to your device")

    private val saveButton = UiSelectorParams(text = "Save")

    private val openButton = UiSelectorParams(text = "Open")
    private val cancelButton = UiSelectorParams(text = "Cancel")

    private val downloadButtonOnVideoFile = UiSelectorParams(text = "Tap to download")
    private val videoDurationLocator = UiSelectorParams(text = "00:03")

    private val messageInputField = UiSelectorParams(className = "android.widget.EditText")
    private val copyTextOption = UiSelectorParams(text = "Copy text")
    private val deleteOption = UiSelectorParams(text = "Delete")
    private val deleteForMeOption = UiSelectorParams(text = "Delete for Me")
    private val deleteForEveryoneOption = UiSelectorParams(text = "Delete for Everyone")
    private val deletedMessageLabel = UiSelectorParams(text = "Deleted message")
    private val editTextOption = UiSelectorParams(text = "Edit text")
    private val editedLabel = UiSelectorParams(text = "Edited")
    private val replyOption = UiSelectorParams(text = "Reply")
    private val visitLinkDialogTitle = UiSelectorParams(text = "Visit Link")
    private val openLinkButton = UiSelectorParams(text = "Open")
    private val cancelReplyButton = UiSelectorParams(description = "Cancel message reply")
    private val mentionSomeoneButton = UiSelectorParams(description = "Mention someone")
    private fun mentionListUser(userName: String) = UiSelectorParams(text = userName)

    private fun conversationDetails1On1(userName: String) = UiSelector().className("android.widget.TextView").text(userName)
    private fun conversationDetailsGroup(userName: String) = UiSelectorParams(text = userName)

    private val sendButton = UiSelectorParams(description = "Send")
    private val editMessageButton = UiSelectorParams(description = "Edit the message")

    private val backButton = UiSelectorParams(description = "Go back to conversation list")

    private val selfDeleteTimerButton = UiSelectorParams(description = "Set timer for self-deleting messages")

    private val selfDeletingMessageLabel = UiSelectorParams(description = " Self-deleting message")
    private val pingButton = UiSelectorParams(description = "Ping")
    private val pingButtonOnModal = UiSelectorParams(text = "Ping")
    private val guestsAndAppsBanner = UiSelectorParams(textContains = "Guests and apps are present")
    private val topOfConversationViewPageMessage = UiSelectorParams(textContains = "You made it to the top")

    private val mlsUpgradeMessageSelectors = listOf(
        UiSelectorParams(textContains = "This conversation now uses the new Messaging"),
        UiSelectorParams(textContains = "Layer Security (MLS) protocol"),
        UiSelectorParams(textContains = "latest version of Wire on your devices")
    )

    private fun selfDeleteOption(label: String): UiSelectorParams {
        return UiSelectorParams(text = label, className = "android.widget.TextView")
    }

    private fun sharingOption(label: String): UiSelectorParams {
        return UiSelectorParams(text = label, className = "android.widget.TextView")
    }
    private fun receivingProhibitedText(label: String): UiSelectorParams {
        return UiSelectorParams(text = label)
    }
    private fun fileWithName(name: String): UiSelectorParams {
        return UiSelectorParams(text = name)
    }

    fun assertConversationIsVisibleWithTeamMember(userName: String): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(displayedUserName(userName))
        } catch (e: AssertionError) {
            throw AssertionError("Team member name '$userName' is not visible in conversation view", e)
        }
        return this
    }

    fun assertConversationIsVisibleWithTeamOwner(userName: String): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(displayedUserName(userName))
        } catch (e: AssertionError) {
            throw AssertionError("Team owner name '$userName' is not visible in conversation view", e)
        }
        return this
    }

    fun assertAudioMessageIsVisible(): ConversationViewPage {
        val seekBar = UiWaitUtils.waitElement(audioSeekBar)
        Assert.assertTrue("Audio file is not visible", !seekBar.visibleBounds.isEmpty)
        return this
    }

    fun assertAudioTimeStartsAtZero(): ConversationViewPage {
        val timeAtZero = UiWaitUtils.waitElement(audioInitialTime)
        assertTrue("Expected audio to start at 00:00", !timeAtZero.visibleBounds.isEmpty)
        return this
    }

    fun assertAudioTimeIsNotZeroAnymore(): ConversationViewPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.text("00:00"),
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            errorMessage = "Audio time is still at 00:00, expected it to have changed"
        )
        return this
    }

    fun assertPollMessageVisible(message: String): ConversationViewPage {
        val pollMessage = UiWaitUtils.waitElement(UiSelectorParams(textContains = message))
        Assert.assertTrue("Poll message '$message' is not visible.", !pollMessage.visibleBounds.isEmpty)
        return this
    }

    fun assertPollButtonVisible(buttonText: String): ConversationViewPage {
        val pollButton = UiWaitUtils.waitElement(UiSelectorParams(text = buttonText))
        Assert.assertTrue("Poll button '$buttonText' is not visible.", !pollButton.visibleBounds.isEmpty)
        return this
    }

    fun tapPollButton(buttonText: String): ConversationViewPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = buttonText)).click()
        return this
    }

    fun clickPlayButtonOnAudioMessage(): ConversationViewPage {
        val button = UiWaitUtils.waitElement(playAudioButton)
        requireNotNull(button) { "Play button with description 'Play audio' not found" }
        button.click()
        return this
    }

    fun clickPauseButtonOnAudioMessage(): ConversationViewPage {
        val button = UiWaitUtils.waitElement(pauseAudioButton)
        requireNotNull(button) { "Pause button with description 'Pause audio' not found" }
        button.click()
        return this
    }

    fun tapRecordAudioButton(): ConversationViewPage {
        UiWaitUtils.waitElement(recordAudioButton).click()
        return this
    }

    fun tapStopRecordingAudioButton(): ConversationViewPage {
        UiWaitUtils.waitElement(stopRecordingAudioButton).click()
        return this
    }

    fun assertAudioMessageWasRecorded(): ConversationViewPage {
        val seekBar = UiWaitUtils.waitElement(audioSeekBar)
        assertTrue("Audio message was not recorded.", !seekBar.visibleBounds.isEmpty)
        return this
    }

    fun sendRecordedAudioMessage(): ConversationViewPage {
        UiWaitUtils.waitElement(sendAudioRecordingButton).click()
        return this
    }

    fun tapApplyAudioFilterCheckbox(): ConversationViewPage {
        val checkbox = applyAudioFilterCheckbox()
        if (!checkbox.isChecked) {
            checkbox.click()
        }
        return this
    }

    fun assertAudioFilterIsApplied(): ConversationViewPage {
        assertTrue("Audio filter is not applied.", applyAudioFilterCheckbox().isChecked)
        return this
    }

    private fun applyAudioFilterCheckbox() =
        device.findObjects(By.clazz("android.widget.CheckBox")).getOrNull(applyAudioFilterCheckboxIndex)
            ?: throw AssertionError("Apply audio filter checkbox is not visible.")

    fun longPressOnAudioSeekBar(): ConversationViewPage {
        val seekBar = UiWaitUtils.waitElement(audioSeekBar)
        val center = seekBar.visibleCenter
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .swipe(center.x, center.y, center.x, center.y, 120)

        return this
    }

    fun longPressOnMessage(message: String): ConversationViewPage {
        val messageElement = UiWaitUtils.waitElement(UiSelectorParams(text = message))
        val center = messageElement.visibleCenter
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .swipe(center.x, center.y, center.x, center.y, 120)

        return this
    }

    fun tapCopyTextOption(): ConversationViewPage {
        UiWaitUtils.waitElement(copyTextOption).click()
        return this
    }

    fun tapDeleteOption(): ConversationViewPage {
        UiWaitUtils.waitElement(deleteOption).click()
        return this
    }

    fun tapDeleteForMeOption(): ConversationViewPage {
        UiWaitUtils.waitElement(deleteForMeOption).click()
        return this
    }

    fun tapDeleteForEveryoneOption(): ConversationViewPage {
        UiWaitUtils.waitElement(deleteForEveryoneOption).click()
        return this
    }

    fun assertDeletedMessageLabelVisible(): ConversationViewPage {
        val deletedLabel = UiWaitUtils.waitElement(deletedMessageLabel)
        Assert.assertTrue("Deleted message label is not visible.", !deletedLabel.visibleBounds.isEmpty)
        return this
    }

    fun tapEditTextOption(): ConversationViewPage {
        UiWaitUtils.waitElement(editTextOption).click()
        return this
    }

    fun tapReplyOption(): ConversationViewPage {
        UiWaitUtils.waitElement(replyOption).click()
        return this
    }

    fun assertEditTextOptionVisible(): ConversationViewPage {
        val editOption = UiWaitUtils.waitElement(editTextOption)
        Assert.assertTrue("Edit text option is not visible.", !editOption.visibleBounds.isEmpty)
        return this
    }

    fun assertReplyOptionVisible(): ConversationViewPage {
        val reply = UiWaitUtils.waitElement(replyOption)
        Assert.assertTrue("Reply option is not visible.", !reply.visibleBounds.isEmpty)
        return this
    }

    fun assertReplyPreviewVisible(quotedMessage: String): ConversationViewPage {
        val quotedText = UiWaitUtils.waitElement(UiSelectorParams(text = quotedMessage))
        val cancelReply = UiWaitUtils.waitElement(cancelReplyButton)
        Assert.assertTrue("Reply preview text '$quotedMessage' is not visible.", !quotedText.visibleBounds.isEmpty)
        Assert.assertTrue("Cancel reply button is not visible.", !cancelReply.visibleBounds.isEmpty)
        return this
    }

    fun assertMessageIsReplyTo(replyMessage: String, quotedMessage: String): ConversationViewPage {
        assertSentMessageIsVisibleInCurrentConversation(replyMessage)
        val isReplyVisible = UiWaitUtils.retryUntilTimeout(
            timeout = 5.seconds,
            pollingInterval = UiWaitUtils.POLLING_SLOW
        ) {
            visibleTextCount(quotedMessage) >= 2
        }
        if (!isReplyVisible) {
            throw AssertionError(
                "Expected message '$replyMessage' to show quoted text '$quotedMessage' as a reply."
            )
        }
        return this
    }

    private fun visibleTextCount(text: String): Int =
        device.findObjects(By.text(text)).count { !it.visibleBounds.isEmpty }

    fun assertEditedLabelVisible(): ConversationViewPage {
        val label = UiWaitUtils.waitElement(editedLabel)
        Assert.assertTrue("Edited label is not visible.", !label.visibleBounds.isEmpty)
        return this
    }

    fun assertBottomSheetIsVisible(): ConversationViewPage {
        val bottomSheet = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .findObject(UiSelector().className("android.view.View").instance(4))
        assertTrue("Bottom sheet is not visible", !bottomSheet.visibleBounds.isEmpty)
        return this
    }

    fun assertBottomSheetButtonsVisible_ReactionsDetailsReplyDownloadShareOpenDelete(): ConversationViewPage {
        val expectedButtons = listOf(
            "REACTIONS",
            "Message Details",
            "Reply",
            "Download",
            "Share",
            "Open",
            "Delete"
        )

        expectedButtons.forEach { expectedText ->
            val element = UiWaitUtils.waitElement(UiSelectorParams(text = expectedText))
            assertTrue("Button with text '$expectedText' is not visible", !element.visibleBounds.isEmpty)
            assertEquals(expectedText, element.text, "Button text does not match expected")
        }

        return this
    }

    fun assertTextMessageReactionOptionsVisible(): ConversationViewPage {
        val expectedOptions = listOf(
            "REACTIONS",
            "Message Details",
            "Copy text",
            "Reply",
            "Delete"
        )

        expectedOptions.forEach { expectedText ->
            val element = UiWaitUtils.waitElement(UiSelectorParams(text = expectedText))
            assertTrue("Option with text '$expectedText' is not visible", !element.visibleBounds.isEmpty)
            assertEquals(expectedText, element.text, "Option text does not match expected")
        }

        return this
    }

    fun tapReactionIcon(reaction: String): ConversationViewPage {
        val reactionIcon = UiWaitUtils.waitElement(UiSelectorParams(text = reaction))
        reactionIcon.click()
        return this
    }

    fun tapVisibleReaction(reaction: String): ConversationViewPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = reaction)).click()
        return this
    }

    fun assertReactionAndUserCountVisible(reaction: String, userCount: Int): ConversationViewPage {
        val reactionElement = UiWaitUtils.waitElement(UiSelectorParams(text = reaction))
        val countElement = UiWaitUtils.waitElement(UiSelectorParams(text = userCount.toString()))

        assertTrue("Reaction '$reaction' is not visible", !reactionElement.visibleBounds.isEmpty)
        assertTrue("Reaction count '$userCount' is not visible", !countElement.visibleBounds.isEmpty)

        return this
    }

    fun assertReactionNotVisible(reaction: String, timeoutSeconds: Int = 5): ConversationViewPage {
        val notVisible = UiWaitUtils.retryUntilTimeout(
            timeout = timeoutSeconds.seconds,
            pollingInterval = UiWaitUtils.POLLING_SLOW
        ) {
            findElementOrNull(UiSelectorParams(text = reaction)) == null
        }
        if (!notVisible) {
            throw AssertionError(
                "Expected reaction '$reaction' to be absent, but it was found within ${timeoutSeconds}s."
            )
        }
        return this
    }

    fun tapDownloadButton(): ConversationViewPage {
        UiWaitUtils.waitElement(downloadButton).click()
        return this
    }

    fun assertFileActionModalIsVisible(timeout: Duration = 8.seconds): ConversationViewPage {
        val modalAnchors = listOf(modalTextLocator, saveButton, openButton, cancelButton)
        val visibleAnchor = UiWaitUtils.waitAnyVisible(
            selectors = modalAnchors,
            timeout = timeout,
            pollingInterval = 150.milliseconds
        )
        if (visibleAnchor == null) {
            throw AssertionError("The file action modal was not visible within ${timeout.inWholeMilliseconds}ms.")
        }
        return this
    }

    fun assertImageFileWithNameIsVisible(fileName: String): ConversationViewPage {
        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName))
        Assert.assertTrue("File with name '$fileName' is not visible", !fileNameElement.visibleBounds.isEmpty)
        return this
    }

    fun assertFileWithNameIsVisible(fileName3: String): ConversationViewPage {
        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName3))
        Assert.assertTrue("File with name '$fileName3' is not visible", !fileNameElement.visibleBounds.isEmpty)
        return this
    }

    fun assertImageMessageVisible(): ConversationViewPage {
        val imageMessage = UiWaitUtils.waitElement(sentQRImage)
        Assert.assertTrue("Image message is not visible", !imageMessage.visibleBounds.isEmpty)
        return this
    }

    fun assertFileWithNameNotVisible(fileName: String, timeoutSeconds: Int = 5): ConversationViewPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.text(fileName),
            timeout = timeoutSeconds.seconds,
            errorMessage = "File with name '$fileName' is still visible."
        )
        return this
    }

    fun assertImageMessageNotVisible(timeoutSeconds: Int = 5): ConversationViewPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.desc("Image message"),
            timeout = timeoutSeconds.seconds,
            errorMessage = "Image message is still visible."
        )
        return this
    }

    fun assertAudioMessageNotVisible(timeoutSeconds: Int = 5): ConversationViewPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.clazz("android.widget.SeekBar"),
            timeout = timeoutSeconds.seconds,
            errorMessage = "Audio message is still visible."
        )
        return this
    }

    fun clickFileWithName(fileName: String): ConversationViewPage {
        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName))
        fileNameElement.click()
        return this
    }

    fun clickTextFileWithName(fileName3: String): ConversationViewPage {
        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName3))
        fileNameElement.click()
        return this
    }

    fun assertDownloadModalButtonsAreVisible_Open_Save_Cancel(): ConversationViewPage {
        val expectedButtons = listOf(
            "Open",
            "Save",
            "Cancel"
        )

        expectedButtons.forEach { expectedText ->
            val element = UiWaitUtils.waitElement(UiSelectorParams(text = expectedText))
            assertTrue("Button with text '$expectedText' is not visible on the modal", !element.visibleBounds.isEmpty)
            assertEquals(expectedText, element.text, "Button text does not match expected")
        }

        return this
    }

    fun clickSaveButtonOnDownloadModal(timeout: Duration = 8.seconds): ConversationViewPage {
        val save = UiWaitUtils.waitElement(saveButton, timeout = timeout)
        val bounds = runCatching { save.visibleBounds }.getOrNull()

        runCatching { save.click() }
        device.waitForIdle(300)

        val stillVisible = UiWaitUtils.findElementOrNull(saveButton)
            ?.let { runCatching { !it.visibleBounds.isEmpty }.getOrDefault(false) } == true

        if (stillVisible && bounds != null && !bounds.isEmpty) {
            device.click(bounds.centerX(), bounds.centerY())
        }

        return this
    }

    fun clickOpenButtonOnDownloadModal(): ConversationViewPage {
        UiWaitUtils.waitElement(openButton).click()
        return this
    }

    fun waitForPreviousFileSavedToastToDisappear(timeout: Duration = 7.seconds): ConversationViewPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.textContains(fileSavedToastMessage),
            timeout = timeout,
            errorMessage = "File saved toast did not disappear within ${timeout.inWholeMilliseconds}ms."
        )
        return this
    }

    fun assertFileSavedToast(
        expectedMessage: String,
        timeout: Duration = 7.seconds
    ): ConversationViewPage {
        val toastPattern = buildSavedFileToastPattern(expectedMessage)

        UiWaitUtils.waitUntilVisible(
            params = UiSelectorParams(
                textMatches = toastPattern
            ),
            timeout = timeout,
            errorMessage = "Toast '$expectedMessage' was not displayed within ${timeout.inWholeMilliseconds}ms."
        )
        return this
    }

    @Suppress("ReturnCount")
    private fun buildSavedFileToastPattern(expectedMessage: String): String {
        val suffix = " $fileSavedToastMessage"

        if (!expectedMessage.startsWith(fileSavedToastPrefix) || !expectedMessage.endsWith(suffix)) {
            return Regex.escape(expectedMessage)
        }

        val fileWithExtension = expectedMessage
            .removePrefix(fileSavedToastPrefix)
            .removeSuffix(suffix)

        val lastDotIndex = fileWithExtension.lastIndexOf('.')
        if (lastDotIndex <= 0 || lastDotIndex == fileWithExtension.lastIndex) {
            return Regex.escape(expectedMessage)
        }

        val fileName = fileWithExtension.substring(0, lastDotIndex)
        val extension = fileWithExtension.substring(lastDotIndex + 1)

        return buildString {
            append(Regex.escape(fileSavedToastPrefix))
            append(Regex.escape(fileName))
            append("(?: \\([0-9]+\\))?\\.")
            append(Regex.escape(extension))
            append(Regex.escape(suffix))
        }
    }

    fun scrollToBottomOfConversationScreen(): ConversationViewPage {
        val scrollable = UiScrollable(UiSelector().scrollable(true))
        scrollable.setAsVerticalList()
        val success = scrollable.flingToEnd(10)
        Assert.assertTrue("Could not scroll to bottom of conversation screen.", success)
        return this
    }

    fun scrollToTopOfConversationScreen(): ConversationViewPage {
        val scrollable = UiScrollable(UiSelector().scrollable(true))
        scrollable.setAsVerticalList()
        val success = scrollable.flingToBeginning(10)
        Assert.assertTrue("Could not scroll to top of conversation screen.", success)
        return this
    }

    fun tapDownloadButtonOnVideoFile(): ConversationViewPage {
        UiWaitUtils.waitElement(downloadButtonOnVideoFile).click()
        return this
    }

    fun tapToPlayVideoFile(): ConversationViewPage {
        UiWaitUtils.waitElement(videoDurationLocator).click()
        return this
    }

    fun assertWireAppIsNotInForeground() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val currentPackage = device.currentPackageName
        assertFalse(
            "Wire app is still in foreground: $currentPackage",
            currentPackage.contains("APP_")
        )
    }

    fun typeMessageInInputField(message: String): ConversationViewPage {
        val field = UiWaitUtils.waitElement(messageInputField)
        field.click()
        field.text = message
        return this
    }

    fun typeMentionQueryInInputField(query: String): ConversationViewPage {
        val field = UiWaitUtils.waitElement(messageInputField)
        field.click()
        val encodedText = query
            .replace("@", "%40")
            .replace(" ", "%s")
        device.executeShellCommand("input text $encodedText")
        return this
    }

    fun pasteClipboardIntoInputField(fallbackText: String? = null): ConversationViewPage {
        val field = UiWaitUtils.waitElement(messageInputField)
        field.click()
        device.executeShellCommand("input keyevent KEYCODE_PASTE")
        if (!fallbackText.isNullOrBlank()) {
            val pasted = UiWaitUtils.retryUntilTimeout(
                timeout = UiWaitUtils.SHORT_TIMEOUT,
                pollingInterval = 250.milliseconds
            ) {
                findElementOrNull(UiSelectorParams(textContains = fallbackText)) != null
            }
            if (!pasted) {
                field.text = fallbackText
            }
        }
        return this
    }

    fun tapMentionSomeoneButton(): ConversationViewPage {
        val field = UiWaitUtils.waitElement(messageInputField)
        field.click()
        UiWaitUtils.waitElement(mentionSomeoneButton).click()
        return this
    }

    fun assertUserVisibleInMentionList(userName: String): ConversationViewPage {
        val user = UiWaitUtils.waitElement(mentionListUser(userName))
        Assert.assertTrue("User '$userName' is not visible in mention list.", !user.visibleBounds.isEmpty)
        return this
    }

    fun selectUserFromMentionList(userName: String): ConversationViewPage {
        UiWaitUtils.waitElement(mentionListUser(userName)).click()
        return this
    }

    fun clickSendButton(): ConversationViewPage {
        UiWaitUtils.waitElement(sendButton).click()
        return this
    }

    fun clickEditMessageButton(): ConversationViewPage {
        UiWaitUtils.waitElement(editMessageButton).click()
        return this
    }

    fun assertSentMessageIsVisibleInCurrentConversation(message: String): ConversationViewPage {
        val messageSelector = UiSelectorParams(text = message)
        val messageElement = UiWaitUtils.waitElement(messageSelector)

        Assert.assertTrue(
            "Message '$message' is not visible in the conversation",
            !messageElement.visibleBounds.isEmpty
        )
        return this
    }

    fun tapLinkInCurrentConversation(link: String): ConversationViewPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = link)).click()
        return this
    }

    fun assertVisitLinkDialogVisible(link: String): ConversationViewPage {
        UiWaitUtils.waitElement(visitLinkDialogTitle)
        UiWaitUtils.waitElement(UiSelectorParams(textContains = link))
        return this
    }

    fun tapOpenButtonOnVisitLinkDialog(): ConversationViewPage {
        UiWaitUtils.waitElement(openLinkButton).click()
        return this
    }

    fun assertMessageNotVisible(text: String, timeoutSeconds: Int = 5) {
        val notVisible = UiWaitUtils.retryUntilTimeout(
            timeout = timeoutSeconds.seconds,
            pollingInterval = UiWaitUtils.POLLING_SLOW
        ) {
            findElementOrNull(UiSelectorParams(text = text)) == null
        }
        if (!notVisible) {
            throw AssertionError(
                "Expected message '$text' to be absent, but it was found within ${timeoutSeconds}s.",
                AssertionError("Message '$text' is still present in the conversation.")
            )
        }
    }

    fun assertMessageContainingTextNotVisible(text: String, timeoutSeconds: Int = 5) {
        val notVisible = UiWaitUtils.retryUntilTimeout(
            timeout = timeoutSeconds.seconds,
            pollingInterval = UiWaitUtils.POLLING_SLOW
        ) {
            findElementOrNull(UiSelectorParams(textContains = text)) == null
        }
        if (!notVisible) {
            throw AssertionError(
                "Expected message containing '$text' to be absent, but it was found within ${timeoutSeconds}s.",
                AssertionError("Message containing '$text' is still present in the conversation.")
            )
        }
    }

    fun tapBackButtonToCloseConversationViewPage(): ConversationViewPage {
        UiWaitUtils.waitElement(backButton).click()
        return this
    }

    fun tapMessageInInputField(): ConversationViewPage {
        val inputField = UiWaitUtils.waitElement(messageInputField)
        inputField.click()
        return this
    }

    fun tapSelfDeleteTimerButton(): ConversationViewPage {
        val button = UiWaitUtils.waitElement(selfDeleteTimerButton)
        button.click()
        return this
    }

    fun assertSelfDeleteOptionVisible(label: String) {
        try {
            UiWaitUtils.waitElement(selfDeleteOption(label))
        } catch (e: AssertionError) {
            throw AssertionError("Self-destruct option '$label' is not visible", e)
        }
    }

    fun tapSelfDeleteOption(label: String) {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val element = device.findObject(
            UiSelector().text(label).className("android.widget.TextView")
        )
        element.click()
    }

    fun tapSharingOption(label: String) {
        val element = UiWaitUtils.waitElement(sharingOption(label))
        element.click()
    }

    fun assertSelfDeletingMessageLabelVisible() {
        try {
            UiWaitUtils.waitElement(selfDeletingMessageLabel)
        } catch (e: AssertionError) {
            throw AssertionError("'Self-deleting message' label is not visible", e)
        }
    }

    fun assertReceivedMessageIsVisibleInCurrentConversation(message: String): ConversationViewPage {
        val messageSelector = UiSelectorParams(text = message)

        try {
            UiWaitUtils.waitElement(messageSelector)
        } catch (e: AssertionError) {
            throw AssertionError("Message '$message' was not found or not visible in the conversation.", e)
        }

        return this
    }

    fun assertMessageContainingTextIsVisibleInCurrentConversation(text: String): ConversationViewPage {
        val messageSelector = UiSelectorParams(textContains = text)

        try {
            UiWaitUtils.waitElement(messageSelector)
        } catch (e: AssertionError) {
            throw AssertionError("Message containing '$text' was not found or not visible in the conversation.", e)
        }

        return this
    }

    fun assertVisibleMentionedNameIs(mentionedName: String): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(UiSelectorParams(text = mentionedName))
        } catch (e: AssertionError) {
            throw AssertionError("Mention '$mentionedName' is not visible in the conversation", e)
        }

        return this
    }

    fun assertConversationScreenVisible(): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(typeMessageField)
        } catch (e: AssertionError) {
            throw AssertionError("Conversation screen is not visible: 'Type a message' field not found.", e)
        }

        return this
    }

    fun assertChannelConversationInForeground(conversationName: String): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(conversationDetailsGroup(conversationName))
        } catch (e: AssertionError) {
            throw AssertionError("Channel conversation '$conversationName' is not in foreground.", e)
        }
        return this
    }

    fun assertGuestsAndAppsBannerVisible(): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(guestsAndAppsBanner)
        } catch (e: AssertionError) {
            throw AssertionError("'Guests and apps are present' banner is not visible in conversation view", e)
        }

        return this
    }

    fun assertTopOfConversationViewPageVisible(): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(topOfConversationViewPageMessage)
        } catch (e: AssertionError) {
            throw AssertionError(
                "Top-of-conversation message is not visible in conversation view.",
                e
            )
        }

        return this
    }

    fun click1On1ConversationDetails(userName: String): ConversationViewPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val userName = device.findObject(conversationDetails1On1(userName))
        if (!userName.exists()) throw AssertionError("User '$userName' not found in current conversation")
        userName.click()

        return this
    }

    fun clickOnGroupConversationDetails(userName: String): ConversationViewPage {
        val params = conversationDetailsGroup(userName)

        UiWaitUtils.waitUntilVisible(
            params = params,
            timeout = 5.seconds,
            errorMessage = "Group conversation details for user '$userName' not visible"
        )

        UiWaitUtils.waitElement(params).click()
        return this
    }

    fun clickOnChannelConversationDetails(conversationName: String): ConversationViewPage {
        return clickOnGroupConversationDetails(conversationName)
    }

    fun iTapStartCallButton(): ConversationViewPage {
        UiWaitUtils.waitElement(startCallButton).click()
        return this
    }

    fun iTapFileSharingButton(): ConversationViewPage {
        UiWaitUtils.waitElement(attachNewFileButton).click()
        return this
    }

    fun assertSharingOptionVisible(label: String) {
        try {
            UiWaitUtils.waitElement(sharingOption(label))
        } catch (e: AssertionError) {
            throw AssertionError("Sharing option '$label' is not visible", e)
        }
    }

    fun assertSharingOptionNotVisible(label: String): ConversationViewPage {
        val option = findElementOrNull(sharingOption(label))
        Assert.assertTrue(
            "Sharing option '$label' is visible",
            option == null || option.visibleBounds.isEmpty
        )
        return this
    }

    fun assertReceivingImagesProhibited(): ConversationViewPage {
        assertReceivingProhibitedTextVisible("Receiving images is prohibited")
        return this
    }

    fun assertReceivingVideosProhibited(): ConversationViewPage {
        assertReceivingProhibitedTextVisible("Receiving videos is prohibited")
        return this
    }

    fun assertReceivingAudioMessagesProhibited(): ConversationViewPage {
        assertReceivingProhibitedTextVisible("Receiving audio messages is prohibited")
        return this
    }

    fun assertReceivingFilesProhibited(): ConversationViewPage {
        assertReceivingProhibitedTextVisible("Receiving files is prohibited")
        return this
    }

    private fun assertReceivingProhibitedTextVisible(label: String) {
        try {
            UiWaitUtils.waitElement(receivingProhibitedText(label))
        } catch (e: AssertionError) {
            throw AssertionError("'$label' is not visible in current conversation", e)
        }
    }

    fun iSeeSentQrCodeImageInCurrentConversation(): ConversationViewPage {

        try {
            UiWaitUtils.waitElement(sentQRImage)
        } catch (e: AssertionError) {
            throw AssertionError("Sent qrCodeImage is not visible in current conversation", e)
        }
        return this
    }

    fun iSeeLocationMapContainer(): ConversationViewPage {

        try {
            UiWaitUtils.waitElement(sharedLocationContainer)
        } catch (e: AssertionError) {
            throw AssertionError("Location map container is not visible", e)
        }
        return this
    }

    fun assertRestoredBackupMessageIsVisibleInCurrentConversation(message: String): ConversationViewPage {
        val messageSelector = UiSelectorParams(text = message)

        try {
            UiWaitUtils.waitElement(messageSelector)
        } catch (e: AssertionError) {
            throw AssertionError("Message '$message' was not found or not visible in the conversation.", e)
        }

        return this
    }

    fun waitUntilConversationTurnsMls(
        timeout: Duration = 20.seconds,
        settleAfterDetected: Duration = Duration.ZERO
    ): ConversationViewPage {
        val mlsMarker = UiWaitUtils.waitAnyVisible(
            selectors = mlsUpgradeMessageSelectors,
            timeout = timeout,
            pollingInterval = 200.milliseconds
        )
        if (mlsMarker != null) {
            // MLS banner can appear slightly before the conversation is fully ready for a first outbound message.
            if (settleAfterDetected > Duration.ZERO) {
                UiWaitUtils.waitFor(settleAfterDetected)
            }
            return this
        }
        throw AssertionError("MLS upgrade system message was not visible within ${timeout.inWholeMilliseconds}ms.")
    }

    fun tapPingButton(): ConversationViewPage {
        UiWaitUtils.waitElement(pingButton).click()
        return this
    }

    fun tapPingButtonModal(): ConversationViewPage {
        UiWaitUtils.waitElement(pingButtonOnModal).click()
        return this
    }

    fun iSeePingModalWithText(message: String): ConversationViewPage {
        val messageSelector = UiSelectorParams(text = message)

        try {
            UiWaitUtils.waitElement(messageSelector)
        } catch (e: AssertionError) {
            throw AssertionError("Message '$message' is not not visible on ping modal.", e)
        }

        return this
    }
}
