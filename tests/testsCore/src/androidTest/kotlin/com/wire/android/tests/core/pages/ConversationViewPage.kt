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
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import junit.framework.TestCase.assertFalse
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.findElementOrNull
import uiautomatorutils.UiWaitUtils.waitElement
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

    private fun conversationDetails1On1(userName: String) = UiSelectorParams(
        className = "android.widget.TextView",
        text = userName
    )
    private fun conversationDetailsGroup(userName: String) = UiSelectorParams(text = userName)

    private val sendButton = UiSelectorParams(description = "Send")

    private val backButton = UiSelectorParams(description = "Go back to conversation list")

    private val conversationOptionsButton = UiSelectorParams(description = "Open conversation options")

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
    private fun fileWithName(name: String): UiSelectorParams {
        return UiSelectorParams(text = name)
    }

    private fun assertElementNotVisible(params: UiSelectorParams, description: String, timeoutSeconds: Int = 5) {
        val notVisible = UiWaitUtils.retryUntilTimeout(
            timeout = timeoutSeconds.seconds,
            pollingInterval = UiWaitUtils.POLLING_SLOW
        ) {
            findElementOrNull(params) == null
        }
        if (!notVisible) {
            throw AssertionError("Expected $description to be absent, but it was found within ${timeoutSeconds}s.")
        }
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

    fun assertAudioMessageNotVisible(): ConversationViewPage {
        assertElementNotVisible(audioSeekBar, "audio file")
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

    fun assertReactionAndUserCountVisible(reaction: String, userCount: Int): ConversationViewPage {
        val reactionElement = UiWaitUtils.waitElement(UiSelectorParams(text = reaction))
        val countElement = UiWaitUtils.waitElement(UiSelectorParams(text = userCount.toString()))

        assertTrue("Reaction '$reaction' is not visible", !reactionElement.visibleBounds.isEmpty)
        assertTrue("Reaction count '$userCount' is not visible", !countElement.visibleBounds.isEmpty)

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

    fun assertFileWithNameNotVisible(fileName: String): ConversationViewPage {
        assertElementNotVisible(fileWithName(fileName), "file with name '$fileName'")
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

    fun scrollToBottomOfConversationScreen() {
        try {
            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

            // Create a scrollable container (finds first scrollable layout on screen)
            val scrollable = UiScrollable(UiSelector().scrollable(true))
            scrollable.setAsVerticalList()

            // Perform fling (fast scroll) to the bottom
            val success = scrollable.flingToEnd(10)
            println("✅ Scrolled to bottom: $success")
        } catch (e: Exception) {
            println("Failed to scroll: ${e.message}")
        }
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

    fun clickSendButton(timeout: Duration = UiWaitUtils.DEFAULT_TIMEOUT): ConversationViewPage {
        if (!UiWaitUtils.clickWhenClickable(sendButton, timeout = timeout)) {
            throw AssertionError("Send button was not enabled within ${timeout.inWholeMilliseconds}ms.")
        }
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

    fun tapBackButtonToCloseConversationViewPage(timeout: Duration = UiWaitUtils.SHORT_TIMEOUT): ConversationViewPage {
        val closed = UiWaitUtils.retryUntilTimeout(
            timeout = timeout,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            UiWaitUtils.clickWhenClickable(
                backButton,
                timeout = UiWaitUtils.POLLING_DEFAULT,
                pollingInterval = UiWaitUtils.POLLING_FAST
            )
            !isConversationViewStillVisible()
        }

        if (!closed) {
            throw AssertionError("Conversation view was still visible after tapping back within ${timeout.inWholeMilliseconds}ms")
        }

        return this
    }

    private fun isConversationViewStillVisible(): Boolean {
        return try {
            val typeMessageVisible = findElementOrNull(typeMessageField)?.let { !it.visibleBounds.isEmpty } == true
            val sendButtonVisible = findElementOrNull(sendButton)?.let { !it.visibleBounds.isEmpty } == true
            typeMessageVisible || sendButtonVisible
        } catch (_: StaleObjectException) {
            false
        }
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

    fun assertSystemMessageVisible(message: String) = apply { waitElement(UiSelectorParams(textContains = message)) }

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

    fun assertGroupConversationInForeground(conversationName: String): ConversationViewPage {
        try {
            UiWaitUtils.waitElement(conversationDetailsGroup(conversationName))
        } catch (e: AssertionError) {
            throw AssertionError("Group conversation '$conversationName' is not in foreground.", e)
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
        val params = conversationDetails1On1(userName)
        UiWaitUtils.waitElement(backButton, timeout = UiWaitUtils.MEDIUM_TIMEOUT)

        val detailsOpened = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            UiWaitUtils.clickWhenClickable(
                params = params,
                timeout = UiWaitUtils.POLLING_DEFAULT,
                pollingInterval = UiWaitUtils.POLLING_FAST
            )
            findElementOrNull(conversationOptionsButton)?.let { !it.visibleBounds.isEmpty } == true
        }

        if (!detailsOpened) {
            throw AssertionError("1:1 conversation details for user '$userName' did not open.")
        }
        return this
    }

    fun clickOnGroupConversationDetails(userName: String): ConversationViewPage {
        val params = conversationDetailsGroup(userName)
        UiWaitUtils.waitElement(backButton, timeout = UiWaitUtils.MEDIUM_TIMEOUT)

        val clicked = UiWaitUtils.clickWhenClickable(
            params = params,
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_FAST
        )

        if (!clicked) {
            throw AssertionError("Group conversation details for user '$userName' was not clickable.")
        }

        try {
            UiWaitUtils.waitElement(conversationOptionsButton, timeout = UiWaitUtils.MEDIUM_TIMEOUT)
        } catch (e: AssertionError) {
            throw AssertionError("Group conversation details for user '$userName' did not open.", e)
        }
        return this
    }

    fun clickOnChannelConversationDetails(conversationName: String) = clickOnGroupConversationDetails(conversationName)

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

    fun iSeeSentQrCodeImageInCurrentConversation(): ConversationViewPage {

        try {
            UiWaitUtils.waitElement(sentQRImage)
        } catch (e: AssertionError) {
            throw AssertionError("Sent qrCodeImage is not visible in current conversation", e)
        }
        return this
    }

    fun assertImageIsVisible(): ConversationViewPage {
        UiWaitUtils.waitElement(sentQRImage)
        return this
    }

    fun assertImageNotVisible(): ConversationViewPage {
        assertElementNotVisible(sentQRImage, "image")
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
