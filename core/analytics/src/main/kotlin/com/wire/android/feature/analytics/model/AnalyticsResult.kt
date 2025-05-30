/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.feature.analytics.model

import com.wire.kalium.logic.data.analytics.AnalyticsIdentifierResult

data class AnalyticsResult<T>(
    val identifierResult: AnalyticsIdentifierResult,
    val profileProperties: suspend () -> AnalyticsProfileProperties,
    val manager: T?
)

data class AnalyticsProfileProperties(
    val isTeamMember: Boolean,
    val teamId: String?,
    val contactsAmount: Int?,
    val teamMembersAmount: Int?,
    val isEnterprise: Boolean?
)
