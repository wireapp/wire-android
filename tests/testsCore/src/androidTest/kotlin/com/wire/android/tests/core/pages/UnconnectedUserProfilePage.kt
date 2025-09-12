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
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class UnconnectedUserProfilePage(private val device: UiDevice) {

    private val acceptButton = UiSelectorParams(text = "Accept")

    private val ignoreButton = UiSelectorParams(text = "Ignore")

    private val connectionRequestButton = UiSelectorParams(text = "Connect")

    private val connectionNotificationText = UiSelectorParams(
        textContains = "This user wants to connect with you."
    )

    private val closeButtonOnUnconnectedUserProfilePage = UiSelectorParams(description = "Close")
    fun assertAcceptButtonIsDisplayed(): UnconnectedUserProfilePage {
        val acceptButtonElement = UiWaitUtils.waitElement(acceptButton)
        Assert.assertTrue("Accept button is not visible", !acceptButtonElement.visibleBounds.isEmpty)
        return this
    }

    fun assertIgnoreButtonIsDisplayed(): UnconnectedUserProfilePage {
        val ignoreButtonElement = UiWaitUtils.waitElement(ignoreButton)
        Assert.assertTrue("Ignore button is not visible", !ignoreButtonElement.visibleBounds.isEmpty)
        return this
    }

    fun clickAcceptButton(): UnconnectedUserProfilePage {
        UiWaitUtils.waitElement(acceptButton).click()
        return this
    }

    fun assertConnectionRequestNotificationTextIsDisplayed(): UnconnectedUserProfilePage {
        val connectionRequestNotificationText = UiWaitUtils.waitElement(connectionNotificationText)
        Assert.assertTrue(
            "'This user wants to connect with you' text is not visible.",
            !connectionRequestNotificationText.visibleBounds.isEmpty
        )
        return this
    }

    fun assertUserNameInUnconnectedUserProfilePage(userName: String): UnconnectedUserProfilePage {
        try {
            UiWaitUtils.waitElement(UiSelectorParams(text = userName))
        } catch (e: AssertionError) {
            throw AssertionError(" The user '$userName' is not visible in unconnected user profile page", e)
        }
        return this
    }

    fun clickCloseButtonOnUnconnectedUserProfilePage(): UnconnectedUserProfilePage {
        UiWaitUtils.waitElement(closeButtonOnUnconnectedUserProfilePage).click()
        return this
    }

    fun clickConnectionRequestButton(): UnconnectedUserProfilePage {
        UiWaitUtils.waitElement(connectionRequestButton).click()
        return this
    }
}
