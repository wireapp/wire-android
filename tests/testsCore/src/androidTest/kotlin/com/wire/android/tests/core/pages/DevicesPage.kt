/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
import androidx.test.uiautomator.UiSelector
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.toBySelector
import kotlin.test.DefaultAsserter.assertTrue

data class DevicesPage(private val device: UiDevice) {
    private val devicesPageTitle = UiSelectorParams(text = "Your Devices")
    private val currentDeviceSection = UiSelectorParams(text = "CURRENT DEVICE")
    private val otherDevicesSection = UiSelectorParams(text = "OTHER DEVICES")
    private val deviceItem = UiSelectorParams(resourceId = "device_item")
    private val deviceIdAndAddedDate = UiSelectorParams(textMatches = "(?s)(?=.*Proteus ID:)(?=.*Added:).*")
    private val verifyDeviceToggle = UiSelector()
        .className("android.view.View")
        .clickable(true)
        .checkable(true)
    private val deviceNotVerified = UiSelectorParams(text = "Not Verified")
    private val deviceVerified = UiSelectorParams(text = "Verified")
    private val backButton = UiSelectorParams(description = "Go back")

    fun assertManageDevicesPageVisible(): DevicesPage {
        UiWaitUtils.waitElement(devicesPageTitle)
        return this
    }

    fun assertCurrentDeviceVisible(): DevicesPage {
        val currentDevice = UiWaitUtils.waitElement(currentDeviceSection)
            .parent
            ?.findObject(deviceItem.toBySelector())
        assertTrue("Current device is not visible", currentDevice?.visibleBounds?.isEmpty == false)
        return this
    }

    fun assertCurrentDeviceIdAndAddedDateVisible(): DevicesPage {
        val currentDeviceDetails = UiWaitUtils.waitElement(currentDeviceSection)
            .parent
            ?.findObject(deviceIdAndAddedDate.toBySelector())
        assertTrue(
            "Current device ID and added date are not visible",
            currentDeviceDetails?.visibleBounds?.isEmpty == false
        )
        return this
    }

    fun assertOtherDeviceVisible(deviceName: String): DevicesPage {
        UiWaitUtils.waitElement(otherDevicesSection)
        val deviceItem = UiWaitUtils.waitAnyVisible(
            selectors = listOf(
                UiSelectorParams(text = deviceName),
                UiSelectorParams(description = "$deviceName, Not Verified")
            ),
            timeout = UiWaitUtils.SHORT_TIMEOUT
        )
        assertTrue("Other device '$deviceName' is not visible", deviceItem != null)
        return this
    }

    fun assertOtherDeviceNotVisible(deviceName: String): DevicesPage {
        listOf(
            UiSelectorParams(text = deviceName),
            UiSelectorParams(description = "$deviceName, Not Verified")
        ).forEach { selector ->
            UiWaitUtils.waitUntilGoneOrThrow(
                selector = selector.toBySelector(),
                timeout = UiWaitUtils.SHORT_TIMEOUT,
                errorMessage = "Other device '$deviceName' is still visible"
            )
        }
        return this
    }

    fun tapDevice(deviceName: String): DevicesPage {
        val deviceItem = UiWaitUtils.waitAnyVisible(
            selectors = listOf(
                UiSelectorParams(description = "$deviceName, Not Verified"),
                UiSelectorParams(text = deviceName)
            ),
            timeout = UiWaitUtils.SHORT_TIMEOUT
        ) ?: throw AssertionError("Device '$deviceName' is not visible")
        deviceItem.click()
        return this
    }

    fun assertDeviceNotVerified(): DevicesPage {
        UiWaitUtils.waitElement(deviceNotVerified)
        return this
    }

    fun tapVerifyDeviceButton(): DevicesPage {
        UiWaitUtils.waitElement(deviceNotVerified)
        device.findObject(verifyDeviceToggle).click()
        return this
    }

    fun assertDeviceVerified(): DevicesPage {
        UiWaitUtils.waitElement(deviceVerified)
        return this
    }

    fun closeDevicesScreen(): DevicesPage {
        UiWaitUtils.waitElement(backButton).click()
        return this
    }
}
