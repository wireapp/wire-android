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

package com.wire.android.ui.e2eiEnrollment

import com.wire.navigation.AuthenticationScreenRoute
import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import com.wire.navigation.authenticationSessionFlowId
import kotlinx.serialization.Serializable

@Serializable
data class E2EIEnrollmentRoute(
    override val sessionId: WireSessionId,
    override val flowId: String = sessionId.authenticationSessionFlowId(),
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute, AuthenticationScreenRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/e2_e_i_enrollment_screen"
    }
}
