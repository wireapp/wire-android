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
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.toBySelector
import kotlin.time.Duration

data class GroupConversationDetailsPage(private val device: UiDevice) {
    private val groupNameInputField = UiSelectorParams(className = "android.widget.EditText")

    private val okButton = UiSelectorParams(text = "OK")

    private val showMoreOptionsButton = UiSelectorParams(description = "Open conversation options")

    private val deleteConversationButton = UiSelectorParams(text = "Delete Conversation")

    private val moveToArchiveButton = UiSelectorParams(text = "Move to Archive")

    private val confirmArchiveConversationButton = UiSelectorParams(text = "Archive")

    private val moveOutOfArchiveButton = UiSelectorParams(text = "Unarchive")

    private val deleteGroupButton = UiSelectorParams(text = "Delete")

    private val clearContentButton = UiSelectorParams(textContains = "Clear Content")

    private val clearContentConfirmButton = UiSelectorParams(text = "Clear content")

    private val leaveConversationButton = UiSelectorParams(text = "Leave Conversation")

    private val leaveConversationConfirmButton = UiSelectorParams(text = "Leave")

    private val participantsTab = UiSelectorParams(text = "PARTICIPANTS")

    private val appsTab = UiSelectorParams(text = "APPS")

    private val addParticipantsButton = UiSelectorParams(text = "Add participants")

    private val continueButton = UiSelectorParams(text = "Continue")

    private val closeButtonOnGroupConversationDetailsPage = UiSelectorParams(description = "Close conversation details")

    private val conversationDetailsHeading = UiSelectorParams(text = "Conversation Details")

    private val removeFromConversationButton = UiSelectorParams(text = "Remove From Conversation")

    private val addToConversationButton = UiSelectorParams(text = "Add To Conversation")

    private val notificationsButton = UiSelectorParams(text = "Notifications")

    private fun notificationStatusSelector(status: String) = UiSelectorParams(text = status)

    private val guestOptions = UiSelectorParams(text = "Guests")

    private val appsOptions = UiSelectorParams(text = "Apps")

    private val guestOptionsText = UiSelector().text("Guests")

    private val appsOptionsText = UiSelector().text("Apps")

    private fun textViewSelector(text: String) = UiSelectorParams(
        className = "android.widget.TextView",
        text = text
    )

    fun assertGroupDetailsPageVisible(): GroupConversationDetailsPage {
        try {
            UiWaitUtils.waitElement(conversationDetailsHeading)
        } catch (e: AssertionError) {
            throw AssertionError("Group details page is not visible.", e)
        }
        return this
    }

    fun tapShowMoreOptionsButton() {
        UiWaitUtils.waitElement(showMoreOptionsButton).click()
    }

