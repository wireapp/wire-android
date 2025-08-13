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
import backendUtils.team.TeamHelper
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.findElementOrNull
import user.usermanager.ClientUserManager
import user.utils.ClientUser
import kotlin.test.DefaultAsserter.assertTrue

data class ConversationListPage(private val device: UiDevice) {

    private val searchField = UiSelectorParams(description = "Search conversations")

    private val conversationListHeading = UiSelectorParams(
        textContains = "Conversations"
    )
    private val mainMenuButton = UiSelectorParams(description = "Main navigation")
    private val settingsButton = UiSelectorParams(text = "Settings")
    private fun displayedUserName(userName: String) = UiSelectorParams(text = userName)
    private val conversationNameSelector: (String) -> UiSelectorParams = { conversationName ->
        UiSelectorParams(text = conversationName)
    }
    private val startNewConversation = UiSelectorParams(description = "Search for people or create a new conversation")
    private val backArrowButtonInsideSearchField = UiSelectorParams(
        className = "android.view.View",
        description = "Go back to add participants view"
    )

    private val closeNewConversationButton = UiSelectorParams(
        description = "Close new conversation view"
    )

    fun assertConversationListVisible(): ConversationListPage {
        val heading = UiWaitUtils.waitElement(conversationListHeading)
        Assert.assertTrue(
            "❌ Conversation list heading is not visible",
            !heading.visibleBounds.isEmpty
        )
        return this
    }
    fun clickMainMenuButtonOnConversationPage(): ConversationListPage {
        UiWaitUtils.waitElement(mainMenuButton).click()
        return this
    }

    fun clickSettingsButtonOnMenuEntry(): ConversationListPage {
        UiWaitUtils.waitElement(settingsButton).click()
        return this
    }

    fun assertGroupConversationVisible(conversationName: String): ConversationListPage {
        val conversation = UiWaitUtils.waitElement(UiSelectorParams(text = conversationName))
        assertTrue("Conversation '$conversationName' is not visible", !conversation.visibleBounds.isEmpty)
        return this
    }

    fun clickConnectionRequestOfUser(userName: String): ConversationListPage {
        val teamMemberName = UiWaitUtils.waitElement(displayedUserName(userName))
        teamMemberName.click()
        return this
    }

    fun assertConnectionRequestNameIs(userName: String): ConversationListPage {
        val teamMemberName = UiWaitUtils.waitElement(displayedUserName(userName))
        assertTrue("Team member name '$userName' is not visible", !teamMemberName.visibleBounds.isEmpty)
        return this
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
            "❌ Conversation '$conversationName' is still visible.", conversation == null || conversation.visibleBounds.isEmpty
        )
        return this
    }

    fun tapStartNewConversationButton(): ConversationListPage {
        UiWaitUtils.waitElement(startNewConversation).click()
        return this
    }

    fun tapBackArrowButtonInsideSearchField(): ConversationListPage {
        val button = UiWaitUtils.waitElement(backArrowButtonInsideSearchField)
        button.click()
        return this
    }

    fun clickCloseButtonOnNewConversationScreen(): ConversationListPage {
        UiWaitUtils.waitElement(closeNewConversationButton).click()
        return this
    }

    fun tapUnreadConversationNameInConversationList(userName: String): ConversationListPage {
        val userName = UiWaitUtils.waitElement(UiSelectorParams(text = userName))
        userName.click()
        return this
    }
}
