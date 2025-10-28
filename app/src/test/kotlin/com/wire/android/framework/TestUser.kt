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

package com.wire.android.framework

import com.wire.kalium.logic.data.conversation.Conversation.Member
import com.wire.kalium.logic.data.conversation.MemberDetails
import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAssetId
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo

object TestUser {
    val USER_ID = UserId("value", "domain")
    val SELF_USER_ID = UserId("selfValue", "domain")

    val SELF_USER = SelfUser(
        SELF_USER_ID,
        name = "username",
        handle = "handle",
        email = "email",
        phone = "phone",
        accentId = 0,
        teamId = TeamId("teamId"),
        connectionStatus = ConnectionState.ACCEPTED,
        previewPicture = UserAssetId("value", "domain"),
        completePicture = UserAssetId("value", "domain"),
        availabilityStatus = UserAvailabilityStatus.AVAILABLE,
        supportedProtocols = setOf(SupportedProtocol.PROTEUS),
        userType = UserTypeInfo.Regular(UserType.INTERNAL),
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
        userType = UserTypeInfo.Regular(UserType.INTERNAL),
        botService = null,
        deleted = false,
        defederated = false,
        isProteusVerified = false,
        supportedProtocols = setOf(SupportedProtocol.PROTEUS)
    )
    val MEMBER_SELF = MemberDetails(SELF_USER, Member.Role.Admin)
    val MEMBER_OTHER = MemberDetails(OTHER_USER, Member.Role.Member)
}
