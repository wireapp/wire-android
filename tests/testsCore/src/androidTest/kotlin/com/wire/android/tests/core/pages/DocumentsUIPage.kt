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
import androidx.test.uiautomator.StaleObjectException
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class DocumentsUIPage(private val device: UiDevice) {
    private val sendButton = UiSelectorParams(text = "Send")
    private val downloadsOption = UiSelectorParams(textContains = "Download")
    private val showRootsButton = UiSelectorParams(description = "Show roots")

    fun iSeeQrCodeImage(fileName: String = "my-test-qr.png"): DocumentsUIPage {
        val qrCodeImage = UiSelectorParams(text = fileName)

        // Picker may open in Recent folder; switch to Downloads so the generated QR file is visible.
        if (UiWaitUtils.findElementOrNull(qrCodeImage) == null) {
            if (!clickWithRetry(downloadsOption)) {
                clickWithRetry(showRootsButton)
                clickWithRetry(downloadsOption)
            }
        }

        try {
            UiWaitUtils.waitElement(qrCodeImage)
        } catch (e: AssertionError) {
            throw AssertionError("QR code '$fileName' is not visible", e)
        }
        return this
    }

    private fun clickWithRetry(selector: UiSelectorParams, attempts: Int = 3): Boolean {
        repeat(attempts) {
            try {
                UiWaitUtils.waitElement(selector, timeoutMillis = 1500).click()
                return true
            } catch (_: StaleObjectException) {

            } catch (_: AssertionError) {
            }
        }
        return false
    }

    fun iOpenDisplayedQrCodeImage(fileName: String = "my-test-qr.png"): DocumentsUIPage {
        val qrCodeImage = UiSelectorParams(text = fileName)
        UiWaitUtils.waitElement(qrCodeImage).click()
        return this
    }

    fun iTapSendButtonOnPreviewImage(): DocumentsUIPage {
        UiWaitUtils.waitElement(sendButton).click()
        return this
    }
}
