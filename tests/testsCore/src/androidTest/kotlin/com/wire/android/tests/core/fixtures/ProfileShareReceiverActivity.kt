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
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.widget.TextView
import util.decodeQRCode

internal const val PROFILE_SHARE_RECEIPT = "Profile share receipt"

// Receives Wire's actual share intent in the separate UI-test APK and reads its text or image.
class ProfileShareReceiverActivity : Activity() {
    @Suppress("DEPRECATION")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val receipt = runCatching {
            require(intent.action == Intent.ACTION_SEND)
            if (intent.type == "text/plain") {
                "Profile link\n${requireNotNull(intent.getStringExtra(Intent.EXTRA_TEXT))}"
            } else {
                require(intent.type?.startsWith("image/") == true)
                val uri = requireNotNull(
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
                    } else {
                        intent.getParcelableExtra<Uri>(Intent.EXTRA_STREAM)
                    }
                )
                val bitmap = requireNotNull(contentResolver.openInputStream(uri)).use {
                    requireNotNull(BitmapFactory.decodeStream(it))
                }
                try {
                    "Profile QR code\n${requireNotNull(decodeQRCode(bitmap))}"
                } finally {
                    bitmap.recycle()
                }
            }
        }
        setContentView(
            TextView(this).apply {
                contentDescription = PROFILE_SHARE_RECEIPT
                text = receipt.getOrElse { "Could not read shared profile: ${it.message}" }
            }
        )
    }
}
