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
import androidx.test.uiautomator.UiObject2
import com.google.android.gms.tasks.Tasks
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import org.junit.Assert.assertTrue
import uiautomatorutils.UiWaitUtils
import java.util.concurrent.TimeUnit

// Canvas-rendered document text is not exposed in the accessibility hierarchy.
fun assertRenderedDocumentContainsText(document: UiObject2, expectedText: String) {
    val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
    try {
        val found = UiWaitUtils.retryUntilTimeout(UiWaitUtils.VERY_LONG_TIMEOUT) {
            val screenshot = requireNotNull(InstrumentationRegistry.getInstrumentation().uiAutomation.takeScreenshot())
            val bounds = document.visibleBounds
            val page = Bitmap.createBitmap(screenshot, bounds.left, bounds.top, bounds.width(), bounds.height())
            try {
                val result = Tasks.await(
                    recognizer.process(InputImage.fromBitmap(page, 0)),
                    UiWaitUtils.SHORT_WAIT.inWholeSeconds,
                    TimeUnit.SECONDS
                )
                result.text.replace("\n", " ").contains(expectedText)
            } finally {
                page.recycle()
                screenshot.recycle()
            }
        }
        assertTrue("Rendered document does not contain '$expectedText'", found)
    } finally {
        recognizer.close()
    }
}
