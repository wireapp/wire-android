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

import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.test.DefaultAsserter.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class ConnectedUserProfilePage(private val device: UiDevice) {
    private val startConversationButton = UiSelectorParams(text = "Start Conversation")

    private val showMoreOptions = UiSelectorParams(description = "Open conversation options")

    private val blockOption = UiSelectorParams(text = "Block")

    private val blockedLabel = UiSelectorParams(text = "Blocked")

    private val unblockUserButton = UiSelectorParams(text = "Unblock User")
    private val blockButtonAlert = UiSelectorParams(text = "Block")
    private val participantRemoveFromConversationButton = UiSelectorParams(textContains = "Remove from conversation")

    private val removeConversationButtonOnModal = UiSelectorParams(text = "Remove")

    private val closeButton = UiSelectorParams(
        className = "android.view.View",
        description = "Close"
    )

    fun clickStartConversationButton(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(startConversationButton).click()
        return this
    }

    fun assertStartConversationButtonVisible(): ConnectedUserProfilePage {
        val button = UiWaitUtils.waitElement(startConversationButton)
        assertTrue(
            "Start Conversation button is not visible",
            !button.visibleBounds.isEmpty
        )
        return this
    }

    fun assertToastMessageIsDisplayed(
        expectedMessage: String,
        timeout: Duration = 5.seconds
    ): ConnectedUserProfilePage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(text = expectedMessage),
            timeout = timeout,
            errorMessage = "Toast message '$expectedMessage' was not displayed within ${timeout.inWholeMilliseconds}ms."
        )

        return this
    }

    fun tapCloseButtonOnConnectedUserProfilePage(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(closeButton).click()
        return this
    }

    fun clickShowMoreOptions(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(showMoreOptions).click()
        return this
    }

    fun clickBlockOption(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(blockOption).click()
        return this
    }

    fun clickBlockButtonAlert(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(blockButtonAlert).click()
        return this
    }

    fun assertBlockedLabelVisible(): ConnectedUserProfilePage {
        try {
            UiWaitUtils.waitElement(blockedLabel)
        } catch (e: AssertionError) {
            throw AssertionError("Blocked label is not visible", e)
        }
        return this
    }

    fun assertUnblockUserButtonVisible(): ConnectedUserProfilePage {
        try {
            UiWaitUtils.waitElement(unblockUserButton)
        } catch (e: AssertionError) {
            throw AssertionError("Unblock User button is not visible", e)
        }
        return this
    }

    fun tapRemoveFromConversationButtonForParticipant(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(participantRemoveFromConversationButton).click()
        return this
    }

    fun assertRemoveFromConversationButtonForParticipant(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(participantRemoveFromConversationButton)
        return this
    }

    fun tapRemoveConversationButtonOnModal(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(removeConversationButtonOnModal).click()
        return this
    }

    fun assetRemoveConversationButtonOnModal(): ConnectedUserProfilePage {
        UiWaitUtils.waitElement(removeConversationButtonOnModal)
        return this
    }
}
