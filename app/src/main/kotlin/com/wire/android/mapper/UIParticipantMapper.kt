package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.previewAsset
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.message.reaction.MessageReaction
import com.wire.kalium.logic.data.message.receipt.DetailedReceipt
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.type.UserType
import javax.inject.Inject

class UIParticipantMapper @Inject constructor(
    private val userTypeMapper: UserTypeMapper,
    private val wireSessionImageLoader: WireSessionImageLoader
) {
    fun toUIParticipant(user: User): UIParticipant = with(user) {
        val (userType, connectionState, unavailable) = when (this) {
            is OtherUser -> Triple(this.userType, this.connectionStatus, this.isUnavailableUser)
            // TODO(refactor): does self user need a type ? to false
            is SelfUser -> Triple(UserType.INTERNAL, null, false)
        }
        UIParticipant(
            id = id,
            name = name.orEmpty(),
            handle = handle.orEmpty(),
            avatarData = avatar(wireSessionImageLoader, connectionState),
            isSelf = user is SelfUser,
            membership = userTypeMapper.toMembership(userType),
            connectionState = connectionState,
            unavailable = unavailable,
            isDeleted = (user is OtherUser && user.deleted)
        )
    }

    fun toUIParticipant(messageReaction: MessageReaction): UIParticipant = with(messageReaction) {
        return UIParticipant(
            id = userSummary.userId,
            name = userSummary.userName.orEmpty(),
            handle = userSummary.userHandle.orEmpty(),
            avatarData = previewAsset(wireSessionImageLoader),
            membership = userTypeMapper.toMembership(userSummary.userType),
            unavailable = !userSummary.isUserDeleted && userSummary.userName.orEmpty().isEmpty(),
            isDeleted = userSummary.isUserDeleted,
            isSelf = isSelfUser
        )
    }

    fun toUIParticipant(detailedReceipt: DetailedReceipt): UIParticipant = with(detailedReceipt) {
        return UIParticipant(
            id = userSummary.userId,
            name = userSummary.userName.orEmpty(),
            handle = userSummary.userHandle.orEmpty(),
            avatarData = previewAsset(wireSessionImageLoader),
            membership = userTypeMapper.toMembership(userSummary.userType),
            unavailable = !userSummary.isUserDeleted && userSummary.userName.orEmpty().isEmpty(),
            isDeleted = userSummary.isUserDeleted,
            isSelf = false,
            readReceiptDate = date
        )
    }
}
