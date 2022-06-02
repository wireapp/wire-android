package com.wire.android.mapper

import com.wire.android.ui.home.conversations.model.MessageContent
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.message.AssetContent
import com.wire.kalium.logic.feature.asset.GetMessageAssetUseCase
import com.wire.kalium.logic.feature.asset.MessageAssetResult
import javax.inject.Inject

class AssetContentMapper @Inject constructor(
    private val getMessageAsset: GetMessageAssetUseCase
) {

    suspend fun fromMessage(
        conversationId: ConversationId,
        messageId: String,
        assetContent: AssetContent
    ): MessageContent? {
        return with(assetContent) {
            val (imgWidth, imgHeight) = when (val md = metadata) {
                is AssetContent.AssetMetadata.Image -> md.width to md.height
                else -> 0 to 0
            }

            if (remoteData.assetId.isNotEmpty()) {
                when {
                    // If it's an image, we download it right away
                    mimeType.contains("image") -> MessageContent.ImageMessage(
                        assetId = remoteData.assetId,
                        rawImgData = getRawAssetData(conversationId, messageId),
                        width = imgWidth,
                        height = imgHeight
                    )

                    // It's a generic Asset Message so let's not download it yet
                    else -> {
                        MessageContent.AssetMessage(
                            assetName = name ?: "",
                            assetExtension = name?.split(".")?.last() ?: "",
                            assetId = remoteData.assetId,
                            assetSizeInBytes = sizeInBytes,
                            downloadStatus = downloadStatus
                        )
                        // On the first asset message received, the asset ID is null, so we filter it out until the second updates it
                    }
                }
            } else null
        }
    }

    private suspend fun getRawAssetData(conversationId: QualifiedID, messageId: String): ByteArray? {
        getMessageAsset(
            conversationId = conversationId,
            messageId = messageId
        ).run {
            return when (this) {
                is MessageAssetResult.Success -> decodedAsset
                else -> null
            }
        }
    }

}
