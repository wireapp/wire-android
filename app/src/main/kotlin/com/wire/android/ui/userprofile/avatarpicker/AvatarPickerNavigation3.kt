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

package com.wire.android.ui.userprofile.avatarpicker

import com.wire.navigation.SessionRoute
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireNavResultContract
import com.wire.navigation.WireNavResultContractId
import com.wire.navigation.WireSessionId
import kotlinx.serialization.Serializable

@Serializable
data class AvatarPickerRoute(
    override val sessionId: WireSessionId,
    override val entryId: WireNavEntryId = WireNavEntryId.random(),
) : SessionRoute {
    override val routeId: String
        get() = ROUTE_ID

    companion object {
        const val ROUTE_ID = "app/avatar_picker_screen"
    }
}

/**
 * A wrapper deliberately preserves the difference between a successful null value and
 * Navigation 3 cancellation.
 */
@Serializable
data class AvatarPickerResult(
    val assetId: String?,
)

internal val AvatarPickerResultContract = WireNavResultContract<AvatarPickerResult>(
    WireNavResultContractId("user-profile.avatar-picker")
)
