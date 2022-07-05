package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType

fun List<MemberDetails>.findUser(userId: UserId): MemberDetails? = firstOrNull { member ->
    when (member) {
        is MemberDetails.Other -> member.otherUser.id == userId
        is MemberDetails.Self -> member.selfUser.id == userId
    }
}

val MemberDetails.name
    get() = when (this) {
        is MemberDetails.Other -> this.otherUser.name
        is MemberDetails.Self -> this.selfUser.name
    }

val MemberDetails.handle
    get() = when (this) {
        is MemberDetails.Other -> this.otherUser.handle
        is MemberDetails.Self -> this.selfUser.handle
    }

val MemberDetails.userId
    get() = when (this) {
        is MemberDetails.Other -> this.otherUser.id
        is MemberDetails.Self -> this.selfUser.id
    }

val MemberDetails.availabilityStatus: UserAvailabilityStatus
    get() = when (this) {
        is MemberDetails.Other -> this.otherUser.availabilityStatus
        is MemberDetails.Self -> this.selfUser.availabilityStatus
    }

fun MemberDetails.previewAsset(wireSessionImageLoader: WireSessionImageLoader): UserAvatarAsset? = when (this) {
    is MemberDetails.Other -> this.otherUser.previewPicture
    is MemberDetails.Self -> this.selfUser.previewPicture
}?.let { UserAvatarAsset(wireSessionImageLoader, it) }

fun MemberDetails.avatar(wireSessionImageLoader: WireSessionImageLoader): UserAvatarData =
    UserAvatarData(asset = this.previewAsset(wireSessionImageLoader), availabilityStatus = this.availabilityStatus)

val MemberDetails.userType: UserType
    get() = when (this) {
        is MemberDetails.Other -> this.otherUser.userType
        is MemberDetails.Self -> UserType.INTERNAL
    }
