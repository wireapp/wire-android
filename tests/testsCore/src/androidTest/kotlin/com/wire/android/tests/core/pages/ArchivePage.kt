/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import androidx.test.uiautomator.UiDevice
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.SHORT_TIMEOUT
import uiautomatorutils.UiWaitUtils.toBySelector
import kotlin.time.Duration

data class ArchivePage(private val device: UiDevice) {
    private val moveOutOfArchiveButton = UiSelectorParams(text = "Unarchive")
    private val archivedConversationNameSelector: (String) -> UiSelectorParams = { conversationName ->
        UiSelectorParams(text = conversationName)
    }

    fun assertConversationVisibleInArchiveList(conversationName: String): ArchivePage {
        val conversation = UiWaitUtils.waitElement(archivedConversationNameSelector(conversationName))
        Assert.assertTrue(
            "Conversation '$conversationName' is not visible in archive list",
            !conversation.visibleBounds.isEmpty
        )
        return this
    }

    fun assertConversationNotVisibleInArchiveList(
        conversationName: String,
        timeout: Duration = SHORT_TIMEOUT
    ): ArchivePage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = archivedConversationNameSelector(conversationName).toBySelector(),
            timeout = timeout,
            errorMessage = "Conversation '$conversationName' is still visible in archive list."
        )
        return this
    }

    fun tapConversationNameInArchiveList(conversationName: String): ArchivePage {
        UiWaitUtils.waitElement(archivedConversationNameSelector(conversationName)).click()
        return this
    }

    fun longPressConversationInArchiveList(conversationName: String): ArchivePage {
        val conversation = UiWaitUtils.waitElement(archivedConversationNameSelector(conversationName))
        val center = conversation.visibleCenter
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .swipe(center.x, center.y, center.x, center.y, 120)
        return this
    }

    fun tapMoveOutOfArchiveButton(): ArchivePage {
        UiWaitUtils.waitElement(moveOutOfArchiveButton).click()
        return this
    }

    fun assertToastMessageIsDisplayedOnArchiveList(
        expectedMessage: String,
        timeout: Duration = SHORT_TIMEOUT
    ): ArchivePage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(text = expectedMessage),
            timeout = timeout,
            errorMessage = "Toast message '$expectedMessage' was not displayed within ${timeout.inWholeMilliseconds}ms."
        )
        return this
    }
}
