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
package com.wire.android.ui.home.conversations.info

import com.wire.android.di.CurrentAccount
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.e2ei.usecase.FetchConversationMLSVerificationStatusUseCase
import dev.zacsweers.metro.Inject

@Inject
class ConversationInfoViewModelFactory(
    private val qualifiedIdMapper: QualifiedIdMapper,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val fetchConversationMLSVerificationStatus: FetchConversationMLSVerificationStatusUseCase,
    private val isWireCellFeatureEnabled: IsWireCellsEnabledUseCase,
    @CurrentAccount private val selfUserId: UserId,
) {
    fun create(args: ConversationNavArgs): ConversationInfoViewModel = ConversationInfoViewModel(
        conversationNavArgs = args,
        qualifiedIdMapper = qualifiedIdMapper,
        observeConversationDetails = observeConversationDetails,
        fetchConversationMLSVerificationStatus = fetchConversationMLSVerificationStatus,
        isWireCellFeatureEnabled = isWireCellFeatureEnabled,
        selfUserId = selfUserId,
    )
}
