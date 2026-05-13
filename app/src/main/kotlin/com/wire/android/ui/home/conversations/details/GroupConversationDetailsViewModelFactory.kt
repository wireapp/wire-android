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
package com.wire.android.ui.home.conversations.details

import com.wire.android.ui.home.conversations.details.participants.usecase.ObserveParticipantsForConversationUseCase
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationReceiptModeUseCase
import com.wire.kalium.logic.feature.featureConfig.ObserveIsAppsAllowedForUsageUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.user.IsMLSEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserWithTeamUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class GroupConversationDetailsViewModelFactory(
    private val dispatcher: DispatcherProvider,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeConversationMembers: ObserveParticipantsForConversationUseCase,
    private val observeSelfUserWithTeam: ObserveSelfUserWithTeamUseCase,
    private val updateConversationReceiptMode: UpdateConversationReceiptModeUseCase,
    private val observeSelfDeletionTimerSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val observeIsAppsAllowedForUsage: ObserveIsAppsAllowedForUsageUseCase,
    private val isMLSEnabled: IsMLSEnabledUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
) {
    fun create(args: GroupConversationDetailsNavArgs): GroupConversationDetailsViewModel = GroupConversationDetailsViewModel(
        groupConversationDetailsNavArgs = args,
        dispatcher = dispatcher,
        observeConversationDetails = observeConversationDetails,
        observeConversationMembers = observeConversationMembers,
        observeSelfUserWithTeam = observeSelfUserWithTeam,
        updateConversationReceiptMode = updateConversationReceiptMode,
        observeSelfDeletionTimerSettingsForConversation = observeSelfDeletionTimerSettingsForConversation,
        observeIsAppsAllowedForUsage = observeIsAppsAllowedForUsage,
        isMLSEnabled = isMLSEnabled,
        refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
        isWireCellsEnabled = isWireCellsEnabled,
    )
}
