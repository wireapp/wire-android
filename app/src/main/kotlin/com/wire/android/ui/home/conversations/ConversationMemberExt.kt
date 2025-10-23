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

package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.NameBasedAvatar
import com.wire.android.model.UserAvatarData
import com.wire.android.ui.home.conversationslist.model.Membership
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.message.UserSummary
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.isAppOrBot

fun List<User>.findUser(userId: UserId): User? = firstOrNull { it.id == userId }
fun List<MemberDetails>.findUser(userId: UserId): MemberDetails? = firstOrNull { it.user.id == userId }

val MemberDetails.name
    get() = this.user.name

val MemberDetails.handle
    get() = this.user.handle

val MemberDetails.userId
    get() = this.user.id

val MemberDetails.availabilityStatus: UserAvailabilityStatus
    get() = when (this.user) {
        is OtherUser -> (user as OtherUser).availabilityStatus
        is SelfUser -> (user as SelfUser).availabilityStatus
    }

fun User.previewAsset(): UserAvatarAsset? = when (this) {
    is OtherUser -> previewPicture
    is SelfUser -> previewPicture
}?.let { UserAvatarAsset(it) }

fun User.avatar(connectionState: ConnectionState?): UserAvatarData =
    UserAvatarData(
        asset = this.previewAsset(),
        availabilityStatus = availabilityStatus,
        connectionState = connectionState,
        membership = if (userType.isAppOrBot()) Membership.Service else Membership.None,
        nameBasedAvatar = NameBasedAvatar(fullName = name, accentColor = accentId)
    )

fun UserSummary.previewAsset() = UserAvatarData(
    asset = this.userPreviewAssetId?.let { UserAvatarAsset(it) },
    availabilityStatus = this.availabilityStatus,
    connectionState = this.connectionStatus,
    nameBasedAvatar = NameBasedAvatar(fullName = userName, accentColor = accentId)
)
