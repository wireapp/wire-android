package com.wire.android.ui.home.conversations

import com.wire.android.model.ImageAsset.UserAvatarAsset
import com.wire.android.model.UserAvatarData
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.kalium.logic.data.conversation.MemberDetails
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

fun User.avatar(wireSessionImageLoader: WireSessionImageLoader): UserAvatarData =
    UserAvatarData(asset = this.previewAsset(wireSessionImageLoader), availabilityStatus = availabilityStatus)

val MemberDetails.userType: UserType
    get() = when (this.user) {
        is OtherUser -> (user as OtherUser).userType
        is SelfUser -> UserType.INTERNAL
    }
