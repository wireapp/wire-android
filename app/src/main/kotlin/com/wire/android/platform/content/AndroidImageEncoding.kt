/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.platform.content

import android.graphics.Bitmap
import com.wire.content.media.EncodedImage
import java.io.ByteArrayOutputStream

fun Bitmap.encodeJpeg(quality: Int): EncodedImage {
    val output = ByteArrayOutputStream()
    check(compress(Bitmap.CompressFormat.JPEG, quality, output)) { "Bitmap encoding failed" }
    return EncodedImage(output.toByteArray(), JPEG_MIME_TYPE)
}

private const val JPEG_MIME_TYPE = "image/jpeg"
