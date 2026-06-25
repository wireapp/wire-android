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
import org.junit.Assert
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import uiautomatorutils.UiWaitUtils.toBySelector

data class GuestOptionsPage(private val device: UiDevice) {

    private val guestsTitle = UiSelectorParams(text = "Guests")
    private val guestsSwitch = UiSelectorParams(className = "android.widget.Switch")
    private val createLinkButton = UiSelectorParams(text = "Create Link")
    private val createLinkWithoutPasswordOption = UiSelectorParams(text = "Create link without password")
    private val guestLink = UiSelectorParams(textContains = "/conversation-join")
    private val copyLinkButton = UiSelectorParams(text = "Copy Link")
    private val copiedToast = UiSelectorParams(text = "Link copied to clipboard")
    private val disableGuestAccessDialog = UiSelectorParams(text = "Disable guest access?")
    private val disableButton = UiSelectorParams(text = "Disable")
    private val backButton = UiSelectorParams(description = "Go back to conversation details")

    fun assertGuestsPageVisible(): GuestOptionsPage {
        val title = UiWaitUtils.waitElement(guestsTitle)
        Assert.assertTrue("Guests page is not visible", !title.visibleBounds.isEmpty)
        return this
    }

    fun assertGuestsSwitchStateIs(expectedState: String): GuestOptionsPage {
        UiWaitUtils.waitElement(guestsTitle)
        UiWaitUtils.waitElement(UiSelectorParams(text = expectedState))
        return this
    }

    fun tapCreateLinkButton(): GuestOptionsPage {
        UiWaitUtils.waitElement(createLinkButton).click()
        return this
    }

    fun tapCreateLinkWithoutPassword(): GuestOptionsPage {
        UiWaitUtils.waitElement(createLinkWithoutPasswordOption).click()
        return this
    }

    fun assertGuestLinkCreated(): GuestOptionsPage {
        val link = UiWaitUtils.waitElement(guestLink, timeout = UiWaitUtils.LONG_TIMEOUT)
        Assert.assertTrue("Guest link is not visible", !link.visibleBounds.isEmpty)
        return this
    }

    fun guestLinkText(): String {
        val link = UiWaitUtils.waitElement(guestLink, timeout = UiWaitUtils.LONG_TIMEOUT)
        return link.text
    }

    fun tapCopyLinkButton(): GuestOptionsPage {
        UiWaitUtils.waitElement(copyLinkButton).click()
        UiWaitUtils.findElementOrNull(copiedToast)
        return this
    }

    fun tapGuestsSwitch(): GuestOptionsPage {
        val switch = UiWaitUtils.findElementOrNull(guestsSwitch)
        if (switch != null) {
            switch.click()
        } else {
            val title = UiWaitUtils.waitElement(guestsTitle)
            device.click(device.displayWidth - SWITCH_TRAILING_OFFSET_PX, title.visibleCenter.y)
        }
        return this
    }

    fun tapDisableButtonOnGuestAccessDialog(): GuestOptionsPage {
        UiWaitUtils.waitElement(disableGuestAccessDialog)
        UiWaitUtils.waitElement(disableButton).click()
        return this
    }

    fun assertGuestLinkNotVisible(): GuestOptionsPage {
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = guestLink.toBySelector(),
            timeout = UiWaitUtils.SHORT_TIMEOUT,
            errorMessage = "Guest link is still visible."
        )
        return this
    }

    fun tapBackButton(): GuestOptionsPage {
        UiWaitUtils.waitElement(backButton).click()
        return this
    }

    private companion object {
        const val SWITCH_TRAILING_OFFSET_PX = 64
    }
}
