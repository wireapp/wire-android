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
package com.wire.android.ui.sharing

import com.wire.android.ui.home.conversations.model.ForwardedAssetBundle
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.feature.message.GetMessageByIdUseCase
import kotlinx.coroutines.withContext
import javax.inject.Inject

class StageForwardedAssetShareUseCase @Inject constructor(
    private val getMessageById: GetMessageByIdUseCase,
    private val importSessionStore: ImportSessionStore,
    private val dispatchers: DispatcherProvider,
) {
    suspend operator fun invoke(conversationId: QualifiedID, messageId: String): String? = withContext(dispatchers.io()) {
        val assetContent = (getMessageById(conversationId, messageId) as? GetMessageByIdUseCase.Result.Success)
            ?.message
            ?.content
            ?.let { it as? MessageContent.Asset }
            ?.value
            ?: return@withContext null

        importSessionStore.store(
            importedText = null,
            importedAssets = emptyList(),
            forwardedAssets = listOf(assetContent.toForwardedAssetBundle()),
        )
    }
}

private fun AssetContent.toForwardedAssetBundle(): ForwardedAssetBundle {
    val imageMetadata = metadata as? AssetContent.AssetMetadata.Image
    val videoMetadata = metadata as? AssetContent.AssetMetadata.Video
    val audioMetadata = metadata as? AssetContent.AssetMetadata.Audio

    return ForwardedAssetBundle(
        assetId = remoteData.assetId,
        assetToken = remoteData.assetToken,
        assetDomain = remoteData.assetDomain,
        otrKey = remoteData.otrKey,
        sha256 = remoteData.sha256,
        encryptionAlgorithm = remoteData.encryptionAlgorithm?.name,
        fileName = name.orEmpty().ifBlank { "Wire File" },
        mimeType = mimeType,
        dataSize = sizeInBytes,
        assetType = AttachmentType.fromMimeTypeString(mimeType),
        imageWidth = imageMetadata?.width,
        imageHeight = imageMetadata?.height,
        videoWidth = videoMetadata?.width,
        videoHeight = videoMetadata?.height,
        durationMs = audioMetadata?.durationMs ?: videoMetadata?.durationMs,
        audioNormalizedLoudness = audioMetadata?.normalizedLoudness,
        localDataPath = localData?.assetDataPath,
    )
}
