/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf

@Stable
class ParticipantsExpansionState {
    private val membersExpanded: MutableState<Boolean> = mutableStateOf(true)
    private val adminsExpanded: MutableState<Boolean> = mutableStateOf(true)
    private val appsExpanded: MutableState<Boolean> = mutableStateOf(true)

    val membersActions =
        MemberSectionActions.WithSectionActions(membersExpanded) { membersExpanded.value = it }
    val adminsActions =
        MemberSectionActions.WithSectionActions(adminsExpanded) { adminsExpanded.value = it }
    val appsActions =
        MemberSectionActions.WithSectionActions(appsExpanded) { appsExpanded.value = it }
}

sealed class MemberSectionActions {
    @Stable
    data class WithSectionActions(
        val expanded: MutableState<Boolean>,
        val onExpansionChanged: (Boolean) -> Unit
    ) : MemberSectionActions()

    @Stable
    data object NoActions : MemberSectionActions()
}
