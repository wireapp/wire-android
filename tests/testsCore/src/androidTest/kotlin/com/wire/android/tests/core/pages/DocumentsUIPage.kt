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

import androidx.test.uiautomator.UiDevice
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
data class DocumentsUIPage(private val device: UiDevice) {
    private val qrCodeImage = UiSelectorParams(text = "my-test-qr.png")

    private val sendButton = UiSelectorParams(text = "Send")

    fun iSeeQrCodeImage(): DocumentsUIPage {

        try {
            UiWaitUtils.waitElement(qrCodeImage)
        } catch (e: AssertionError) {
            throw AssertionError("qrCodeImage is not visible", e)
        }
        return this
    }

    fun iOpenDisplayedQrCodeImage(): DocumentsUIPage {
        UiWaitUtils.waitElement(qrCodeImage).click()
        return this
    }

    fun iTapSendButtonOnPreviewImage(): DocumentsUIPage {
        UiWaitUtils.waitElement(sendButton).click()
        return this
    }
}
