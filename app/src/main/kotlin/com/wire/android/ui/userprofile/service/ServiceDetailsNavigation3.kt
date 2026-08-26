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

package com.wire.android.ui.userprofile.service

import com.wire.android.ui.userprofile.UserProfileQualifiedId
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
sealed interface ServiceProfileTarget {
    val id: UserProfileQualifiedId

    @Serializable
    data class Bot(
        override val id: UserProfileQualifiedId,
    ) : ServiceProfileTarget

    @Serializable
    data class App(
        override val id: UserProfileQualifiedId,
    ) : ServiceProfileTarget
}

@Serializable
data class ServiceDetailsViewModelArgs(
    val conversationId: UserProfileQualifiedId?,
    val target: ServiceProfileTarget,
)

@Serializable
data class ServiceDetailsRoute(
    override val sessionId: WireSessionId,
    val conversationId: UserProfileQualifiedId?,
    val target: ServiceProfileTarget,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/service_details_screen"
    }
}

internal fun ServiceDetailsRoute.toViewModelArgs() = ServiceDetailsViewModelArgs(
    conversationId = conversationId,
    target = target,
)
