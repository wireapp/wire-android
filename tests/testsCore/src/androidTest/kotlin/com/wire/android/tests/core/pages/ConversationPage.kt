package com.wire.android.tests.core.pages
import kotlin.test.DefaultAsserter.assertTrue


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

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until
import junit.framework.TestCase.assertFalse
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.test.assertEquals
import kotlin.test.assertFalse

data class ConversationPage(private val device: UiDevice) {

     private val mainMenuButton = UiSelectorParams(description = "Main navigation")
    private val settingsButton = UiSelectorParams(text = "Settings")
    private fun displayedUserName(userName: String) = UiSelectorParams(text = userName)
//    private val audioSeekBar = UiSelectorParams(className = "android.widget.SeekBar")
//    private val audioInitialTime = UiSelectorParams(text = "00:00")
//
//    private val playAudioButton = UiSelectorParams(description = "Play audio")
//
//    private val pauseAudioButton = UiSelectorParams(description = "Pause audio")
//    private val downloadButton = UiSelectorParams(text = "Download")
//
//    private val modalTextLocator = UiSelectorParams(textContains = "save it to your device")
//
//    private val saveButtonLocator = UiSelectorParams(text = "Save")
//
//    private val saveButton = UiSelectorParams(text = "Save")
//
//    private val openButton = UiSelectorParams(text = "Open")
//
//    private val downloadButtonOnVideoFile = UiSelectorParams(text = "Tap to download")
//    private val videoDurationLocator = UiSelectorParams(text = "00:03")
//
//    private fun fileWithName(name: String): UiSelectorParams {
//        return UiSelectorParams(text = name)
//    }
//
//
    fun clickMainMenuButtonOnConversationPage(): ConversationPage {
        UiWaitUtils.waitElement(mainMenuButton).click()
        return this
    }

    fun clickSettingsButtonOnMenuEntry(): ConversationPage {
        UiWaitUtils.waitElement(settingsButton).click()
        return this
    }

    fun assertGroupConversationVisible(conversationName: String): ConversationPage {
        val conversation = UiWaitUtils.waitElement(UiSelectorParams(text = conversationName))
        assertTrue("Conversation '$conversationName' is not visible", !conversation.visibleBounds.isEmpty)
        return this
    }

//    fun assertGroupConversationVisible(conversationName: String): ConversationPage {
//        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//
//        // Wait for the element to appear
//        UiWaitUtils.waitElement(UiSelectorParams(text = conversationName))
//
//        // Re-fetch the element to avoid stale reference
//        val conversation = device.findObject(UiSelector().text(conversationName))
//
//        assertTrue(
//            "Conversation '$conversationName' is not visible",
//            !conversation.visibleBounds.isEmpty
//        )
//
//        return this
//    }


    fun clickConnectionRequestOfUser(userName: String): ConversationPage {
        val teamMemberName = UiWaitUtils.waitElement(displayedUserName(userName))
        teamMemberName.click()
        return this
    }

