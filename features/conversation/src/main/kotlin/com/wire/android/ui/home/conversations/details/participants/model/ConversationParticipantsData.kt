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

package com.wire.android.ui.home.conversations.details.participants.model

data class ConversationParticipantsData(
    val admins: List<UIParticipant> = listOf(),
    val participants: List<UIParticipant> = listOf(),
    val apps: List<UIParticipant> = listOf(),
    val allAdminsCount: Int = 0,
    val allParticipantsCount: Int = 0,
    val allAppsCount: Int = 0,
    val isSelfAnAdmin: Boolean = false,
    val isSelfExternalMember: Boolean = false,
    val isSelfGuest: Boolean = false,
) {
    val allCount: Int = allAdminsCount + allParticipantsCount + allAppsCount
    val allParticipants: List<UIParticipant> = participants + admins + apps
}