    fun tapNotificationsButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(notificationsButton).click()
        return this
    }

    fun tapNotificationStatus(status: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(notificationStatusSelector(status)).click()
        return this
    }

    fun assertNotificationStatusVisible(status: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(notificationStatusSelector(status))
        return this
    }

    fun tapDeleteConversationButton() {
        UiWaitUtils.waitElement(deleteConversationButton).click()
    }

    fun assertDeleteConversationButtonNotVisible(): GroupConversationDetailsPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = deleteConversationButton.toBySelector(),
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            errorMessage = "Delete Conversation button is visible."
        )
        return this
    }

    fun tapMoveToArchiveButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(moveToArchiveButton).click()
        return this
    }

    fun tapConfirmArchiveConversationButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(confirmArchiveConversationButton).click()
        return this
    }

    fun tapMoveOutOfArchiveButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(moveOutOfArchiveButton).click()
        return this
    }

    fun tapClearContentButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(clearContentButton).click()
        return this
    }

    fun tapClearContentConfirmButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(clearContentConfirmButton).click()
        return this
    }

    fun tapLeaveConversationButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(leaveConversationButton).click()
        return this
    }

    fun tapLeaveConversationConfirmButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(leaveConversationConfirmButton).click()
        return this
    }

    fun assertToastMessageIsDisplayed(
        expectedMessage: String,
        timeout: Duration = UiWaitUtils.SHORT_TIMEOUT
    ): GroupConversationDetailsPage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(text = expectedMessage),
            timeout = timeout,
            errorMessage = "Toast message '$expectedMessage' was not displayed within ${timeout.inWholeMilliseconds}ms."
        )

        return this
    }

    fun tapDeleteGroupButton() {
        UiWaitUtils.waitElement(deleteGroupButton).click()
    }

    fun tapOnParticipantsTab() {
        UiWaitUtils.waitElement(participantsTab).click()
    }

    fun tapOnAppsTab(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(appsTab).click()
        return this
    }

    fun tapAddParticipantsButton() {
        UiWaitUtils.waitElement(addParticipantsButton).click()
    }

    fun assertUsernameInSuggestionsListIs(expectedHandle: String): GroupConversationDetailsPage {
        val handleSelector = textViewSelector(expectedHandle)
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
        val handleSelector = textViewSelector(expectedHandle)

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

    fun assertAppInSearchResultsVisible(appName: String): GroupConversationDetailsPage {
        return assertUsernameInSuggestionsListIs(appName)
    }

    fun tapAppInSearchResults(appName: String): GroupConversationDetailsPage {
        return selectUserInSuggestionList(appName)
    }

    fun tapContinueButton() {
        UiWaitUtils.waitElement(continueButton).click()
    }

    fun assertChannelNameVisible(expectedName: String): GroupConversationDetailsPage {
        try {
            UiWaitUtils.waitElement(textViewSelector(expectedName))
        } catch (e: AssertionError) {
            throw AssertionError("Expected channel name '$expectedName' is not visible.", e)
        }
        return this
    }

    fun tapOnChannelName(expectedName: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(textViewSelector(expectedName)).click()
        return this
    }

    fun changeChannelName(newName: String): GroupConversationDetailsPage {
        val channelNameInput = UiWaitUtils.waitElement(groupNameInputField)
        channelNameInput.text = ""
        channelNameInput.text = newName
        UiWaitUtils.waitElement(okButton).click()
        return this
    }

    fun assertGroupNameVisible(expectedName: String): GroupConversationDetailsPage {
        return assertChannelNameVisible(expectedName)
    }

    fun tapOnGroupName(expectedName: String): GroupConversationDetailsPage {
        return tapOnChannelName(expectedName)
    }

    fun changeGroupName(newName: String): GroupConversationDetailsPage {
        return changeChannelName(newName)
    }

    fun assertGuestOptionsState(expectedState: String): GroupConversationDetailsPage {
        return assertAccessOptionState(guestOptions, guestOptionsText, "Guests", expectedState)
    }

    fun tapGuestOptions(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(guestOptions).click()
        return this
    }

    fun assertAppsOptionsState(expectedState: String): GroupConversationDetailsPage {
        return assertAccessOptionState(appsOptions, appsOptionsText, "Apps", expectedState)
    }

    fun tapAppsOptions(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(appsOptions).click()
        return this
    }

    fun assertUsernameIsAddedToParticipantsList(expectedHandle: String): GroupConversationDetailsPage {
        val handleSelector = textViewSelector(expectedHandle)
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

    fun tapUserInParticipantsList(expectedHandle: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(textViewSelector(expectedHandle)).parent.click()
        return this
    }

    fun assertRemoveFromConversationButtonForAppVisible(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(removeFromConversationButton)
        return this
    }

    fun tapRemoveFromConversationButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(removeFromConversationButton).click()
        return this
    }

    fun assertRemoveFromConversationButtonNotVisible(): GroupConversationDetailsPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = removeFromConversationButton.toBySelector(),
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            errorMessage = "Remove From Conversation button is still visible."
        )
        return this
    }

    fun assertAddToConversationButtonVisible(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(addToConversationButton)
        return this
    }

    fun tapAddToConversationButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(addToConversationButton).click()
        return this
    }

    fun tapBackButton(): GroupConversationDetailsPage {
        device.pressBack()
        return this
    }

    fun assertUserIsNotInParticipantsList(expectedHandle: String): GroupConversationDetailsPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = textViewSelector(expectedHandle).toBySelector(),
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            errorMessage = "User '$expectedHandle' is still visible in participants list."
        )
        return this
    }

    fun tapCloseButtonOnGroupConversationDetailsPage(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(closeButtonOnGroupConversationDetailsPage).click()
        return this
    }

    fun tapCloseButtonOnChannelConversationDetailsPage(): GroupConversationDetailsPage {
        return tapCloseButtonOnGroupConversationDetailsPage()
    }

    private fun assertAccessOptionState(
        optionSelector: UiSelectorParams,
        optionTextSelector: UiSelector,
        optionName: String,
        expectedState: String
    ): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(optionSelector)
        val option = device.findObject(optionTextSelector)
        val state = option.getFromParent(UiSelector().text(expectedState))
        assertTrue(
            "$optionName option is not in $expectedState state.",
            state.exists() && !state.visibleBounds.isEmpty
        )
        return this
    }
}
