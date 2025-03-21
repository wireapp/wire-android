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

package com.wire.android.ui.home.conversations

enum class ConversationScreenDialogType {
    VERIFICATION_DEGRADED,
    ONGOING_ACTIVE_CALL,
    NO_CONNECTIVITY,
    CALL_CONFIRMATION,
    PING_CONFIRMATION,
    CALLING_FEATURE_UNAVAILABLE,
    CALLING_FEATURE_UNAVAILABLE_TEAM_MEMBER,
    CALLING_FEATURE_UNAVAILABLE_TEAM_ADMIN,
    CALLING_FEATURE_ACTIVATED,
    NONE
}
