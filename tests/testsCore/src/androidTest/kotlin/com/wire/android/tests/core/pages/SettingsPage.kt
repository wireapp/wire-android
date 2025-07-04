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
import junit.framework.TestCase.assertFalse
import junit.framework.TestCase.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class SettingsPage(private val device: UiDevice) {

    fun assertSendAnonymousUsageDataToggleIsOn(): SettingsPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())

        val container = device.findObject(
            UiSelector().className("android.view.View")
                .childSelector(UiSelector().text("Send anonymous usage data"))
        )

        val toggle = container.getFromParent(UiSelector().text("ON"))

        assertTrue("'Send anonymous usage data' label is not visible", !toggle.visibleBounds.isEmpty)

        return this
    }
    fun clickBackButtonOnPrivacySettingsPage() {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        device.pressBack()
    }
    fun clickPrivacySettingsButtonOnSettingsPage(): SettingsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Privacy Settings")).click()
        return this
    }
    fun clickDebugSettingsButton(): SettingsPage {
        // val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        UiWaitUtils.waitElement(UiSelectorParams(text = "Debug Settings")).click()
        // device.findObject(UiSelector().text("Debug Settings")).click()
        return this
    }
    fun assertAnalyticsInitializedIsSetToTrue(): SettingsPage {
        // Step 1: Wait for the "Analytics Initialized" label
        val label = UiWaitUtils.waitElement(UiSelectorParams(text = "Analytics Initialized"))
        // Step 2: Get its parent and find sibling with text "true"
        val parent = label.parent
        val value = parent?.children?.find { it.text == "true" }
        assertTrue("'Analytics Initialized' is not set to true", value != null && value.visibleBounds.width() > 0)
        return this
    }

    fun assertAnalyticsTrackingIdentifierIsDispayed(): SettingsPage {
        val device = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
        // Step 1: Find the container with the label text
        val container = device.findObject(
            UiSelector().className("android.view.View")
                .childSelector(UiSelector().text("Analytics Tracking Identifier"))
        )
        // Step 2: Use fromParent to locate the sibling TextView that holds the identifier
        val identifierView = container.getFromParent(
            UiSelector().className("android.widget.TextView")
                .instance(1) // Typically the second TextView under the same parent
        )
        // Step 3: Assert it's visible and not empty
        val value = identifierView.text
        assertTrue("Analytics tracking ID is missing or blank", value.isNotBlank())

        return this
    }

    fun tapEnableLoggingToggle() {
        val label = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .findObject(UiSelector().text("Enable Logging"))
        val toggle = label.getFromParent(UiSelector().className("android.view.View"))
        toggle.click()
    }

//    fun assertEnableLoggingToggleIsOff() {
//        val toggle = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//            .findObject(
//                UiSelector().text("Enable Logging")
//                    .fromParent(UiSelector().className("android.view.View").clickable(true))
//            )
//
//        assertFalse("Enable Logging toggle should be OFF", toggle.isChecked)
//    }

//    fun assertToggleCheckedState(expectedChecked: Boolean) {
//        val toggle = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//            .findObject(
//                UiSelector()
//                    .className("android.view.View")
//                    .clickable(true)
//                    .checkable(true)
//                    .checked(expectedChecked)
//            )
//
//        assertTrue("Toggle should be ${if (expectedChecked) "ON" else "OFF"}", toggle.exists())
//    }


//    fun assertToggleIsOn() {
//        val toggle = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
//            .findObject(
//                UiSelector()
//                    .className("android.view.View")
//                    .clickable(true)
//                    .checkable(true)
//                    .checked(true)
//            )
//
//        assertTrue("Toggle should be ON", toggle.exists())
//    }



    fun assertToggleIsOff() {
        val toggle = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .findObject(
                UiSelector()
                    .className("android.view.View")
                    .clickable(true)
                    .checked(false)
            )

        assertFalse("Toggle should be OFF", toggle.isChecked)
    }

    fun assertToggleIsOn() {
        val toggle = UiDevice.getInstance(InstrumentationRegistry.getInstrumentation())
            .findObject(
                UiSelector()
                    .className("android.view.View")
                    .clickable(true)
                    .checked(true)
            )

        assertTrue("Toggle should be OFF", toggle.isChecked)

    }



}
