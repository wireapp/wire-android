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
import androidx.test.uiautomator.UiSelector
import androidx.test.uiautomator.Until

data class ConnectedUserProfilePage(private val device: UiDevice) {

    private val startConversationButton = UiSelector().text("Start Conversation")

    fun clickStartConversationButton(): ConnectedUserProfilePage {
        device.findObject(startConversationButton).click()
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
            throw AssertionError("❌ Toast message '$expectedMessage' was not displayed within ${timeoutMillis}ms.")
        }

        return this
    }
}
