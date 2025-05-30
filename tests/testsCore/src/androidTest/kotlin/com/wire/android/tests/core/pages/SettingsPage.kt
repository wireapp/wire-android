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
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertTrue
import uiautomatorutils.UiAutomatorUtils

data class SettingsPage(private val device: UiDevice) {


    fun assertSendAnonymousUsageDataToggleIsOn() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Step 1: Find the label with exact text
        val label = device.findObject(UiSelector().text("Send anonymous usage data"))
        // Step 2: From that label, find the sibling with text "ON"
        val toggleState = label.getFromParent(UiSelector().text("ON"))
        // Step 3: Assert that the sibling exists (toggle is ON)
        check(toggleState.exists()) { "Expected 'Send anonymous usage data' toggle to be ON, but it was not found." }
    }

    fun clickBackButtonOnPrivacySettingsPage(): SettingsPage {
        UiAutomatorUtils.waitElement(text = "Go back").click()
        return this
    }

    fun clickPrivacySettingsButtonOnSettingsPage(): SettingsPage {
        UiAutomatorUtils.waitElement(text = "Privacy Settings").click()
        return this
    }

}
