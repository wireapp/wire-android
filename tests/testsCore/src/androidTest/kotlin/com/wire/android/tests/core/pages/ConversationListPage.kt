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

import android.os.SystemClock
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.StaleObjectException
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.findElementOrNull
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.seconds

data class ConversationListPage(private val device: UiDevice) {

    private val searchField = UiSelectorParams(description = "Search conversations")
    private val userProfileButtonNoPhoto = UiSelectorParams(description = "Your profile")

    private val userProfileButton = UiSelectorParams(resourceId = "User avatar")
    private val conversationListHeading = UiSelectorParams(
        textContains = "Conversations"
    )
    private val mainMenuButton = UiSelectorParams(description = "Main navigation")
    private val settingsButton = UiSelectorParams(text = "Settings")

    private val conversationsButton = UiSelectorParams(text = "Conversations")

    private fun displayedUserName(userName: String) = UiSelectorParams(text = userName)
    private val conversationNameSelector: (String) -> UiSelectorParams = { conversationName ->
        UiSelectorParams(text = conversationName)
    }
    private val startNewConversation = UiSelectorParams(description = "New. Start a new conversation")

    private val userConversationNamePendingLabelSelector =
        UiSelector().description("pending approval of connection request")
    private val pendingApprovalLabel = UiSelectorParams(description = "pending approval of connection request")
    fun assertConversationListVisible(): ConversationListPage {
        val heading = UiWaitUtils.waitElement(conversationListHeading)
        Assert.assertTrue(
            "Conversation list heading is not visible",
            !heading.visibleBounds.isEmpty
        )
        return this
    }

    fun clickConversationsMenuEntry(): ConversationListPage {
        UiWaitUtils.waitElement(mainMenuButton).click()
        return this
    }

    /**
     * The navigation drawer can appear before the Settings entry is fully attached and clickable.
     * Retry for a short window, reopening the drawer at a throttled pace until the Settings row is stable.
     */
    fun clickSettingsButtonOnMenuEntry(timeout: Duration = 10.seconds): ConversationListPage {
        var lastMenuClickAt = 0L

        val success = UiWaitUtils.retryUntilTimeout(
            timeout = timeout,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            if (
                UiWaitUtils.clickWhenClickable(
                    settingsButton,
                    timeout = UiWaitUtils.POLLING_DEFAULT,
                    pollingInterval = UiWaitUtils.POLLING_FAST
                )
            ) {
                true
            } else {
                lastMenuClickAt = reopenMenuIfNeeded(lastMenuClickAt)
                false
            }
        }

        if (!success) {
            throw AssertionError("Settings menu entry was not found within ${timeout.inWholeMilliseconds}ms.")
        }
        return this
    }

    private fun reopenMenuIfNeeded(lastMenuClickAt: Long, minIntervalMs: Long = 600L): Long {
        val now = SystemClock.uptimeMillis()
        if (
            now - lastMenuClickAt < minIntervalMs ||
            !UiWaitUtils.clickWhenClickable(
                mainMenuButton,
                timeout = UiWaitUtils.POLLING_DEFAULT,
                pollingInterval = UiWaitUtils.POLLING_FAST
            )
        ) {
            return lastMenuClickAt
        }

        device.waitForIdle(300)
        return now
    }

    private fun tryClickIfVisible(selector: UiSelectorParams): Boolean {
        val element = UiWaitUtils.findElementOrNull(selector) ?: return false
        return try {
            if (!element.visibleBounds.isEmpty && element.isEnabled) {
                element.click()
                true
            } else {
                false
            }
        } catch (_: StaleObjectException) {
            false
        }
    }

    fun clickConversationsButtonOnMenuEntry(): ConversationListPage {
        UiWaitUtils.waitElement(conversationsButton).click()
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
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(text = conversationName),
            timeout = 10.seconds,
            errorMessage = "Group conversation '$conversationName' was not found."
        )

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
            "Conversation '$conversationName' is still visible.",
            conversation == null || conversation.visibleBounds.isEmpty
        )
        return this
    }

    fun tapStartNewConversationButton(): ConversationListPage {
        UiWaitUtils.waitElement(startNewConversation).click()
        return this
    }

    fun clickCloseButtonOnNewConversationScreen(timeout: Duration = 5.seconds): ConversationListPage {
        val closeButton = UiSelectorParams(
            className = "android.view.View",
            description = "Close new conversation view"
        )

        val closed = UiWaitUtils.retryUntilTimeout(
            timeout = timeout,
            pollingInterval = UiWaitUtils.POLLING_DEFAULT
        ) {
            runCatching {
                UiWaitUtils.waitElement(closeButton, timeout = UiWaitUtils.POLLING_DEFAULT).click()
            }
            UiWaitUtils.findElementOrNull(conversationListHeading)?.let { !it.visibleBounds.isEmpty } == true
        }
        if (!closed) {
            throw AssertionError("Conversation list was not visible again within ${timeout.inWholeMilliseconds}ms")
        }

        return this
    }

    fun tapUnreadConversationNameInConversationList(userName: String): ConversationListPage {
        val userName = UiWaitUtils.waitElement(UiSelectorParams(text = userName))
        userName.click()
        return this
    }

    @Suppress("ThrowsCount")
    fun assertConversationNameWithPendingStatusVisibleInConversationList(userName: String): ConversationListPage {
        try {
            UiWaitUtils.waitUntilVisibleOrThrow(
                params = UiSelectorParams(text = userName),
                timeout = 10.seconds,
                errorMessage = "User '$userName' is not visible in the conversation list"
            )
        } catch (e: Throwable) {
            throw AssertionError("User '$userName' is not visible in the conversation list", e)
        }

        try {
            UiWaitUtils.waitUntilVisibleOrThrow(
                params = pendingApprovalLabel,
                timeout = 10.seconds,
                errorMessage = "Pending status is not visible for user '$userName'"
            )
        } catch (e: Throwable) {
            throw AssertionError("Pending status is not visible for user '$userName'", e)
        }

        return this
    }

    fun assertPendingStatusIsNoLongerVisible(): ConversationListPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        UiWaitUtils.waitUntilElementGone(
            device = device,
            selector = userConversationNamePendingLabelSelector,
            timeout = 10.seconds,
            pollingInterval = 250.milliseconds
        )

        return this
    }

    fun tapConversationNameInConversationList(userName: String): ConversationListPage {
        val userName = UiWaitUtils.waitElement(UiSelectorParams(text = userName))
        userName.click()
        return this
    }

    fun clickUserProfileButton(): ConversationListPage {
        val buttonWithPhoto = UiWaitUtils.findElementOrNull(userProfileButton)
        if (buttonWithPhoto != null && !buttonWithPhoto.visibleBounds.isEmpty) {
            buttonWithPhoto.click()
        } else {
            val buttonNoPhoto = UiWaitUtils.waitElement(userProfileButtonNoPhoto)
            buttonNoPhoto.click()
        }
        return this
    }

    fun assertConversationIsVisibleWithTeamOwner(userName: String): ConversationListPage {
        try {
            UiWaitUtils.waitElement(displayedUserName(userName))
        } catch (e: AssertionError) {
            throw AssertionError("Team owner name '$userName' is not visible in conversation view", e)
        }
        return this
    }
}
