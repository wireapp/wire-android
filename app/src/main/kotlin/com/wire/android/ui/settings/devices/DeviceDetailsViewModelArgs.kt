/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.wire.android.ui.settings.devices

import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId

/**
 * Navigation-runtime-neutral arguments consumed by [DeviceDetailsViewModel].
 *
 * Both the legacy destination and Navigation 3 map to this type at their boundary. The ViewModel
 * therefore has no dependency on a generated destination or a navigation-owned SavedStateHandle.
 */
data class DeviceDetailsViewModelArgs(
    val userId: UserId,
    val clientId: ClientId,
)

internal fun DeviceDetailsNavArgs.toViewModelArgs() = DeviceDetailsViewModelArgs(
    userId = userId,
    clientId = clientId,
)

internal fun DeviceDetailsRoute.toViewModelArgs() = DeviceDetailsViewModelArgs(
    userId = UserId(targetUserId.value, targetUserId.domain),
    clientId = ClientId(clientId),
)
