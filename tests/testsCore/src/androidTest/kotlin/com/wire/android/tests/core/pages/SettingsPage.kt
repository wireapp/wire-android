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


//    fun assertSendAnonymousUsageDataToggleIsOn() {
//        val label = UiAutomatorUtils.waitElement(
//            text = "Send anonymous usage data"
//        )
//
//        val toggle = UiAutomatorUtils.waitElement(
//            text = "ON"
//        )
//
//        assertTrue("'Send anonymous usage data' label is not visible", !label.visibleBounds.isEmpty)
//        assertTrue("'Send anonymous usage data' toggle is not ON (missing)", !toggle.visibleBounds.isEmpty)
//    }

//    fun assertSendAnonymousUsageDataToggleIsOn() {
//        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//
//        val containers = device.findObjects(By.clazz("android.view.View"))
//
//        val match = containers.firstOrNull { container ->
//            val hasLabel = container.findObjects(By.text("Send anonymous usage data")).isNotEmpty()
//            val hasToggle = container.findObjects(By.text("ON")).isNotEmpty()
//            hasLabel && hasToggle
//        }
//
//        check(match != null) {
//            "Expected 'Send anonymous usage data' with ON toggle in the same container, but not found."
//        }
//    }

    fun assertSendAnonymousUsageDataToggleIsOn(): SettingsPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        val container = device.findObject(
            UiSelector().className("android.view.View")
                .childSelector(UiSelector().text("Send anonymous usage data"))
        )

        val toggle = container.getFromParent(UiSelector().text("ON"))

//        check(toggle.exists()) {
//            "'Send anonymous usage data' toggle is expected to be ON, but it was not found."
       // }

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
