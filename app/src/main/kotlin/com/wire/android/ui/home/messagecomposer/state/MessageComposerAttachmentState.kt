/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer.state

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.FileManager
import com.wire.android.util.dispatchers.DefaultDispatcherProvider
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.getFileName
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.resampleImageAndCopyToTempPath
import kotlinx.coroutines.withContext
import okio.Path.Companion.toPath
import java.io.IOException
import java.util.UUID

class AttachmentInnerState(val context: Context) {
    var attachmentState by mutableStateOf<AttachmentState>(AttachmentState.NotPicked)

//    suspend fun pickAttachment(
//        attachmentUri: Uri,
//        tempCachePath: okio.Path,
//        dispatcherProvider: DispatcherProvider = DefaultDispatcherProvider()
//    ) = withContext(dispatcherProvider.io()) {
//        val fileManager = FileManager(context)
//        attachmentState = try {
//            val fullTempAssetPath = "$tempCachePath/${UUID.randomUUID()}".toPath()
//            val assetFileName = context.getFileName(attachmentUri) ?: throw IOException("The selected asset has an invalid name")
//            val mimeType = attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE)
//            val attachmentType = AttachmentType.fromMimeTypeString(mimeType)
//            val assetSize = if (attachmentType == AttachmentType.IMAGE) {
//                attachmentUri.resampleImageAndCopyToTempPath(context, fullTempAssetPath)
//            } else {
//                fileManager.copyToTempPath(attachmentUri, fullTempAssetPath)
//            }
//            val attachment = AssetBundle(mimeType, fullTempAssetPath, assetSize, assetFileName, attachmentType)
//            AttachmentState.Picked(attachment)
//        } catch (e: IOException) {
//            appLogger.e("There was an error while obtaining the file from disk", e)
//            AttachmentState.Error
//        }
//    }

    fun resetAttachmentState() {
        attachmentState = AttachmentState.NotPicked
    }
}

sealed class AttachmentState {
    object NotPicked : AttachmentState()
    class Picked(val attachmentBundle: AssetBundle) : AttachmentState()
    object Error : AttachmentState()
}
