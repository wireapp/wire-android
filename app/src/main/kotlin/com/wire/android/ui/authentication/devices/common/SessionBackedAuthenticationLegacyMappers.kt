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

package com.wire.android.ui.authentication.devices.common

import com.wire.navigation.WireSessionId

/**
 * Bridges the nullable pair used by the legacy generated destination to a typed Navigation 3
 * session identity.
 *
 * These screens are rendered from a session Metro graph. Silently treating a partially populated
 * identity as an authentication-scoped route could attach the screen to the wrong account, so the
 * migration bridge rejects that state.
 */
internal fun SessionBackedAuthenticationNavArgs.requireWireSessionId(): WireSessionId {
    val value = requireNotNull(userIdValue) {
        "A session-backed authentication route requires a user id value"
    }
    val domain = requireNotNull(userIdDomain) {
        "A session-backed authentication route requires a user id domain"
    }
    return WireSessionId(value, domain)
}
