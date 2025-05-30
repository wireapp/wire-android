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
import uiautomatorutils.UiAutomatorUtils

data class SettingsPage(private val device: UiDevice) {

//    fun assertSendAnonymousUsageDataToggleIsOn(): SettingsPage {
//        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//
//        // Step 1: Locate the "Send anonymous usage data" label
//        val label = device.findObject(UiSelector().text("Send anonymous usage data"))
//
//        // Step 2: Go up to the parent container
//        val parent = label.getFromParent(UiSelector().className("android.view.View"))
//
//        // Step 3: Find the "ON" text inside that container
//        val toggleText = parent.getChild(UiSelector().text("ON"))
//
//        // Step 4: Assert
//        check(toggleText.exists()) {
//            "Expected 'Send anonymous usage data' toggle to be ON, but 'ON' was not found."
//        }
//
//        return this
//    }

    fun assertSendAnonymousUsageDataToggleIsOn(): SettingsPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        // Locate the label
        val label = device.findObject(UiSelector().text("Send anonymous usage data"))

        // Locate the ON text (sibling or nested)
        val toggle = device.findObject(
            UiSelector().text("ON")
                .childSelector(UiSelector().className("android.view.View"))
        )

        // Optional: add an assertion if needed
        check(toggle.exists()) {
            "Expected toggle to be ON but 'ON' text was not found."
        }

        return this
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
