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
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Rect
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.Assert.assertTrue
import uiautomatorutils.UiWaitUtils
import java.io.File
import java.io.IOException
import kotlin.math.abs

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
            renderedPagePixelsMatch(expected, actual)
        } finally {
            actual.recycle()
        }
    }
    assertTrue("Document screenshot does not match the expected content in the same viewer", matches)
}

// Saves the document reference in a temporary cache file and releases the screenshot from memory.
fun saveRenderedDocumentScreenshot(bounds: Rect): File {
    val screenshot = captureRenderedDocumentScreenshot(bounds)
    try {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val file = File.createTempFile("pdf-reference-", ".png", context.cacheDir)
        try {
            file.outputStream().use { screenshot.compress(Bitmap.CompressFormat.PNG, 100, it) }
        } catch (error: IOException) {
            file.delete()
            throw error
        }
        return file
    } finally {
        screenshot.recycle()
    }
}

// Loads the saved reference and compares it with the displayed document area in the same viewer.
fun assertRenderedDocumentMatchesScreenshot(bounds: Rect, expected: File) {
    val screenshot = requireNotNull(BitmapFactory.decodeFile(expected.absolutePath))
    try {
        assertRenderedDocumentMatchesScreenshot(bounds, screenshot)
    } finally {
        screenshot.recycle()
    }
}

// Android's PDF renderer can change a colour channel by one when the same page is drawn after scrolling.
private fun renderedPagePixelsMatch(expected: Bitmap, actual: Bitmap): Boolean {
    if (expected.width != actual.width || expected.height != actual.height) return false
    val expectedPixels = IntArray(expected.width * expected.height)
    val actualPixels = IntArray(actual.width * actual.height)
    expected.getPixels(expectedPixels, 0, expected.width, 0, 0, expected.width, expected.height)
    actual.getPixels(actualPixels, 0, actual.width, 0, 0, actual.width, actual.height)
    return expectedPixels.indices.all { index ->
        val first = expectedPixels[index]
        val second = actualPixels[index]
        abs(Color.red(first) - Color.red(second)) <= 1 &&
            abs(Color.green(first) - Color.green(second)) <= 1 &&
            abs(Color.blue(first) - Color.blue(second)) <= 1 &&
            Color.alpha(first) == Color.alpha(second)
    }
}

private fun captureDocumentArea(bounds: Rect): Bitmap {
    val source = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
    val screenshot = requireNotNull(source.copy(Bitmap.Config.ARGB_8888, false))
    source.recycle()
    val cropped = Bitmap.createBitmap(screenshot, bounds.left, bounds.top, bounds.width(), bounds.height())
    if (cropped !== screenshot) screenshot.recycle()
    return cropped
}
