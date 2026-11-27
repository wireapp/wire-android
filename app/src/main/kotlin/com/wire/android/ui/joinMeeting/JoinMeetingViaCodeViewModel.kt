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
package com.wire.android.ui.joinMeeting

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.ui.common.textfield.textAsFlow
import com.wire.kalium.logic.feature.meeting.JoinMeetingViaCodeUseCase
import com.wire.kalium.logic.feature.user.IsMeetingsEnabledUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.launch

/** Session-scoped state for joining one meeting link at a time. */
class JoinMeetingViaCodeViewModel @Inject constructor(
    private val joinViaCode: JoinMeetingViaCodeUseCase,
    private val isMeetingsEnabled: IsMeetingsEnabledUseCase,
) : ViewModel() {
    var state: JoinMeetingDialogState by mutableStateOf(JoinMeetingDialogState.Idle)
        private set

    val passwordTextState = TextFieldState()

    init {
        viewModelScope.launch {
            passwordTextState.textAsFlow().distinctUntilChanged().collect {
                val passwordState = state as? JoinMeetingDialogState.PasswordRequired
                if (passwordState?.error != null) state = JoinMeetingDialogState.PasswordRequired()
            }
        }
    }

    suspend fun startJoining(link: MeetingJoinLink) {
        passwordTextState.clearText()
        join(link, null)
    }

    suspend fun submitPassword(link: MeetingJoinLink) {
        val passwordState = state as? JoinMeetingDialogState.PasswordRequired ?: return
        val wrongPassword = passwordState.error == JoinMeetingDialogState.Error.InvalidPassword
        if (passwordState.loading || wrongPassword || passwordTextState.text.isBlank()) return
        join(link, passwordTextState.text.toString())
    }

    private suspend fun join(link: MeetingJoinLink, password: String?) {
        state = if (password == null) JoinMeetingDialogState.Loading else JoinMeetingDialogState.PasswordRequired(loading = true)
        val result = if (isMeetingsEnabled.isGuestLinkSharingSupported()) {
            joinViaCode(domain = link.domain, key = link.key, code = link.code, password = password)
        } else {
            JoinMeetingViaCodeUseCase.Result.LinkUnavailable
        }
        val resultState = when (result) {
            JoinMeetingViaCodeUseCase.Result.Success -> JoinMeetingDialogState.Success
            JoinMeetingViaCodeUseCase.Result.InvalidPassword ->
                if (password == null) JoinMeetingDialogState.PasswordRequired() else JoinMeetingDialogState.Error.InvalidPassword
            JoinMeetingViaCodeUseCase.Result.MeetingNotFound -> JoinMeetingDialogState.Error.NotFound
            JoinMeetingViaCodeUseCase.Result.LinkUnavailable -> JoinMeetingDialogState.Error.Unavailable
            JoinMeetingViaCodeUseCase.Result.Failure -> JoinMeetingDialogState.Error.Generic
        }
        state = if (password != null && resultState is JoinMeetingDialogState.Error) {
            JoinMeetingDialogState.PasswordRequired(error = resultState)
        } else {
            resultState
        }
    }

    fun reset() {
        state = JoinMeetingDialogState.Idle
        passwordTextState.clearText()
    }

    suspend fun retryJoining(link: MeetingJoinLink) {
        if (state is JoinMeetingDialogState.Error) startJoining(link)
    }
}
