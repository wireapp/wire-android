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

package com.wire.android.ui.common.banner

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ViewModelScopedPreview
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.launch
@ViewModelScopedPreview
interface SecurityClassificationViewModel {
    fun state(): SecurityClassificationType = SecurityClassificationType.NONE
}

class SecurityClassificationViewModelImpl(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val args: SecurityClassificationArgs
) : SecurityClassificationViewModel, ViewModel() {

    private var state by mutableStateOf(SecurityClassificationType.NONE)

    override fun state(): SecurityClassificationType = state

    init {
        viewModelScope.launch {
            coreLogic.getGlobalScope().session.currentSessionFlow()
                .flatMapLatest { currentSession ->
                    when (currentSession) {
                        is CurrentSessionResult.Success -> when (args) {
                            is SecurityClassificationArgs.Conversation -> observeConversationClassificationType(
                                currentSession.accountInfo.userId,
                                args.id
                            )

                            is SecurityClassificationArgs.User -> observeUserClassificationType(
                                currentSession.accountInfo.userId,
                                args.id
                            )
                        }

                        is CurrentSessionResult.Failure -> flowOf(SecurityClassificationType.NONE)
                    }
                }
                .collect { classificationType ->
                    state = classificationType
                }
        }
    }

    private suspend fun observeConversationClassificationType(currentUserId: UserId, conversationId: ConversationId) =
        coreLogic.getSessionScope(currentUserId).observeSecurityClassificationLabel(conversationId)

    private suspend fun observeUserClassificationType(currentUserId: UserId, userId: UserId) =
        coreLogic.getSessionScope(currentUserId).getOtherUserSecurityClassificationLabel(userId)
}
