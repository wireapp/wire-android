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
package com.wire.android.tests.core.fixtures

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.OpenableColumns
import android.widget.TextView
import java.io.InputStream
import java.security.MessageDigest

internal const val PDF_SHARE_RECEIPT = "PDF share receipt"

/**
 * A receiving screen packaged in the UI-test APK.
 * The test manifest registers it as "PDF Test Receiver" in Android's share sheet.
 * When selected, it reads the PDF shared by Wire and displays its filename and SHA-256 checksum
 * (a fingerprint of the file's contents). PdfShareReceiverPage compares these with the original file
 * to verify that another app can read the shared PDF without its name or contents changing.
 * This screen checks the received file bytes; it does not render the PDF pages.
 */
class PdfShareReceiverActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receipt = runCatching {
            val uri = requireNotNull(
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                } else {
                    intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                }
            )
            val fileName = contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
            val digest = requireNotNull(contentResolver.openInputStream(uri)).use { it.sha256() }
            "${requireNotNull(fileName)}\n$digest"
        }
        setContentView(
            TextView(this).apply {
                contentDescription = PDF_SHARE_RECEIPT
                text = receipt.getOrElse { "Could not read shared PDF: ${it.message}" }
            }
        )
    }
}

// Reads the file in chunks and calculates its content fingerprint without loading the whole file into memory.
internal fun InputStream.sha256(): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
    var bytesRead = read(buffer)
    while (bytesRead != -1) {
        digest.update(buffer, 0, bytesRead)
        bytesRead = read(buffer)
    }
    return digest.digest().joinToString("") { "%02x".format(it) }
}
