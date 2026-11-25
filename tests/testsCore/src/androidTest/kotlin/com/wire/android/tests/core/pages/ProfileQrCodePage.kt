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

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import com.google.zxing.ReaderException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import util.decodeQRCode

class ProfileQrCodePage(private val device: UiDevice) {
    fun assertProfileQrCodeAndReadLink(userId: String, username: String): String {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Share Profile"))
        assertProfileUsername(username)
        val link = UiWaitUtils.waitElement(UiSelectorParams(textContains = "/user-profile/?id=")).text
        val qualifiedId = requireNotNull(Uri.parse(link).getQueryParameter("id"))
        assertEquals("Profile link belongs to another user", userId, qualifiedId.substringBefore('@'))
        val expectedQr = "wire://user/${qualifiedId.substringAfter('@')}/$userId"
        assertTrue(
            "Displayed QR code does not encode this user's profile",
            UiWaitUtils.retryUntilTimeout(UiWaitUtils.DEFAULT_TIMEOUT) {
                val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
                try {
                    decodeQRCode(screenshot) == expectedQr
                } catch (_: ReaderException) {
                    false
                } finally {
                    screenshot.recycle()
                }
            }
        )
        return link
    }

    fun assertProfileUsername(username: String): ProfileQrCodePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "@${username.removePrefix("@")}"))
        return this
    }

    fun tapShareProfileLinkButton(): ProfileQrCodePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Share Profile Link")).click()
        return this
    }

    fun tapShareQrCodeButton(): ProfileQrCodePage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Share QR Code")).click()
        return this
    }

    fun tapBackButton(): ProfileQrCodePage {
        device.pressBack()
        return this
    }
}
