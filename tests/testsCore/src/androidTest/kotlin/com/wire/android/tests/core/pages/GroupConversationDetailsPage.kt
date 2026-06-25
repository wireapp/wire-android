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
import uiautomatorutils.UiWaitUtils.toBySelector

data class GroupConversationDetailsPage(private val device: UiDevice) {
    private val groupNameInputField = UiSelectorParams(className = "android.widget.EditText")

    private val okButton = UiSelectorParams(text = "OK")

    private val showMoreOptionsButton = UiSelectorParams(description = "Open conversation options")

    private val deleteConversationButton = UiSelectorParams(text = "Delete Conversation")

    private val removeGroupButton = UiSelectorParams(text = "Remove")
    private val moveToArchiveOption = UiSelectorParams(text = "Move to Archive")
    private val unarchiveOption = UiSelectorParams(text = "Unarchive")
    private val archiveButtonAlert = UiSelectorParams(text = "Archive")
    private val clearContentOption = UiSelectorParams(textContains = "Clear Content")
    private val clearContentConfirmationButton = UiSelectorParams(text = "Clear content")
    private val leaveConversationOption = UiSelectorParams(text = "Leave Conversation")
    private val leaveConversationConfirmationButton = UiSelectorParams(text = "Leave")

    private val participantsTab = UiSelectorParams(text = "PARTICIPANTS")
    private val guestsOption = UiSelectorParams(text = "Guests")
    private val toggleSwitch = UiSelectorParams(className = "android.widget.Switch")
    private val disableGuestAccessDialog = UiSelectorParams(text = "Disable guest access?")
    private val disableButton = UiSelectorParams(text = "Disable")

    private val addParticipantsButton = UiSelectorParams(text = "Add participants")

    private val continueButton = UiSelectorParams(text = "Continue")

    private val closeButtonOnGroupConversationDetailsPage = UiSelectorParams(description = "Close conversation details")

    private val conversationDetailsHeading = UiSelectorParams(text = "Conversation Details")
    private val groupNameLabel = UiSelectorParams(text = "GROUP NAME")

    private val removeFromConversationButton = UiSelectorParams(text = "Remove From Conversation")

    private val addToConversationButton = UiSelectorParams(text = "Add To Conversation")
    private val notificationsButton = UiSelectorParams(text = "Notifications")
    private val defaultNotificationsStatus = UiSelectorParams(text = "Everything")

    private fun textViewSelector(text: String) = UiSelectorParams(
        className = "android.widget.TextView",
        text = text
    )

    fun assertGroupDetailsPageVisible(): GroupConversationDetailsPage {
        val isVisible = UiWaitUtils.retryUntilTimeout(timeout = UiWaitUtils.LONG_TIMEOUT) {
            UiWaitUtils.findElementOrNull(conversationDetailsHeading) != null ||
                    UiWaitUtils.findElementOrNull(groupNameLabel) != null ||
                    UiWaitUtils.findElementOrNull(participantsTab) != null ||
                    UiWaitUtils.findElementOrNull(guestsOption) != null
        }
        if (!isVisible) {
            throw AssertionError("Group details page is not visible.")
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

    fun assertDefaultNotificationStatusIsEverything(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(defaultNotificationsStatus)
        return this
    }

    fun tapNotificationStatus(status: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = status)).click()
        return this
    }

    fun assertNotificationStatusIs(status: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = status))
        return this
    }

    fun tapDeleteConversationButton() {
        UiWaitUtils.waitElement(deleteConversationButton).click()
    }

    fun tapMoveToArchiveOption(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(moveToArchiveOption).click()
        return this
    }

    fun tapUnarchiveOption(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(unarchiveOption).click()
        return this
    }

    fun tapArchiveButtonAlert(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(archiveButtonAlert).click()
        return this
    }

    fun tapClearContentOption(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(clearContentOption).click()
        return this
    }

    fun tapClearContentConfirmationButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(clearContentConfirmationButton).click()
        return this
    }

    fun tapLeaveConversationOption(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(leaveConversationOption).click()
        return this
    }

    fun tapLeaveConversationConfirmationButton(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(leaveConversationConfirmationButton).click()
        return this
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

    fun assertGuestsOptionStateIs(expectedState: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(guestsOption)
        UiWaitUtils.waitElement(UiSelectorParams(text = expectedState))
        return this
    }

    fun tapGuestsOption(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(guestsOption).click()
        return this
    }

    fun assertGuestsSwitchStateIs(expectedState: String): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(guestsOption)
        UiWaitUtils.waitElement(UiSelectorParams(text = expectedState))
        return this
    }

    fun tapGuestsSwitch(): GroupConversationDetailsPage {
        val switch = UiWaitUtils.findElementOrNull(toggleSwitch)
        if (switch != null) {
            switch.click()
            return this
        }

        val guests = UiWaitUtils.waitElement(guestsOption)
        device.click(device.displayWidth - SWITCH_TRAILING_OFFSET_PX, guests.visibleCenter.y)
        return this
    }

    fun tapDisableButtonOnGuestAccessDialog(): GroupConversationDetailsPage {
        UiWaitUtils.waitElement(disableGuestAccessDialog)
        UiWaitUtils.waitElement(disableButton).click()
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

    private companion object {
        const val SWITCH_TRAILING_OFFSET_PX = 120
    }
}
