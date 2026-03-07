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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.Until
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import kotlin.test.DefaultAsserter.assertTrue

data class ConnectedUserProfilePage(private val device: UiDevice) {
    private val startConversationButton = UiSelectorParams(text = "Start Conversation")

    private val showMoreOptions = UiSelectorParams(description = "Open conversation options")

    private val blockOption = UiSelectorParams(text = "Block")

    private val blockedLabel = UiSelectorParams(text = "Blocked")

    private val unblockUserButton = UiSelectorParams(text = "Unblock User")
    private val blockButtonAlert = UiSelectorParams(text = "Block")

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
        timeoutMillis: Long = 5_000
    ): ConnectedUserProfilePage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        val selector = By.text(expectedMessage)
        val toast = device.wait(Until.findObject(selector), timeoutMillis)

        if (toast == null || toast.visibleBounds.isEmpty) {
            throw AssertionError("Toast message '$expectedMessage' was not displayed within ${timeoutMillis}ms.")
        }

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
}
