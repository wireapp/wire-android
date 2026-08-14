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

import dev.zacsweers.metro.Assisted
import dev.zacsweers.metro.AssistedFactory
import dev.zacsweers.metro.AssistedInject

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ViewModelScopedPreview
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.SecurityClassificationType
import kotlinx.coroutines.launch
@ViewModelScopedPreview
interface SecurityClassificationViewModel {
    fun state(): SecurityClassificationType = SecurityClassificationType.NONE
}

class SecurityClassificationViewModelImpl @AssistedInject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @CurrentAccount private val currentAccount: UserId,
    @Assisted private val args: SecurityClassificationArgs,
) : SecurityClassificationViewModel, ViewModel() {
    @AssistedFactory
    interface Factory {
        fun create(args: SecurityClassificationArgs): SecurityClassificationViewModelImpl
    }

    private var state by mutableStateOf(SecurityClassificationType.NONE)

    override fun state(): SecurityClassificationType = state

    init {
        viewModelScope.launch {
            when (args) {
                is SecurityClassificationArgs.Conversation -> observeConversationClassificationType(currentAccount, args.id)
                is SecurityClassificationArgs.User -> observeUserClassificationType(currentAccount, args.id)
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
