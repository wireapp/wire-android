package com.wire.android.mapper.message.content

import com.wire.android.mapper.message.content.asset.AssetContentMapper
import com.wire.android.mapper.message.content.asset.RestrictedAssetMapper
import com.wire.android.mapper.message.content.text.MessageTextContentMapper
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.User
import javax.inject.Inject

class RegularMessageContentMapper @Inject constructor(
    private val messageTextContentMapper: MessageTextContentMapper,
    private val assetContentMapper: AssetContentMapper,
    private val restrictedAssetMapper: RestrictedAssetMapper
) {
    fun mapRegularMessage(
        message: Message.Regular,
        sender: User?
    ): UIMessageContent =
        when (val content = message.content) {
            is MessageContent.Asset ->
                assetContentMapper.toRegularAsset(
                    message = message,
                    assetContent = content.value,
                    sender = sender
                )

            is MessageContent.RestrictedAsset ->
                restrictedAssetMapper.toRestrictedAsset(
                    mimeType = content.mimeType,
                    assetSize = content.sizeInBytes,
                    assetName = content.name
                )

            else -> {
                messageTextContentMapper.toTextMessage(
                    conversationId = message.conversationId,
                    content = content
                )
            }
        }

}
