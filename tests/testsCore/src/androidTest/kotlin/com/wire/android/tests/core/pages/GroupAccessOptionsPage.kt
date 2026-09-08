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
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class GroupAccessOptionsPage(private val device: UiDevice) {
    private val clickableSwitch = UiSelector().className("android.view.View").clickable(true)
    private val disableButton = UiSelectorParams(text = "Disable")
    private val backButton = UiSelectorParams(description = "Go back to conversation details")

    fun assertGuestSwitchState(expectedState: String): GroupAccessOptionsPage {
        return assertSwitchState("Guests", expectedState)
    }

    fun tapGuestSwitch(): GroupAccessOptionsPage {
        return tapSwitch("Guests")
    }

    fun assertAppsSwitchState(expectedState: String): GroupAccessOptionsPage {
        return assertSwitchState("Apps", expectedState)
    }

    fun tapAppsSwitch(): GroupAccessOptionsPage {
        return tapSwitch("Apps")
    }

    fun tapDisableButton(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(disableButton).click()
        return this
    }

    fun tapBackButton(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(backButton).click()
        return this
    }

    private fun assertSwitchState(optionName: String, expectedState: String): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = optionName))
        val expectedStateIsVisible = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.DEFAULT_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_FAST
        ) {
            val option = device.findObject(UiSelector().text(optionName))
            val state = option.getFromParent(UiSelector().text(expectedState))
            state.exists() && !state.visibleBounds.isEmpty
        }
        assertTrue("$optionName switch is not in $expectedState state.", expectedStateIsVisible)
        return this
    }

    private fun tapSwitch(optionName: String): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = optionName))
        val option = device.findObject(UiSelector().text(optionName))
        val switch = option.getFromParent(clickableSwitch)
        assertTrue("$optionName switch is not visible.", switch.exists() && !switch.visibleBounds.isEmpty)
        switch.click()
        return this
    }
}
