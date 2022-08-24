package com.wire.android.framework

import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType

object TestUser {
    val USER_ID = UserId("value", "domain")
    val SELF_USER = SelfUser(
        USER_ID,
        name = "username",
        handle = "handle",
        email = "email",
        phone = "phone",
        accentId = 0,
        teamId = TeamId("teamId"),
        connectionStatus = ConnectionState.ACCEPTED,
        previewPicture = UserAssetId("value", "domain"),
        completePicture = UserAssetId("value", "domain"),
        availabilityStatus = UserAvailabilityStatus.AVAILABLE
    )
    val OTHER_USER = OtherUser(
        USER_ID.copy(value = "otherValue"),
        name = "otherUsername",
        handle = "otherHandle",
        email = "otherEmail",
        phone = "otherPhone",
        accentId = 0,
        teamId = TeamId("otherTeamId"),
        connectionStatus = ConnectionState.ACCEPTED,
        previewPicture = UserAssetId("value", "domain"),
        completePicture = UserAssetId("value", "domain"),
        availabilityStatus = UserAvailabilityStatus.AVAILABLE,
        userType = UserType.INTERNAL,
        botService = null,
        deleted = false
    )
    val MEMBER_SELF = MemberDetails(SELF_USER, Member.Role.Admin)
    val MEMBER_OTHER = MemberDetails(OTHER_USER, Member.Role.Member)
}
