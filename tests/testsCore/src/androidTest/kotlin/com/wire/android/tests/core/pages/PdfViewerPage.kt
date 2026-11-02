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

import androidx.test.uiautomator.By
import androidx.test.uiautomator.UiDevice
import org.junit.Assert.assertFalse
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class PdfViewerPage(private val device: UiDevice) {
    private val firstPage = UiSelectorParams(description = "Page 1")

    fun assertPdfPreviewVisible(fileName: String): PdfViewerPage {
        UiWaitUtils.waitElement(UiSelectorParams(text = fileName))
        UiWaitUtils.waitElement(firstPage, timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        return this
    }

    fun assertRenderedPdfContainsText(expectedText: String): PdfViewerPage {
        val page = UiWaitUtils.waitElement(firstPage, timeout = UiWaitUtils.VERY_LONG_TIMEOUT)
        assertRenderedDocumentContainsText(page, expectedText)
        return this
    }

    fun assertViewerPdfHasNoEditOrShareControls(): PdfViewerPage {
        listOf("Edit file", "Share").forEach { action ->
            assertFalse("Viewer PDF preview exposes '$action'", device.hasObject(By.descContains(action)))
        }
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
