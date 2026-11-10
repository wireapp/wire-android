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
import android.graphics.Color
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import uiautomatorutils.UiWaitUtils

// Crop to the document area and wait for visible content before using it as a reference.
fun captureRenderedDocumentScreenshot(bounds: Rect): Bitmap {
    var image: Bitmap? = null
    val loaded = UiWaitUtils.retryUntilTimeout(UiWaitUtils.SHORT_TIMEOUT) {
        image?.recycle()
        val cropped = captureDocumentArea(bounds)
        image = cropped
        val pixels = IntArray(cropped.width * cropped.height)
        cropped.getPixels(pixels, 0, cropped.width, 0, 0, cropped.width, cropped.height)
        pixels.any { Color.red(it) < 128 && Color.green(it) < 128 && Color.blue(it) < 128 }
    }
    if (!loaded) {
        image?.recycle()
        throw AssertionError("Document content is blank")
    }
    return requireNotNull(image)
}

fun assertRenderedDocumentMatchesScreenshot(bounds: Rect, expected: Bitmap) {
    // The canvas can appear before the page finishes drawing.
    val matches = UiWaitUtils.retryUntilTimeout(UiWaitUtils.STABLE_TIMEOUT) {
        val actual = captureDocumentArea(bounds)
        try {
            expected.sameAs(actual)
        } finally {
            actual.recycle()
        }
    }
    assertTrue("Document screenshot does not match the expected content in the same viewer", matches)
}

private fun captureDocumentArea(bounds: Rect): Bitmap {
    val source = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    val screenshot = requireNotNull(source.copy(Bitmap.Config.ARGB_8888, false))
    source.recycle()
    val cropped = Bitmap.createBitmap(screenshot, bounds.left, bounds.top, bounds.width(), bounds.height())
    if (cropped !== screenshot) screenshot.recycle()
    return cropped
}
