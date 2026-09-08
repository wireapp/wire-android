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

package com.wire.android.ui.userprofile

import kotlinx.serialization.Serializable

/**
 * Navigation-owned representation of a qualified Wire identifier.
 *
 * Keeping Kalium models out of the route contract makes the back stack portable to every
 * Navigation 3 host while the pure mappers retain parity with the legacy destinations.
 */
@Serializable
data class UserProfileQualifiedId(
    val value: String,
    val domain: String,
) {
    init {
        require(value.isNotBlank()) { "A qualified id value cannot be blank" }
        require(domain.isNotBlank()) { "A qualified id domain cannot be blank" }
    }
}
