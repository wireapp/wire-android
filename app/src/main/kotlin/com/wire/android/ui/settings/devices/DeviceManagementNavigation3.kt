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

package com.wire.android.ui.settings.devices

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
data class SelfDevicesRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/self_devices_screen"
    }
}

/**
 * The target user is deliberately separate from [DeviceDetailsRoute.sessionId].
 *
 * Device details can be opened for another user's client. The session identity selects the Metro
 * graph, while this value selects the user whose client is displayed.
 */
@Serializable
data class DeviceTargetUserId(
    val value: String,
    val domain: String,
) {
    init {
        require(value.isNotBlank()) { "A device target user id value cannot be blank" }
        require(domain.isNotBlank()) { "A device target user id domain cannot be blank" }
    }
}

@Serializable
data class DeviceDetailsRoute(
    override val sessionId: WireSessionId,
    val targetUserId: DeviceTargetUserId,
    val clientId: String,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    init {
        require(clientId.isNotBlank()) { "A device details client id cannot be blank" }
    }

    companion object {
        const val ROUTE_ID = "app/device_details_screen"
    }
}
