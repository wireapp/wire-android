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
import androidx.test.uiautomator.UiScrollable
import androidx.test.uiautomator.UiSelector
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.toBySelector
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

data class SelfUserProfilePage(private val device: UiDevice) {

    private val userProfilePageTitle = UiSelectorParams(text = "User Profile")
    private val logoutButton = UiSelectorParams(text = "Log out")
    private val clearDataAlert = UiSelectorParams(text = "Clear Data?")
    private val newTeamOrAddAccountButton = UiSelectorParams(text = "New Team or Add Account")
    private val newAccountButton = UiSelectorParams(resourceId = "New Team or Account")
    private val otherAccountsHeader = UiSelectorParams(text = "YOUR OTHER ACCOUNTS")
    private val cancelLoginDialogTitle = UiSelectorParams(text = "Are you sure you want to cancel?")
    private val cancelLoginDialogCancelButton = UiSelectorParams(text = "Cancel")
    private val removedDeviceDialogTitle = UiSelectorParams(text = "Removed Device")
    private val okButton = UiSelectorParams(text = "OK")
    private val availabilitySelector = UiSelectorParams(text = "None")
    private val availabilityLabel = UiSelectorParams(text = "AVAILABILITY")
    private val statusChangeInfoText = UiSelectorParams(textContains = "You will")
    private val closeProfileButton = UiSelectorParams(description = "Close your profile")

    private val infoTextCheckbox = UiSelectorParams(className = "android.widget.CheckBox")

    private fun activeAccountName(accountName: String) =
        UiSelectorParams(description = "Profile name, $accountName")

    private fun availabilityStatus(status: String) = UiSelectorParams(text = status)

    fun iSeeUserProfilePage(timeout: Duration = 30.seconds): SelfUserProfilePage {
        UiWaitUtils.waitAnyVisible(
            selectors = listOf(userProfilePageTitle, logoutButton),
            timeout = timeout
        ) ?: throw AssertionError("User Profile Page is not displayed")
        return this
    }

    fun assertCurrentAccountActive(accountName: String): SelfUserProfilePage {
        UiWaitUtils.waitElement(activeAccountName(accountName))
        return this
    }

    fun assertChangeStatusOptionsVisible(): SelfUserProfilePage {
        UiWaitUtils.waitElement(availabilitySelector)
        return this
    }

    fun changeAvailabilityStatus(status: String): SelfUserProfilePage {
        UiWaitUtils.waitElement(availabilitySelector).click()
        UiWaitUtils.waitElement(UiSelectorParams(text = status)).click()
        return this
    }

    fun assertStatusChangeTextVisible(text: String): SelfUserProfilePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = text))
        return this
    }

    fun assertAvailabilityOptionsVisible(currentStatus: String = "None"): SelfUserProfilePage {
        UiWaitUtils.waitElement(availabilityLabel)
        UiWaitUtils.waitElement(availabilityStatus(currentStatus))
        return this
    }

    fun changeAvailabilityStatus(currentStatus: String, newStatus: String): SelfUserProfilePage {
        UiWaitUtils.waitElement(availabilityStatus(currentStatus)).click()
        UiWaitUtils.waitElement(availabilityStatus(newStatus)).click()
        return this
    }

    fun assertStatusChangeInfoText(expectedText: String): SelfUserProfilePage {
        val actualText = UiWaitUtils.waitElement(statusChangeInfoText).text
        if (actualText != expectedText) {
            throw AssertionError(
                "Status information text does not match. Expected '$expectedText', but was '$actualText'."
            )
        }
        return this
    }

    fun confirmStatusChange(): SelfUserProfilePage {
        UiWaitUtils.waitElement(okButton).click()
        return this
    }

    fun assertAvailabilityStatusSelected(status: String): SelfUserProfilePage {
        UiWaitUtils.waitElement(availabilityStatus(status))
        return this
    }

    fun tapCloseProfileButton(): SelfUserProfilePage {
        UiWaitUtils.waitElement(closeProfileButton).click()
        return this
    }

    fun closeUserProfile(): SelfUserProfilePage = tapCloseProfileButton()

    fun tapLogoutButton(): SelfUserProfilePage {
        UiWaitUtils.waitElement(logoutButton).click()
        return this
    }

    fun tapNewAccountButton(): SelfUserProfilePage {
        UiWaitUtils.waitElement(newAccountButton).click()
        return this
    }

    fun assertOtherAccountsVisible(timeout: Duration = 15.seconds): SelfUserProfilePage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = otherAccountsHeader,
            timeout = timeout,
            errorMessage = "Other accounts section is not visible."
        )
        return this
    }

    fun assertOtherLoggedInAccountVisible(displayName: String): SelfUserProfilePage {
        assertOtherAccountsVisible()
        UiWaitUtils.waitElement(UiSelectorParams(text = displayName))
        return this
    }

    fun assertNoOtherAccountsLoggedIn(timeout: Duration = 15.seconds): SelfUserProfilePage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = otherAccountsHeader.toBySelector(),
            timeout = timeout,
            errorMessage = "Other logged-in accounts are still visible."
        )
        return this
    }

    fun tapOtherAccountByName(displayName: String): SelfUserProfilePage {
        runCatching {
            UiScrollable(UiSelector().scrollable(true)).apply {
                setAsVerticalList()
                setMaxSearchSwipes(5)
                scrollIntoView(UiSelector().text(displayName))
            }
        }
        UiWaitUtils.waitElement(UiSelectorParams(text = displayName)).click()
        return this
    }

    fun assertCancelLoginDialogVisible(): SelfUserProfilePage {
        UiWaitUtils.waitElement(cancelLoginDialogTitle)
        return this
    }

    fun confirmCancelLogin(): SelfUserProfilePage {
        UiWaitUtils.waitElement(cancelLoginDialogCancelButton).click()
        return this
    }

    fun assertRemovedDeviceDialogVisible(timeout: Duration = 30.seconds): SelfUserProfilePage {
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = removedDeviceDialogTitle,
            timeout = timeout,
            errorMessage = "Removed Device dialog is not visible."
        )
        return this
    }

    fun confirmRemovedDeviceDialog(): SelfUserProfilePage {
        UiWaitUtils.waitElement(okButton).click()
        return this
    }

    fun iSeeClearDataOnLogOutAlert(): SelfUserProfilePage {
        try {
            UiWaitUtils.waitElement(clearDataAlert)
        } catch (e: AssertionError) {
            throw AssertionError("Clear Data alert is not visible", e)
        }
        return this
    }

    fun iSeeInfoTextCheckbox(message: String): SelfUserProfilePage {
        val messageSelector = UiSelectorParams(text = message)
        try {
            UiWaitUtils.waitElement(messageSelector)
        } catch (e: AssertionError) {
            throw AssertionError("Message '$message' is not visible on the Clear Data alert", e)
        }
        return this
    }

    fun tapInfoTextCheckbox(): SelfUserProfilePage {
        UiWaitUtils.waitElement(infoTextCheckbox).click()
        return this
    }

    fun tapNewTeamOrAddAccountButton(): SelfUserProfilePage {
        UiWaitUtils.waitElement(newTeamOrAddAccountButton).click()
        return this
    }
}
