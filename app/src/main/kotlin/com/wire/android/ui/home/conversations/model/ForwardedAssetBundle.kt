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
package com.wire.android.ui.home.conversations.model

import android.os.Parcelable
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.MessageEncryptionAlgorithm
import kotlinx.parcelize.Parcelize

@Parcelize
data class ForwardedAssetBundle(
    val assetId: String,
    val assetToken: String?,
    val assetDomain: String?,
    val otrKey: ByteArray,
    val sha256: ByteArray,
    val encryptionAlgorithm: String?,
    val fileName: String,
    val mimeType: String,
    val dataSize: Long,
    val assetType: AttachmentType,
    val imageWidth: Int? = null,
    val imageHeight: Int? = null,
    val videoWidth: Int? = null,
    val videoHeight: Int? = null,
    val durationMs: Long? = null,
    val audioNormalizedLoudness: ByteArray? = null,
    val localDataPath: String? = null,
) : Parcelable {
    fun toAssetContent(): AssetContent =
        AssetContent(
            sizeInBytes = dataSize,
            name = fileName,
            mimeType = mimeType,
            metadata = when (assetType) {
                AttachmentType.IMAGE -> {
                    if (imageWidth != null && imageHeight != null) {
                        AssetContent.AssetMetadata.Image(imageWidth, imageHeight)
                    } else {
                        null
                    }
                }

                AttachmentType.VIDEO -> AssetContent.AssetMetadata.Video(videoWidth, videoHeight, durationMs)
                AttachmentType.AUDIO -> AssetContent.AssetMetadata.Audio(durationMs, audioNormalizedLoudness)
                AttachmentType.GENERIC_FILE -> null
            },
            remoteData = AssetContent.RemoteData(
                otrKey = otrKey,
                sha256 = sha256,
                assetId = assetId,
                assetToken = assetToken,
                assetDomain = assetDomain,
                encryptionAlgorithm = encryptionAlgorithm?.let(MessageEncryptionAlgorithm::valueOf),
            ),
        )
}
