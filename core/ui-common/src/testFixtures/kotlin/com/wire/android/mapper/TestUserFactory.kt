/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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

import com.wire.kalium.logic.data.id.TeamId
import com.wire.kalium.logic.data.user.ConnectionState
import com.wire.kalium.logic.data.user.OtherUser
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserAvailabilityStatus
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.data.user.type.UserType
import com.wire.kalium.logic.data.user.type.UserTypeInfo

fun testSelfUser(i: Int): SelfUser = SelfUser(
    id = UserId(value = "value$i", domain = "domain$i"),
    name = "name$i",
    handle = "handle$i",
    email = "email$i",
    phone = "phone$i",
    accentId = i,
    teamId = TeamId("team$i"),
    connectionStatus = ConnectionState.NOT_CONNECTED,
    previewPicture = null,
    completePicture = null,
    availabilityStatus = UserAvailabilityStatus.NONE,
    supportedProtocols = setOf(SupportedProtocol.PROTEUS),
    userType = UserTypeInfo.Regular(UserType.INTERNAL),
)

fun testOtherUser(i: Int): OtherUser = OtherUser(
    id = UserId(value = "value$i", domain = "domain$i"),
    name = "name$i",
    handle = "handle$i",
    email = "email$i",
    phone = "phone$i",
    accentId = i,
    teamId = TeamId("team$i"),
    connectionStatus = ConnectionState.NOT_CONNECTED,
    previewPicture = null,
    completePicture = null,
    availabilityStatus = UserAvailabilityStatus.NONE,
    userType = UserTypeInfo.Regular(UserType.INTERNAL),
    botService = null,
    deleted = false,
    defederated = false,
    isProteusVerified = false,
    supportedProtocols = setOf(SupportedProtocol.PROTEUS),
    isUnderLegalHold = false,
)
