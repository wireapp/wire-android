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

import android.graphics.Rect
import android.net.Uri
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import util.profileQrLink
import util.qrCodeInScreenArea

data class ProfileQrCodePage(private val device: UiDevice) {
    private val shareProfileTitle = UiSelectorParams(text = "Share Profile")
    private val profileLinkText = UiSelectorParams(textContains = "/user-profile/?id=")
    private val shareProfileLinkButton = UiSelectorParams(resourceId = "Share link")
    private val shareQrCodeButton = UiSelectorParams(resourceId = "Share QR link")

    private fun profileUsername(username: String) = UiSelectorParams(text = "@${username.removePrefix("@")}")

    fun assertProfileQrCodeAndReadLink(userId: String, username: String): String {
        val heading = UiWaitUtils.waitElement(shareProfileTitle)
        val handle = UiWaitUtils.waitElement(profileUsername(username))
        val link = UiWaitUtils.waitElement(profileLinkText).text
        val qualifiedId = requireNotNull(Uri.parse(link).getQueryParameter("id"))
        assertEquals("Profile link belongs to another user", userId, qualifiedId.substringBefore('@'))
        val expectedQr = profileQrLink(link)
        assertTrue(
            "Displayed QR code does not encode this user's profile",
            UiWaitUtils.retryUntilTimeout(UiWaitUtils.DEFAULT_TIMEOUT) {
                // The QR is unlabelled and rendered between the heading and the username.
                val qrArea = Rect(0, heading.visibleBounds.bottom, device.displayWidth, handle.visibleBounds.top)
                qrCodeInScreenArea(qrArea) == expectedQr
            }
        )
        return link
    }

    fun assertProfileUsername(username: String): ProfileQrCodePage {
        UiWaitUtils.waitElement(profileUsername(username))
        return this
    }

    fun tapShareProfileLinkButton(): ProfileQrCodePage {
        UiWaitUtils.waitElement(shareProfileLinkButton).click()
        return this
    }

    fun tapShareQrCodeButton(): ProfileQrCodePage {
        UiWaitUtils.waitElement(shareQrCodeButton).click()
        return this
    }

    fun tapBackButton(): ProfileQrCodePage {
        device.pressBack()
        return this
    }
}
