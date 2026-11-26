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
import androidx.test.uiautomator.UiDevice
import com.wire.android.tests.core.fixtures.PDF_SHARE_RECEIPT
import com.wire.android.tests.core.fixtures.sha256
import org.junit.Assert.assertEquals
import uiautomatorutils.UiSelectorParams
import uiautomatorutils.UiWaitUtils

data class PdfShareReceiverPage(private val device: UiDevice) {
    fun selectFromShareSheet() {
        selectShareTarget(device, "PDF Test Receiver")
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
