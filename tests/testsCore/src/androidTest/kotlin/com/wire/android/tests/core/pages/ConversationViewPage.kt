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
import androidx.test.uiautomator.Until
import androidx.test.uiautomator.type
import junit.framework.TestCase.assertFalse
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.findElementOrNull
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertEquals

data class ConversationViewPage(private val device: UiDevice) {
    private fun displayedUserName(userName: String) = UiSelectorParams(text = userName)
    private val typeMessageField = UiSelectorParams(description = " Type a message")
    private val sentQRImage = UiSelectorParams(description = "Image message")
    private val attachNewFileButton = UiSelectorParams(description = "Attach new item to conversation")
    private val audioSeekBar = UiSelectorParams(className = "android.widget.SeekBar")
    private val audioInitialTime = UiSelectorParams(text = "00:00")
    private val playAudioButton = UiSelectorParams(description = "Play audio")

    private val startCallButton = UiSelectorParams(description = "Start audio call")
    private val pauseAudioButton = UiSelectorParams(description = "Pause audio")
    private val downloadButton = UiSelectorParams(text = "Download")

    private val modalTextLocator = UiSelectorParams(textContains = "save it to your device")

    private val saveButtonLocator = UiSelectorParams(text = "Save")

    private val saveButton = UiSelectorParams(text = "Save")

    private val openButton = UiSelectorParams(text = "Open")

    private val downloadButtonOnVideoFile = UiSelectorParams(text = "Tap to download")
    private val videoDurationLocator = UiSelectorParams(text = "00:03")

    private val messageInputField = UiSelectorParams(className = "android.widget.EditText")

    private fun conversationDetails1On1(userName: String) =
        UiSelector()
            .resourceId("User avatar")
            .fromParent(UiSelector().className("android.widget.TextView").text(userName))

    private val sendButton = UiSelectorParams(description = "Send")

    private val backButton = UiSelectorParams(description = "Go back to conversation list")

    private val selfDeleteTimerButton = UiSelectorParams(description = "Set timer for self-deleting messages")

    private val selfDeletingMessageLabel = UiSelectorParams(description = " Self-deleting message")

    private fun selfDeleteOption(label: String): UiSelectorParams {
        return UiSelectorParams(text = label, className = "android.widget.TextView")
    }

    private fun sharingOption(label: String): UiSelectorParams {
        return UiSelectorParams(text = label, className = "android.widget.TextView")
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
        val gone = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .wait(Until.gone(By.text("00:00")), 5000)
        assertTrue("Audio time is still at 00:00, expected it to have changed", gone)
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

    fun longPressOnAudioSeekBar(): ConversationViewPage {
        val seekBar = UiWaitUtils.waitElement(audioSeekBar)
        val center = seekBar.visibleCenter
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

    fun tapDownloadButton(): ConversationViewPage {
        UiWaitUtils.waitElement(downloadButton).click()
        return this
    }

    fun assertFileActionModalIsVisible(): ConversationViewPage {
        val modalText = UiWaitUtils.waitElement(modalTextLocator)
        assertTrue("The file action modal is not visible.", !modalText.visibleBounds.isEmpty)
        return this
    }

    fun tapSaveButtonOnModal(): ConversationViewPage {
        UiWaitUtils.waitElement(saveButtonLocator).click()
        return this
    }

    fun assertImageFileWithNameIsVisible(fileName: String): ConversationViewPage {
        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName))
        Assert.assertTrue("File with name '$fileName' is not visible", !fileNameElement.visibleBounds.isEmpty)
        return this
    }

    fun assertTextFileWithNameIsVisible(fileName3: String): ConversationViewPage {
        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName3))
        Assert.assertTrue("File with name '$fileName3' is not visible", !fileNameElement.visibleBounds.isEmpty)
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

    fun clickSaveButtonOnDownloadModal(): ConversationViewPage {
        UiWaitUtils.waitElement(saveButton).click()
        return this
    }

    fun clickOpenButtonOnDownloadModal(): ConversationViewPage {
        UiWaitUtils.waitElement(openButton).click()
        return this
    }

    fun assertFileSavedToastContain(partialText: String): ConversationViewPage {
        val toast = UiWaitUtils.waitElement(UiSelectorParams(textContains = partialText))

        Assert.assertTrue(
            "Toast message containing '$partialText' is not displayed.",
            !toast.visibleBounds.isEmpty
        )

        return this
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
        UiWaitUtils.waitElement(messageInputField).click()
        device.type(message)
        return this
    }

    fun clickSendButton(): ConversationViewPage {
        UiWaitUtils.waitElement(sendButton).click()
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

    fun assertMessageNotVisible(text: String) {
        val element = findElementOrNull(UiSelectorParams(text = text))

        Assert.assertTrue(
            "Message '$text' is still visible on the screen.",
            element == null || element.visibleBounds.isEmpty
        )
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

    fun click1On1ConversationDetails(userName: String): ConversationViewPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val userName = device.findObject(conversationDetails1On1(userName))
        if (!userName.exists()) throw AssertionError("User '$userName' not found in current conversation")
        userName.click()

        return this
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

    fun iSeeSentQrCodeImageInCurrentConversation(): ConversationViewPage {

        try {
            UiWaitUtils.waitElement(sentQRImage)
        } catch (e: AssertionError) {
            throw AssertionError("Sent qrCodeImage is not visible in current conversation", e)
        }
        return this
    }
}
