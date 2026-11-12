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
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils
import java.io.File
import kotlin.math.roundToInt

data class PdfViewerPage(private val device: UiDevice) {
    private val firstPage = UiSelectorParams(description = "Page 1")

    fun assertPdfPreviewVisible(fileName: String): PdfViewerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        UiWaitUtils.waitElement(firstPage, timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        return this
    }

    fun capturePdfPageScreenshot(): Bitmap {
        val page = UiWaitUtils.waitElement(firstPage, timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        return captureRenderedDocumentScreenshot(page.visibleBounds)
    }

    fun assertPdfMatchesScreenshot(expected: Bitmap): PdfViewerPage {
        val page = UiWaitUtils.waitElement(firstPage, timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        assertRenderedDocumentMatchesScreenshot(page.visibleBounds, expected)
        return this
    }

    fun savePdfPageScreenshot(pageNumber: Int = 1): File {
        val page = UiWaitUtils.waitElement(
            UiSelectorParams(description = "Page $pageNumber")
        )
        return saveRenderedDocumentScreenshot(page.visibleBounds)
    }

    fun assertPdfMatchesScreenshot(expected: File, pageNumber: Int = 1): PdfViewerPage {
        val page = UiWaitUtils.waitElement(
            UiSelectorParams(description = "Page $pageNumber")
        )
        assertRenderedDocumentMatchesScreenshot(page.visibleBounds, expected)
        return this
    }

    fun assertPdfPageCount(expectedCount: Int): PdfViewerPage {
        UiWaitUtils.waitElement(
            UiSelectorParams(textMatches = "Page \\d+ of $expectedCount")
        )
        return this
    }

    // Generated PDF fixtures are 300 by 400 points; compare the whole page, never a clipped slice.
    @Suppress("MagicNumber")
    fun scrollToPdfPage(pageNumber: Int): PdfViewerPage {
        val visible = UiWaitUtils.retryUntilTimeout(UiWaitUtils.MEDIUM_TIMEOUT) {
            val list = device.findObject(By.scrollable(true)) ?: return@retryUntilTimeout false
            val page = device.findObject(By.desc("Page $pageNumber"))?.visibleBounds
            if (page != null && page.height() >= (page.width() * 4f / 3f).roundToInt()) {
                true
            } else {
                val viewport = list.visibleBounds
                val direction = if (page != null && page.top <= viewport.top) Direction.UP else Direction.DOWN
                val distance = if (page == null) {
                    0.5f
                } else {
                    ((page.width() * 4f / 3f - page.height()) / viewport.height() + 0.05f).coerceIn(0.1f, 0.6f)
                }
                list.scroll(direction, distance)
                false
            }
        }
        assertTrue("PDF page $pageNumber did not become fully visible while scrolling", visible)
        return this
    }

    fun assertPdfHasNoEditOrShareControls(): PdfViewerPage {
        listOf("Edit file", "Share").forEach { action ->
            assertFalse("PDF preview exposes '$action'", device.hasObject(By.descContains(action)))
        }
        return this
    }

    fun assertPdfLoading(fileName: String): PdfViewerPage {
        // The list also shows a spinner during availability checks; wait for the viewer itself.
        UiWaitUtils.waitUntilGoneOrThrow(
            selector = By.text("Shared Drive"),
            timeout = UiWaitUtils.MEDIUM_TIMEOUT,
            errorMessage = "PDF '$fileName' has not opened its viewer"
        )
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(text = fileName),
            errorMessage = "The PDF preview for '$fileName' is not visible"
        )
        UiWaitUtils.waitUntilVisibleOrThrow(
            params = UiSelectorParams(className = "android.widget.ProgressBar"),
            timeout = UiWaitUtils.SHORT_WAIT,
            errorMessage = "The PDF download finished before it could be interrupted; use a controlled slow connection"
        )
        assertNoPdfPageRendered()
        return this
    }

    fun assertPdfDownloadFailedWithRetry(fileName: String): PdfViewerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        UiWaitUtils.waitElement(
            UiSelectorParams(text = "This document could not be downloaded."),
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        )
        val retry = UiWaitUtils.waitElement(UiSelectorParams(text = "Try again"))
        assertTrue("Try again is not enabled after the interrupted PDF download", retry.isEnabled)
        assertNoPdfPageRendered()
        return this
    }

    fun tapTryAgainButton(): PdfViewerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = "Try again")).click()
        return this
    }

    fun assertNoPdfPageRendered(): PdfViewerPage {
        assertFalse("An incomplete PDF is rendered as a complete document", device.hasObject(By.desc("Page 1")))
        return this
    }

    fun assertPdfDownloadDenied(fileName: String): PdfViewerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        UiWaitUtils.waitElement(
            UiSelectorParams(text = "This document could not be downloaded."),
            timeout = UiWaitUtils.VERY_LONG_TIMEOUT
        )
        assertFalse("Removed Guest can see PDF content", device.hasObject(By.desc("Page 1")))
        return this
    }

    fun navigateBack(): PdfViewerPage {
        device.pressBack()
        return this
    }
}
