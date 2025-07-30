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
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.findElementOrNull

data class ConversationListPage(private val device: UiDevice) {

    private val searchField = UiSelectorParams(description = "Search conversations")

    private val conversationNameSelector: (String) -> UiSelectorParams = { conversationName ->
        UiSelectorParams(text = conversationName)
    }

    fun tapSearchConversationField(): ConversationListPage {
        val element = UiWaitUtils.waitElement(searchField)
        element.click()
        return this
    }

    fun typeFirstNCharsInSearchField(fullText: String, charCount: Int): ConversationListPage {
        val trimmedText = fullText.take(charCount)
        val searchFieldElement = UiWaitUtils.waitElement(searchField)
        searchFieldElement.click()

        val encodedText = trimmedText.replace(" ", "%s")
        UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .executeShellCommand("input text $encodedText") // removed single quotes

        return this
    }


    fun clickGroupConversation(conversationName: String): ConversationListPage {
        val conversation = UiWaitUtils.waitElement(UiSelectorParams(text = conversationName))
        conversation.click()
        return this
    }

    private fun unreadCountSelector(count: String): UiSelectorParams {
        return UiSelectorParams(text = count)
    }

    fun assertUnreadMessagesCount(expectedCount: String): ConversationListPage {
        val unreadCount = UiWaitUtils.waitElement(unreadCountSelector(expectedCount))
        Assert.assertTrue("Unread message count '$expectedCount' not visible", !unreadCount.visibleBounds.isEmpty)
        return this
    }

    fun assertConversationNotVisible(conversationName: String): ConversationListPage {
        val conversation = findElementOrNull(conversationNameSelector(conversationName))
        Assert.assertTrue(
            "❌ Conversation '$conversationName' is still visible.",
            conversation == null || conversation.visibleBounds.isEmpty
        )
        return this
    }
}
