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
package com.wire.android.ui.home.conversations.privacy

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ramcosta.composedestinations.generated.app.navArgs
import com.wire.android.feature.privacy.auth.ConversationAuthenticator
import com.wire.android.feature.privacy.data.ConversationPrivacyRepository
import com.wire.android.feature.privacy.model.AutoLockTimeout
import com.wire.android.feature.privacy.model.ConversationPrivacyLevel
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

data class ConversationPrivacyState(
    val level: ConversationPrivacyLevel = ConversationPrivacyLevel.NORMAL,
    val autoLock: AutoLockTimeout = AutoLockTimeout.IMMEDIATELY,
    val isChatPinSet: Boolean = false,
    /** Set when the user picked HIGHLY_SENSITIVE but no Chat PIN exists yet — they must create one. */
    val needsChatPinSetup: Boolean = false,
    /** Set when the user picked HIGHLY_SENSITIVE and must confirm with their existing Chat PIN. */
    val needsChatPinConfirmation: Boolean = false,
    /** Set when the entered Chat PIN was incorrect during confirmation. */
    val pinError: Boolean = false,
)

class ConversationPrivacyViewModel(
    savedStateHandle: SavedStateHandle,
    private val repository: ConversationPrivacyRepository,
    private val authenticator: ConversationAuthenticator,
) : ViewModel() {

    private val navArgs: ConversationPrivacyNavArgs = savedStateHandle.navArgs()
    private val conversationId = navArgs.conversationId

    var state by mutableStateOf(ConversationPrivacyState())
        private set

    init {
        viewModelScope.launch {
            combine(repository.observe(conversationId), authenticator.isChatPinSet()) { settings, pinSet ->
                state.copy(level = settings.level, autoLock = settings.autoLock, isChatPinSet = pinSet)
            }.collect { state = it }
        }
    }

    fun onLevelSelected(level: ConversationPrivacyLevel) {
        if (level == state.level) return
        viewModelScope.launch {
            if (level == ConversationPrivacyLevel.HIGHLY_SENSITIVE) {
                 state = if (authenticator.isChatPinSetOnce()) {
                    state.copy(needsChatPinConfirmation = true, pinError = false)
                } else {
                    state.copy(needsChatPinSetup = true)
                }
                return@launch
            }
            repository.setLevel(conversationId, level)
        }
    }

    fun onAutoLockSelected(timeout: AutoLockTimeout) {
        viewModelScope.launch { repository.setAutoLock(conversationId, timeout) }
    }

    fun onChatPinDialogDismissed() {
        state = state.copy(needsChatPinSetup = false, needsChatPinConfirmation = false, pinError = false)
    }

    fun clearPinError() {
        if (state.pinError) state = state.copy(pinError = false)
    }

    /** Create the Chat PIN inline, then apply the pending HIGHLY_SENSITIVE level. */
    fun onChatPinCreated(pin: String) {
        viewModelScope.launch {
            authenticator.setChatPin(pin)
            repository.setLevel(conversationId, ConversationPrivacyLevel.HIGHLY_SENSITIVE)
            state = state.copy(needsChatPinSetup = false)
        }
    }

    /** Verify the existing Chat PIN and, when correct, apply the pending HIGHLY_SENSITIVE level. */
    fun onChatPinConfirmed(pin: String) {
        viewModelScope.launch {
            if (authenticator.verifyChatPin(pin)) {
                repository.setLevel(conversationId, ConversationPrivacyLevel.HIGHLY_SENSITIVE)
                state = state.copy(needsChatPinConfirmation = false, pinError = false)
            } else {
                state = state.copy(pinError = true)
            }
        }
    }
}
