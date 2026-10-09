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

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiSelector
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class GroupAccessOptionsPage(private val device: UiDevice) {
    private val clickableSwitch = UiSelector().className("android.view.View").clickable(true)
    private val disableButton = UiSelectorParams(text = "Disable")
    private val backButton = UiSelectorParams(description = "Go back to conversation details")
    private val createLinkButton = UiSelectorParams(text = "Create Link")
    private val createLinkWithPassword = UiSelectorParams(text = "Create password secured link")
    private val createLinkWithoutPassword = UiSelectorParams(text = "Create link without password")
    private val createPasswordSecuredLinkHeading = UiSelectorParams(text = "Create Password Secured Link")
    private val guestLink = UiSelectorParams(textContains = "/conversation-join")
    private val copyLinkButton = UiSelectorParams(text = "Copy Link")
    private val passwordSecuredLinkBanner = UiSelectorParams(text = "Link is password secured")

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

    fun tapCreateLinkButton(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(createLinkButton).click()
        return this
    }

    fun tapCreateLinkWithPassword(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(createLinkWithPassword).click()
        return this
    }

    fun tapCreateLinkWithoutPassword(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(createLinkWithoutPassword).click()
        return this
    }

    fun assertGuestLinkCreated(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(guestLink)
        return this
    }

    fun assertCreatePasswordSecuredLinkPageVisible(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(createPasswordSecuredLinkHeading)
        return this
    }

    fun enterGuestLinkPassword(password: String?): GroupAccessOptionsPage {
        enterPassword(password, 0)
        return this
    }

    fun enterGuestLinkConfirmPassword(password: String?): GroupAccessOptionsPage {
        enterPassword(password, 1)
        return this
    }

    fun assertGuestLinkIsPasswordSecured(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(passwordSecuredLinkBanner)
        return this
    }

    fun tapCopyGuestLinkButton(): GroupAccessOptionsPage {
        UiWaitUtils.waitElement(copyLinkButton).click()
        return this
    }

    fun assertGuestLinkNotVisible(): GroupAccessOptionsPage {
        val linkIsGone = UiWaitUtils.retryUntilTimeout(
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            pollingInterval = UiWaitUtils.POLLING_FAST
        ) {
            UiWaitUtils.findElementOrNull(guestLink) == null
        }
        assertTrue("Guest link is visible after guest access was disabled.", linkIsGone)
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

    private fun enterPassword(password: String?, fieldIndex: Int) {
        val fieldsAreVisible = UiWaitUtils.retryUntilTimeout(UiWaitUtils.DEFAULT_TIMEOUT) {
            device.findObjects(By.clazz("android.widget.EditText")).size > fieldIndex
        }
        assertTrue("Guest-link password field is not visible.", fieldsAreVisible)
        device.findObjects(By.clazz("android.widget.EditText"))[fieldIndex].text = password
    }
}