    fun assertConnectionRequestNameIs(userName: String): ConversationPage {
        val teamMemberName = UiWaitUtils.waitElement(displayedUserName(userName))
        assertTrue("Team member name '$userName' is not visible", !teamMemberName.visibleBounds.isEmpty)
        return this
    }
//
//    fun assertConversationIsVisibleWithTeamMember(userName: String): ConversationPage {
//        val teamMemberName = UiWaitUtils.waitElement(displayedUserName(userName))
//        assertTrue("Team member name '$userName' is not visible in conversation view", !teamMemberName.visibleBounds.isEmpty)
//        return this
//    }
//
//    fun assertAudioMessageIsVisible(): ConversationPage {
//        val seekBar = UiWaitUtils.waitElement(audioSeekBar)
//        Assert.assertTrue("Audio file is not visible", !seekBar.visibleBounds.isEmpty)
//        return this
//    }
//
//    fun assertAudioTimeStartsAtZero(): ConversationPage {
//        val timeAtZero = UiWaitUtils.waitElement(audioInitialTime)
//        assertTrue("Expected audio to start at 00:00", !timeAtZero.visibleBounds.isEmpty)
//        return this
//    }
//
//    fun assertAudioTimeIsNotZeroAnymore(): ConversationPage {
//        val gone = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//            .wait(Until.gone(By.text("00:00")), 5000)
//        assertTrue("Audio time is still at 00:00, expected it to have changed", gone)
//        return this
//    }
//
//    fun clickPlayButtonOnAudioMessage(): ConversationPage {
//        val button = UiWaitUtils.waitElement(playAudioButton)
//        requireNotNull(button) { "❌ Play button with description 'Play audio' not found" }
//        button.click()
//        return this
//    }
//
//    fun clickPauseButtonOnAudioMessage(): ConversationPage {
//        val button = UiWaitUtils.waitElement(pauseAudioButton)
//        requireNotNull(button) { "❌ Pause button with description 'Play audio' not found" }
//        button.click()
//        return this
//    }
//
//    fun longPressOnAudioSeekBar(): ConversationPage {
//        val seekBar = UiWaitUtils.waitElement(audioSeekBar)
//        val center = seekBar.visibleCenter
//        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//            .swipe(center.x, center.y, center.x, center.y, 120)
//
//        return this
//    }
//
//    fun assertBottomSheetIsVisible(): ConversationPage {
//        val bottomSheet = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//            .findObject(UiSelector().className("android.view.View").instance(4))
//        assertTrue("Bottom sheet is not visible", !bottomSheet.visibleBounds.isEmpty)
//        return this
//    }
//
//    fun assertBottomSheetButtonsVisible_ReactionsDetailsReplyDownloadShareOpenDelete(): ConversationPage {
//        val expectedButtons = listOf(
//            "REACTIONS",
//            "Message Details",
//            "Reply",
//            "Download",
//            "Share",
//            "Open",
//            "Delete"
//        )
//
//        expectedButtons.forEach { expectedText ->
//            val element = UiWaitUtils.waitElement(UiSelectorParams(text = expectedText))
//            assertTrue("Button with text '$expectedText' is not visible", !element.visibleBounds.isEmpty)
//            assertEquals(expectedText, element.text, "Button text does not match expected")
//        }
//
//        return this
//    }
//
//    fun tapDownloadButton(): ConversationPage {
//        UiWaitUtils.waitElement(downloadButton).click()
//        return this
//    }
//
//    fun assertFileActionModalIsVisible(): ConversationPage {
//        val modalText = UiWaitUtils.waitElement(modalTextLocator)
//        assertTrue("The file action modal is not visible.", !modalText.visibleBounds.isEmpty)
//        return this
//    }
//
//    fun tapSaveButtonOnModal(): ConversationPage {
//        UiWaitUtils.waitElement(saveButtonLocator).click()
//        return this
//    }
//
//    fun assertImageFileWithNameIsVisible(fileName: String): ConversationPage {
//        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName))
//        Assert.assertTrue("File with name '$fileName' is not visible", !fileNameElement.visibleBounds.isEmpty)
//        return this
//    }
//
//    fun assertTextFileWithNameIsVisible(fileName3: String): ConversationPage {
//        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName3))
//        Assert.assertTrue("File with name '$fileName3' is not visible", !fileNameElement.visibleBounds.isEmpty)
//        return this
//    }
//
//
//    fun clickFileWithName(fileName: String): ConversationPage {
//        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName))
//        fileNameElement.click()
//        return this
//    }
//
//    fun clickTextFileWithName(fileName3: String): ConversationPage {
//        val fileNameElement = UiWaitUtils.waitElement(fileWithName(fileName3))
//        fileNameElement.click()
//        return this
//    }
//
//    fun assertDownloadModalButtonsAreVisible_Open_Save_Cancel(): ConversationPage {
//        val expectedButtons = listOf(
//            "Open",
//            "Save",
//            "Cancel"
//        )
//
//        expectedButtons.forEach { expectedText ->
//            val element = UiWaitUtils.waitElement(UiSelectorParams(text = expectedText))
//            assertTrue("Button with text '$expectedText' is not visible on the modal", !element.visibleBounds.isEmpty)
//            assertEquals(expectedText, element.text, "Button text does not match expected")
//        }
//
//        return this
//    }
//
//
//    fun clickSaveButtonOnDownloadModal(): ConversationPage {
//        UiWaitUtils.waitElement(saveButton).click()
//        return this
//    }
//
//    fun clickOpenButtonOnDownloadModal(): ConversationPage {
//        UiWaitUtils.waitElement(openButton).click()
//        return this
//    }
//
//
//    fun assertFileSavedToastContain(partialText: String): ConversationPage {
//        val toast = UiWaitUtils.waitElement(UiSelectorParams(textContains = partialText))
//
//        Assert.assertTrue(
//            "Toast message containing '$partialText' is not displayed.",
//            !toast.visibleBounds.isEmpty
//        )
//
//        return this
//    }
//
//    fun scrollToBottomOfConversationScreen() {
//        try {
//            val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//
//            // Create a scrollable container (finds first scrollable layout on screen)
//            val scrollable = UiScrollable(UiSelector().scrollable(true))
//            scrollable.setAsVerticalList()
//
//            // Perform fling (fast scroll) to the bottom
//            val success = scrollable.flingToEnd(10)
//            println("✅ Scrolled to bottom: $success")
//        } catch (e: Exception) {
//            println("❌ Failed to scroll: ${e.message}")
//        }
//    }
//
//    fun tapDownloadButtonOnVideoFile(): ConversationPage {
//        UiWaitUtils.waitElement(downloadButtonOnVideoFile).click()
//        return this
//    }
//
//    fun tapToPlayVideoFile(): ConversationPage {
//        UiWaitUtils.waitElement(videoDurationLocator).click()
//        return this
//    }
//
//    fun assertWireAppIsNotInForeground() {
//        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//        val currentPackage = device.currentPackageName
//        assertFalse(
//            "❌ Wire app is still in foreground: $currentPackage",
//            currentPackage.contains("APP_")
//        )
    }
