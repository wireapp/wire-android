/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android.mapper

import com.wire.android.ui.home.conversations.avatar
import com.wire.android.ui.home.conversations.details.participants.model.UIParticipant
import com.wire.android.ui.home.conversations.previewAsset
import com.wire.kalium.logic.data.message.UserSummary
import com.wire.kalium.logic.data.message.reaction.MessageReaction
import com.wire.kalium.logic.data.message.receipt.DetailedReceipt
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo
import com.wire.kalium.logic.data.user.type.isAppOrBot
import javax.inject.Inject

class UIParticipantMapper @Inject constructor(
    private val userTypeMapper: UserTypeMapper,
) {
    fun toUIParticipant(user: User, isMLSVerified: Boolean = false): UIParticipant = with(user) {
        val (userType, connectionState, unavailable) = when (this) {
            is OtherUser -> Triple(this.userType, this.connectionStatus, this.isUnavailableUser)
            // TODO(refactor): does self user need a type ? to false
            is SelfUser -> Triple(UserTypeInfo.Regular(UserType.INTERNAL), null, false)
        }
        UIParticipant(
            id = id,
            name = name.orEmpty(),
            handle = handle.orEmpty(),
            avatarData = avatar(connectionState),
            isSelf = user is SelfUser,
            isService = userType.isAppOrBot(),
            membership = userTypeMapper.toMembership(userType),
            connectionState = connectionState,
            unavailable = unavailable,
            isDeleted = (user is OtherUser && user.deleted),
            botService = (user as? OtherUser)?.botService,
            isDefederated = (user is OtherUser && user.defederated),
            isProteusVerified = (user is OtherUser && user.isProteusVerified),
            isMLSVerified = isMLSVerified,
            supportedProtocolList = supportedProtocols.orEmpty().toList(),
            isUnderLegalHold = isUnderLegalHold,
            expiresAt = user.expiresAt
        )
    }

    fun toUIParticipant(messageReaction: MessageReaction): UIParticipant = with(messageReaction) {
        return UIParticipant(
            id = userSummary.userId,
            name = userSummary.userName.orEmpty(),
            handle = userSummary.userHandle.orEmpty(),
            avatarData = userSummary.previewAsset(),
            membership = userTypeMapper.toMembership(userSummary.userType),
            unavailable = !userSummary.isUserDeleted && userSummary.userName.orEmpty().isEmpty(),
            isDeleted = userSummary.isUserDeleted,
            isSelf = isSelfUser,
            isDefederated = false,
            isProteusVerified = false,
            supportedProtocolList = listOf()
        )
    }

    fun toUIParticipant(detailedReceipt: DetailedReceipt): UIParticipant = with(detailedReceipt) {
        return UIParticipant(
            id = userSummary.userId,
            name = userSummary.userName.orEmpty(),
            handle = userSummary.userHandle.orEmpty(),
            avatarData = userSummary.previewAsset(),
            membership = userTypeMapper.toMembership(userSummary.userType),
            unavailable = !userSummary.isUserDeleted && userSummary.userName.orEmpty().isEmpty(),
            isDeleted = userSummary.isUserDeleted,
            isSelf = false,
            readReceiptDate = date,
            isDefederated = false,
            isProteusVerified = false,
            supportedProtocolList = listOf()
        )
    }

    fun toUIParticipant(userSummary: UserSummary): UIParticipant = with(userSummary) {
        return UIParticipant(
            id = userSummary.userId,
            name = userSummary.userName.orEmpty(),
            handle = userSummary.userHandle.orEmpty(),
            avatarData = previewAsset(),
            membership = userTypeMapper.toMembership(userSummary.userType),
            unavailable = !userSummary.isUserDeleted && userSummary.userName.orEmpty().isEmpty(),
            isDeleted = userSummary.isUserDeleted,
            isSelf = false,
            isDefederated = false
        )
    }
}
