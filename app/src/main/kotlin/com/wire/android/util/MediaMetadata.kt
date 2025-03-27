/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.util

import android.graphics.BitmapFactory
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import com.wire.kalium.logic.data.message.AssetContent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okio.Path
import java.io.File

object MediaMetadata {
    suspend fun getMediaMetadata(
        filePath: Path,
        mimeType: String
    ): AssetContent.AssetMetadata? = withContext(Dispatchers.IO) {
        when {
            isImageFile(mimeType) -> {
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeFile(filePath.toFile().absolutePath, options)
                AssetContent.AssetMetadata.Image(options.outWidth, options.outHeight)
            }

            isVideoFile(mimeType) -> getVideoMetaData(filePath.toFile().absolutePath)
            isAudioFile(mimeType) ->
                AssetContent.AssetMetadata.Audio(getAudioLengthInMs(filePath, mimeType), null)
            isPdfFile(mimeType) -> getPdfMetaData(filePath.toFile())
            else -> null
        }
    }
}

private fun getPdfMetaData(pdfFile: File): AssetContent.AssetMetadata.Image =
    ParcelFileDescriptor.open(pdfFile, ParcelFileDescriptor.MODE_READ_ONLY).use { fd ->
        PdfRenderer(fd).use { renderer ->
            renderer.openPage(0).use { page ->
                AssetContent.AssetMetadata.Image(page.width, page.height)
            }
        }
    }
