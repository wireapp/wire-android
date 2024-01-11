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
import com.wire.android.model.UserAvatarData
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.message.UserSummary
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType

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

fun User.previewAsset(wireSessionImageLoader: WireSessionImageLoader): UserAvatarAsset? = when (this) {
    is OtherUser -> previewPicture
    is SelfUser -> previewPicture
}?.let { UserAvatarAsset(wireSessionImageLoader, it) }

fun User.avatar(wireSessionImageLoader: WireSessionImageLoader, connectionState: ConnectionState?): UserAvatarData =
    UserAvatarData(
        asset = this.previewAsset(wireSessionImageLoader),
        availabilityStatus = availabilityStatus,
        connectionState = connectionState
    )

val MemberDetails.userType: UserType
    get() = when (this.user) {
        is OtherUser -> (user as OtherUser).userType
        is SelfUser -> UserType.INTERNAL
    }

fun UserSummary.previewAsset(
    wireSessionImageLoader: WireSessionImageLoader
) = UserAvatarData(
    asset = this.userPreviewAssetId?.let { UserAvatarAsset(wireSessionImageLoader, it) },
    availabilityStatus = this.availabilityStatus,
    connectionState = this.connectionStatus
)
