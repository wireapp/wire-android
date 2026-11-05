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
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.UiObject2
import org.junit.Assert.assertTrue
import uiautomatorutils.UiWaitUtils

// Crop to the document area and wait for visible content before using it as a reference.
fun captureRenderedDocumentScreenshot(document: UiObject2): Bitmap {
    var image: Bitmap? = null
    val loaded = UiWaitUtils.retryUntilTimeout(UiWaitUtils.SHORT_TIMEOUT) {
        image?.recycle()
        val source = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
        val screenshot = requireNotNull(source.copy(Bitmap.Config.ARGB_8888, false))
        source.recycle()
        val bounds = document.visibleBounds
        val cropped = Bitmap.createBitmap(screenshot, bounds.left, bounds.top, bounds.width(), bounds.height())
        if (cropped !== screenshot) screenshot.recycle()
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

fun assertRenderedDocumentMatchesScreenshot(document: UiObject2, expected: Bitmap) {
    val actual = captureRenderedDocumentScreenshot(document)
    try {
        assertTrue("Document screenshot does not match the expected content in the same viewer", expected.sameAs(actual))
    } finally {
        actual.recycle()
    }
}
