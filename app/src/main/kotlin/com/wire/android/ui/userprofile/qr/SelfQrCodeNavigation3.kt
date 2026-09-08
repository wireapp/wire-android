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

package com.wire.android.ui.userprofile.qr

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
data class SelfQrCodeViewModelArgs(
    val userHandle: String = "",
    val isTeamMember: Boolean,
)

@Serializable
data class SelfQrCodeRoute(
    override val sessionId: WireSessionId,
    val userHandle: String = "",
    val isTeamMember: Boolean,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        // Compose Destinations preserves the acronym as q_r; keep it stable for analytics.
        const val ROUTE_ID = "app/self_q_r_code_screen"
    }
}

internal fun SelfQrCodeRoute.toViewModelArgs() = SelfQrCodeViewModelArgs(
    userHandle = userHandle,
    isTeamMember = isTeamMember,
)
