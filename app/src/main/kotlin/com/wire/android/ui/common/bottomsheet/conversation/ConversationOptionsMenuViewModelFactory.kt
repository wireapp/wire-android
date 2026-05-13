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
package com.wire.android.ui.common.bottomsheet.conversation

import androidx.work.WorkManager
import com.wire.android.di.CurrentAccount
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationLeaveConditionsUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedLocallyUseCase
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Inject

@Inject
@Suppress("LongParameterList")
class ConversationOptionsMenuViewModelFactory(
    @CurrentAccount private val currentAccount: UserId,
    private val observeConversationDetails: ObserveConversationDetailsUseCase,
    private val observeSelfUser: ObserveSelfUserUseCase,
    private val addConversationToFavorites: AddConversationToFavoritesUseCase,
    private val removeConversationFromFavorites: RemoveConversationFromFavoritesUseCase,
    private val removeConversationFromFolder: RemoveConversationFromFolderUseCase,
    private val updateConversationArchivedStatus: UpdateConversationArchivedStatusUseCase,
    private val updateConversationMutedStatus: UpdateConversationMutedStatusUseCase,
    private val deleteTeamConversation: DeleteTeamConversationUseCase,
    private val markConversationAsDeletedLocally: MarkConversationAsDeletedLocallyUseCase,
    private val leaveConversation: LeaveConversationUseCase,
    private val checkConversationLeaveConditions: CheckConversationLeaveConditionsUseCase,
    private val blockUser: BlockUserUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val clearConversationContent: ClearConversationContentUseCase,
    private val workManager: WorkManager,
    private val dispatchers: DispatcherProvider,
) {
    fun create(): ConversationOptionsMenuViewModel = ConversationOptionsMenuViewModelImpl(
        currentAccount = currentAccount,
        observeConversationDetails = observeConversationDetails,
        observeSelfUser = observeSelfUser,
        addConversationToFavorites = addConversationToFavorites,
        removeConversationFromFavorites = removeConversationFromFavorites,
        removeConversationFromFolder = removeConversationFromFolder,
        updateConversationArchivedStatus = updateConversationArchivedStatus,
        updateConversationMutedStatus = updateConversationMutedStatus,
        deleteTeamConversation = deleteTeamConversation,
        markConversationAsDeletedLocally = markConversationAsDeletedLocally,
        leaveConversation = leaveConversation,
        checkConversationLeaveConditions = checkConversationLeaveConditions,
        blockUser = blockUser,
        unblockUser = unblockUser,
        clearConversationContent = clearConversationContent,
        workManager = workManager,
        dispatchers = dispatchers,
    )
}
