/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.userprofile.qr

import android.content.Context
import com.wire.android.appLogger
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getTempWritableAttachmentUri
import com.wire.kalium.logic.data.asset.KaliumFileSystem
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import java.io.FileOutputStream
import javax.inject.Inject

class AndroidSelfQRCodeAssetRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val kaliumFileSystem: KaliumFileSystem,
    private val dispatchers: DispatcherProvider
) : SelfQRCodeAssetRepository {

    override suspend fun saveQRCode(qrCodeImage: SelfQRCodeImage): String = withContext(dispatchers.io()) {
        val tempImagePath = "${kaliumFileSystem.rootCachePath}/$TEMP_SELF_QR_FILENAME".toPath()
        val qrImageUri = getTempWritableAttachmentUri(context, tempImagePath)
        context.contentResolver.openFileDescriptor(qrImageUri, "rwt")?.use { fileDescriptor ->
            FileOutputStream(fileDescriptor.fileDescriptor).use { fileOutputStream ->
                qrCodeImage.writeTo(fileOutputStream)
                fileOutputStream.flush()
            }
        }
        appLogger.withTextTag("SelfQRCodeAssetRepository").d("Image written to: $qrImageUri")
        qrImageUri.toString()
    }

    companion object {
        private const val TEMP_SELF_QR_FILENAME = "temp_self_qr.jpg"
    }
}
