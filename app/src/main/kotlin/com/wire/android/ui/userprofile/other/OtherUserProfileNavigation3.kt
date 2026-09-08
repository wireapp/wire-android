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

package com.wire.android.ui.userprofile.other

import com.wire.android.ui.userprofile.UserProfileQualifiedId
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class OtherUserProfileViewModelArgs(
    val targetUserId: UserProfileQualifiedId,
    val groupConversationId: UserProfileQualifiedId? = null,
)

@Serializable
data class OtherUserProfileRoute(
    override val sessionId: WireSessionId,
    val targetUserId: UserProfileQualifiedId,
    val groupConversationId: UserProfileQualifiedId? = null,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/other_user_profile_screen"
    }
}

internal fun OtherUserProfileRoute.toViewModelArgs() = OtherUserProfileViewModelArgs(
    targetUserId = targetUserId,
    groupConversationId = groupConversationId,
)

@Serializable
data class ConnectionRequestIgnoredResult(
    @SerialName("userId")
    val userName: String,
) {
    init {
        require(userName.isNotBlank()) { "An ignored connection request user name cannot be blank" }
    }
}

internal val ConnectionRequestIgnoredResultContract =
    WireNavResultContract<ConnectionRequestIgnoredResult>(
        WireNavResultContractId("user-profile.connection-request-ignored")
    )
