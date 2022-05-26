package com.wire.android.model

import android.os.Parcelable
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.parseIntoQualifiedID
import com.wire.kalium.logic.data.user.UserAssetId
import kotlinx.parcelize.Parcelize

sealed class ImageAsset {
    data class UserAvatarAsset(val userAssetId: UserAssetId) : ImageAsset()
    data class PrivateAsset(val conversationId: ConversationId, val messageId: String) : ImageAsset() {
        override fun toString(): String = "$conversationId:$messageId"
    }
}

fun String.parseIntoPrivateImageAsset(): ImageAsset.PrivateAsset {
    val (conversationIdString, messageId) = split(":")
    val conversationIdParam = conversationIdString.parseIntoQualifiedID()
    return ImageAsset.PrivateAsset(conversationIdParam, messageId)
}
