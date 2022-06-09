package com.wire.android.framework

import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.conversation.UserType
import com.wire.kalium.logic.data.publicuser.model.OtherUser
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId

object TestUser {
    val USER_ID = UserId("value", "domain")
    val SELF_USER = SelfUser(
        USER_ID,
        name = "username",
        handle = "handle",
        email = "email",
        phone = "phone",
        accentId = 0,
        team = "teamId",
        connectionStatus = ConnectionState.ACCEPTED,
        previewPicture = UserAssetId(),
        completePicture = UserAssetId(),
        availabilityStatus = UserAvailabilityStatus.AVAILABLE
    )
    val OTHER_USER = OtherUser(
        USER_ID.copy(value = "otherValue"),
        name = "otherUsername",
        handle = "otherHandle",
        email = "otherEmail",
        phone = "otherPhone",
        accentId = 0,
        team = "otherTeamId",
        connectionStatus = ConnectionState.ACCEPTED,
        previewPicture = UserAssetId(),
        completePicture = UserAssetId(),
        availabilityStatus = UserAvailabilityStatus.AVAILABLE
    )
    val MEMBER_SELF = MemberDetails.Self(SELF_USER)
    val MEMBER_OTHER = MemberDetails.Other(OTHER_USER, UserType.INTERNAL)
}
