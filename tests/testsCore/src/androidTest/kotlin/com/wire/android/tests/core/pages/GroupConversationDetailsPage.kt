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

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.type
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class GroupConversationDetailsPage(private val device: UiDevice) {

    private val showMoreOptionsButton = UiSelectorParams(description = "Open conversation options")

    private val deleteConversationButton = UiSelectorParams(text = "Delete Conversation")

    private val removeGroupButton = UiSelectorParams(text = "Remove")

    private val participantsTab = UiSelectorParams(text = "PARTICIPANTS")

    private val addParticipantsButton = UiSelectorParams(text = "Add participants")

    private val continueButton = UiSelectorParams(text = "Continue")

    private val closeButtonOnGroupConversationDetailsPage = UiSelectorParams(description = "Close conversation details")

    fun tapShowMoreOptionsButton() {
        UiWaitUtils.waitElement(showMoreOptionsButton).click()
    }

    fun tapDeleteConversationButton() {
        UiWaitUtils.waitElement(deleteConversationButton).click()
    }

    fun tapRemoveGroupButton() {
        UiWaitUtils.waitElement(removeGroupButton).click()
    }

    fun tapOnParticipantsTab() {
        UiWaitUtils.waitElement(participantsTab).click()
    }

    fun tapAddParticipantsButton() {
        UiWaitUtils.waitElement(addParticipantsButton).click()

    }

    fun assertUsernameInSuggestionsListIs(expectedHandle: String): GroupConversationDetailsPage {
        val handleSelector = UiSelectorParams(
            className = "android.widget.TextView",
            text = expectedHandle
        )
        try {
            UiWaitUtils.waitElement(params = handleSelector)
        } catch (e: AssertionError) {
            throw AssertionError(
                "Expected user name in suggestion results to be '$expectedHandle' but its not '$expectedHandle'",
                e
            )
        }
        return this
    }

    fun selectUserInSuggestionList(expectedHandle: String): GroupConversationDetailsPage {
        val handleSelector = UiSelectorParams(
            className = "android.widget.TextView",
            text = expectedHandle
        )

        val handleTextView = try {
            UiWaitUtils.waitElement(params = handleSelector)
        } catch (e: AssertionError) {
            throw AssertionError(
                "Expected user name '$expectedHandle' was not found in suggestion list",
                e
            )
        }

        handleTextView.parent.click()

        return this
    }

    fun tapContinueButton() {
        UiWaitUtils.waitElement(continueButton).click()
    }

    fun assertUsernameIsAddedToParticipantsList(expectedHandle: String): GroupConversationDetailsPage {
        val handleSelector = UiSelectorParams(
            className = "android.widget.TextView",
            text = expectedHandle
        )
        try {
            UiWaitUtils.waitElement(params = handleSelector)
        } catch (e: AssertionError) {
            throw AssertionError(
                "Expected user name in participants list results to be '$expectedHandle' but its not '$expectedHandle'",
                e
            )
        }
        return this
    }

    fun tapCloseButtonOnGroupConversationDetailsPage(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(closeButtonOnGroupConversationDetailsPage).click()
        return this
    }
}
