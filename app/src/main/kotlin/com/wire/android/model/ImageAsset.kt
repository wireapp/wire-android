package com.wire.android.model

import androidx.compose.runtime.Composable
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.data.user.UserAssetId

sealed class ImageAsset(private val imageLoader: WireSessionImageLoader) {

    data class UserAvatarAsset(
        private val imageLoader: WireSessionImageLoader,
        val userAssetId: UserAssetId
    ) : ImageAsset(imageLoader)

    data class PrivateAsset(
        private val imageLoader: WireSessionImageLoader,
        val conversationId: ConversationId,
        val messageId: String,
        val isSelfAsset: Boolean
    ) : ImageAsset(imageLoader) {
        override fun toString(): String = "$conversationId:$messageId:$isSelfAsset"
    }

    @Composable
    fun paint(fallbackData: Any? = null) = imageLoader.paint(asset = this, fallbackData)
}

fun String.parseIntoPrivateImageAsset(imageLoader: WireSessionImageLoader): ImageAsset.PrivateAsset {
    val (conversationIdString, messageId, isSelfAsset) = split(":")
    val conversationIdParam = conversationIdString.parseIntoQualifiedID()
    return ImageAsset.PrivateAsset(imageLoader, conversationIdParam, messageId, isSelfAsset.toBoolean())
}
