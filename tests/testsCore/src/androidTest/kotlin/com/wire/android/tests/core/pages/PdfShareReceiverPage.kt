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

import android.net.Uri
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.UiDevice
import com.wire.android.tests.core.fixtures.PDF_SHARE_RECEIPT
import com.wire.android.tests.core.fixtures.sha256
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class PdfShareReceiverPage(private val device: UiDevice) {
    fun selectFromShareSheet() {
        // Sharing can load the PDF before Android displays its share targets.
        UiWaitUtils.waitElement(
            UiSelectorParams(resourceId = "android:id/chooser_header"),
            timeout = UiWaitUtils.LONG_TIMEOUT
        )
        val receiver = By.text("PDF Test Receiver")
        val visible = UiWaitUtils.retryUntilTimeout(UiWaitUtils.SHORT_TIMEOUT) {
            if (!device.hasObject(receiver)) {
                // Samsung's app row scrolls horizontally; More opens the remaining share targets.
                val samsungApps = device.findObject(By.res("com.android.intentresolver:id/sem_chooser_recycler_ranked_app"))
                if (samsungApps != null) {
                    val more = samsungApps.findObject(By.text("More"))
                    if (more != null) more.click() else samsungApps.scroll(Direction.RIGHT, 1f)
                } else {
                    device.findObject(By.scrollable(true))?.scroll(Direction.DOWN, 1f)
                }
            }
            device.hasObject(receiver)
        }
        assertTrue("PDF Test Receiver is missing from Android's share sheet", visible)
        device.findObject(receiver).click()
    }

    // Compares the receiver's filename and content fingerprint with the original PDF.
    fun assertReceivedPdf(fileName: String, source: Uri) {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val expectedDigest = requireNotNull(context.contentResolver.openInputStream(source)).use { it.sha256() }
        val receipt = UiWaitUtils.waitElement(UiSelectorParams(description = PDF_SHARE_RECEIPT))
        assertEquals("The PDF receiver did not open", context.packageName, device.currentPackageName)
        assertEquals("The receiving app did not read the original PDF", "$fileName\n$expectedDigest", receipt.text)
    }
}
