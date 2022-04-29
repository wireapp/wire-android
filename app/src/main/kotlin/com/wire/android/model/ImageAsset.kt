package com.wire.android.model

import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserAssetId

sealed class ImageAsset {
    data class UserAvatarAsset(val userAssetId: UserAssetId) : ImageAsset()
    data class PrivateAsset(val conversationId: ConversationId, val messageId: String) : ImageAsset()
}
