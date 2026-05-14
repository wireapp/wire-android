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
package com.wire.android.ui.home.conversations.attachment

import android.webkit.MimeTypeMap
import com.wire.android.media.audiomessage.toNormalizedLoudness
import com.wire.android.util.FileManager
import com.wire.android.util.getAudioLengthInMs
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.util.fileExtension
import okio.Path
import okio.Path.Companion.toPath
import java.io.File

interface MessageAttachmentFileGateway {
    fun exists(localFilePath: String): Boolean
    fun open(localFilePath: String, fileName: String, onError: () -> Unit)
    fun audioMetadata(dataPath: Path, mimeType: String, wavesMask: List<Int>?): AssetContent.AssetMetadata.Audio
}

class MessageAttachmentFileGatewayImpl(
    private val fileManager: FileManager,
) : MessageAttachmentFileGateway {

    override fun exists(localFilePath: String): Boolean = File(localFilePath).exists()

    override fun open(localFilePath: String, fileName: String, onError: () -> Unit) {
        val mimeType = MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileName.fileExtension() ?: "")
        fileManager.openWithExternalApp(localFilePath.toPath(), fileName, mimeType, onError)
    }

    override fun audioMetadata(
        dataPath: Path,
        mimeType: String,
        wavesMask: List<Int>?,
    ): AssetContent.AssetMetadata.Audio =
        AssetContent.AssetMetadata.Audio(
            durationMs = getAudioLengthInMs(dataPath, mimeType),
            normalizedLoudness = wavesMask?.toNormalizedLoudness(),
        )
}
