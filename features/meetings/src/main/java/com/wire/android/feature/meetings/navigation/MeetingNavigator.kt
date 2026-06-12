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
package com.wire.android.feature.meetings.navigation

import com.wire.android.navigation.WireNavigator
import com.wire.kalium.logic.data.user.UserId

/**
 * Navigator for the meetings feature. It extends [WireNavigator] and adds meeting-specific navigation functions that allows navigating to
 * different screens from other modules without needing to know about the implementation details of the whole navigation system.
 */
class MeetingNavigator(
    val navigator: WireNavigator,
    val navigateToProfile: (userId: UserId) -> Unit
) : WireNavigator by navigator
