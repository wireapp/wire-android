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

import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.navigation.WireNavEntryId
import com.wire.navigation.WireSessionId

internal fun DeviceDetailsNavArgs.toDeviceDetailsRoute(
    sessionId: WireSessionId,
    entryId: WireNavEntryId = WireNavEntryId.random(),
) = DeviceDetailsRoute(
    sessionId = sessionId,
    targetUserId = DeviceTargetUserId(userId.value, userId.domain),
    clientId = clientId.value,
    entryId = entryId,
)

internal fun DeviceDetailsRoute.toLegacyNavArgs() = DeviceDetailsNavArgs(
    userId = UserId(targetUserId.value, targetUserId.domain),
    clientId = ClientId(clientId),
)
