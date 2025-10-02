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

data class SelfUserProfilePage(private val device: UiDevice) {

    private val userProfilePageTitle = UiSelectorParams(text = "User Profile")
    private val logoutButton = UiSelectorParams(text = "Log out")
    private val clearDataAlert = UiSelectorParams(text = "Clear Data?")

    private val infoTextCheckbox = UiSelectorParams(className = "android.widget.CheckBox")

    fun iSeeUserProfilePage(): SelfUserProfilePage {
        try {
            UiWaitUtils.waitElement(userProfilePageTitle)
        } catch (e: AssertionError) {
            throw AssertionError("User Profile Page is not displayed", e)
        }
        return this
    }

    fun tapLogoutButton(): SelfUserProfilePage {
        UiWaitUtils.waitElement(logoutButton).click()
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
}
