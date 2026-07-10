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
import kotlin.time.Duration.Companion.milliseconds

data class DocumentsUIPage(private val device: UiDevice) {
    private val sendButton = UiSelectorParams(text = "Send")
    private val downloadsOption = UiSelectorParams(textContains = "Download")
    private val showRootsButton = UiSelectorParams(description = "Show roots")

    fun iSeeFile(fileName: String): DocumentsUIPage {
        val file = UiSelectorParams(text = fileName)

        // Picker may open in Recent folder; switch to Downloads so the generated test file is visible.
        if (UiWaitUtils.findElementOrNull(file) == null) {
            if (!clickWithRetry(downloadsOption)) {
                clickWithRetry(showRootsButton)
                clickWithRetry(downloadsOption)
            }
        }

        try {
            UiWaitUtils.waitElement(file)
        } catch (e: AssertionError) {
            throw AssertionError("File '$fileName' is not visible", e)
        }
        return this
    }

    fun iSeeQrCodeImage(fileName: String = "my-test-qr.png"): DocumentsUIPage = iSeeFile(fileName)

    private fun clickWithRetry(selector: UiSelectorParams, attempts: Int = 3): Boolean {
        repeat(attempts) {
            try {
                UiWaitUtils.waitElement(selector, timeout = 1500.milliseconds).click()
                return true
            } catch (_: StaleObjectException) {
                // Retry with a fresh node.
            } catch (_: AssertionError) {
                // Selector not present in current picker pane.
            }
        }
        return false
    }

    fun iOpenDisplayedFile(fileName: String): DocumentsUIPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName)).click()
        return this
    }

    fun iOpenDisplayedQrCodeImage(fileName: String = "my-test-qr.png"): DocumentsUIPage = iOpenDisplayedFile(fileName)

    fun iTapSendButtonOnPreviewImage(): DocumentsUIPage {
        UiWaitUtils.waitElement(sendButton).click()
        return this
    }
}
