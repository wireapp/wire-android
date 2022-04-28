package com.wire.android.ui.home.messagecomposer.attachment

import android.content.Context
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.wire.android.appLogger
import com.wire.android.ui.home.conversations.model.AttachmentBundle
import com.wire.android.ui.home.conversations.model.AttachmentType
import com.wire.android.util.DEFAULT_FILE_MIME_TYPE
import com.wire.android.util.getFileName
import com.wire.android.util.getMimeType
import com.wire.android.util.orDefault
import com.wire.android.util.toByteArray
import java.io.IOException


class AttachmentState(val context: Context) {
    var attachmentFileState by mutableStateOf<AttachmentFileState>(AttachmentFileState.NotPicked)

    suspend fun pickAttachment(attachmentUri: Uri) {
        attachmentFileState = try {
            val mimeType = attachmentUri.getMimeType(context).orDefault(DEFAULT_FILE_MIME_TYPE)
            val assetRawData = attachmentUri.toByteArray(context)
            val assetFileName = context.getFileName(attachmentUri)
            val attachmentType = if (mimeType.contains("image/")) AttachmentType.IMAGE else AttachmentType.GENERIC_FILE
            val attachment = AttachmentBundle(mimeType, assetRawData, assetFileName, attachmentType)
            AttachmentFileState.Picked(attachment)
        } catch (e: IOException) {
            appLogger.e("There was an error while obtaining the file from disk", e)
            AttachmentFileState.Error
        }
    }

    fun resetAttachmentState() {
        attachmentFileState = AttachmentFileState.NotPicked
    }
}

sealed class AttachmentFileState {
    object NotPicked : AttachmentFileState()
    class Picked(val attachmentBundle: AttachmentBundle) : AttachmentFileState()
    object Error : AttachmentFileState()
}
