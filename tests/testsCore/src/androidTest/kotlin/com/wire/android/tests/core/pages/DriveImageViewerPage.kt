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

import android.graphics.Bitmap
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiDevice
import androidx.test.uiautomator.UiObject2
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import util.decodeQRCode

/** Verifies the selected image's identity and rendered pixels in Wire's image viewer. */
data class DriveImageViewerPage(private val device: UiDevice) {
    fun assertQrImageVisible(fileName: String, expectedValue: String): DriveImageViewerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        val image = UiWaitUtils.waitElement(UiSelectorParams(description = fileName))
        assertQrContent(image, expectedValue)
        return this
    }

    fun assertConversationImageVisible(conversationName: String, expectedValue: String): DriveImageViewerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = conversationName))
        val image = UiWaitUtils.waitElement(UiSelectorParams(description = "Image message"))
        assertQrContent(image, expectedValue)
        return this
    }

    private fun assertQrContent(image: UiObject2, expectedValue: String) {
        val rendered = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_LONG_TIMEOUT) {
            val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            val bounds = image.visibleBounds
            val content = Bitmap.createBitmap(screenshot, bounds.left, bounds.top, bounds.width(), bounds.height())
            try {
                decodeQRCode(content) == expectedValue
            } finally {
                content.recycle()
                screenshot.recycle()
            }
        }
        assertTrue("The selected image does not render QR content '$expectedValue'", rendered)
    }

    fun navigateBack(): DriveImageViewerPage {
        device.pressBack()
        return this
    }
}
