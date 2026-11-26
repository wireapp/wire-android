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

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.wire.android.tests.core.fixtures.PROFILE_SHARE_RECEIPT
import org.junit.Assert.assertEquals
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import util.profileQrLink

data class ProfileShareReceiverPage(private val device: UiDevice) {
    private val profileReceipt = UiSelectorParams(description = PROFILE_SHARE_RECEIPT)
    private val shareProfileTitle = UiSelectorParams(text = "Share Profile")

    fun selectFromShareSheet(): ProfileShareReceiverPage {
        selectShareTarget(device, "Profile Test Receiver")
        return this
    }

    fun assertReceivedProfileLink(expectedLink: String): ProfileShareReceiverPage {
        assertReceivedContent("Profile link\n$expectedLink")
        return this
    }

    fun assertReceivedProfileQrCode(profileLink: String): ProfileShareReceiverPage {
        val expectedQr = profileQrLink(profileLink)
        assertReceivedContent("Profile QR code\n$expectedQr")
        return this
    }

    private fun assertReceivedContent(expected: String) {
        val receipt = UiWaitUtils.waitElement(profileReceipt)
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        assertEquals("The profile receiver did not open", context.packageName, device.currentPackageName)
        assertEquals("The receiving app did not read the expected profile content", expected, receipt.text)
    }

    fun returnToShareProfile(): ProfileShareReceiverPage {
        device.pressBack()
        UiWaitUtils.waitElement(shareProfileTitle)
        return this
    }
}
