package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId

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

val MemberDetails.previewAsset: UserAvatarAsset?
    get() = when (this) {
        is MemberDetails.Other -> this.otherUser.previewPicture
        is MemberDetails.Self -> this.selfUser.previewPicture
    }?.let { UserAvatarAsset(it) }

val MemberDetails.availabilityStatus: UserAvailabilityStatus
    get() = when (this) {
        is MemberDetails.Other -> this.otherUser.availabilityStatus
        is MemberDetails.Self -> this.selfUser.availabilityStatus
    }

val MemberDetails.avatar: UserAvatarData
    get() = UserAvatarData(asset = this.previewAsset, availabilityStatus = this.availabilityStatus)

val MemberDetails.userType: UserType
    get() = when (this) {
        is MemberDetails.Other -> this.userType
        is MemberDetails.Self -> UserType.INTERNAL
    }
