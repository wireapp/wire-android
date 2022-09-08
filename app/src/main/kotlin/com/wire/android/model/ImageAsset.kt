package com.wire.android.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserAssetId

@Stable
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

fun String.parseIntoPrivateImageAsset(
    imageLoader: WireSessionImageLoader,
    qualifiedIdMapper: QualifiedIdMapper,
): ImageAsset.PrivateAsset {
    val (conversationIdString, messageId, isSelfAsset) = split(":")
    val conversationIdParam = qualifiedIdMapper.fromStringToQualifiedID(conversationIdString)
    return ImageAsset.PrivateAsset(imageLoader, conversationIdParam, messageId, isSelfAsset.toBoolean())
}
