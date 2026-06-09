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
package com.wire.android.ui.common

import androidx.compose.runtime.Composable
import androidx.work.WorkManager
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.metro.MetroViewModelGraph
import com.wire.android.di.wireMetroViewModelScoped
import com.wire.android.ui.common.banner.SecurityClassificationArgs
import com.wire.android.ui.common.banner.SecurityClassificationViewModel
import com.wire.android.ui.common.banner.SecurityClassificationViewModelImpl
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModel
import com.wire.android.ui.common.bottomsheet.conversation.ConversationOptionsMenuViewModelImpl
import com.wire.android.ui.common.topappbar.CommonTopAppBarParams
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModel
import com.wire.android.ui.connection.ConnectionActionButtonArgs
import com.wire.android.ui.connection.ConnectionActionButtonViewModel
import com.wire.android.ui.connection.ConnectionActionButtonViewModelImpl
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.connection.AcceptConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.BlockUserUseCase
import com.wire.kalium.logic.feature.connection.CancelConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.IgnoreConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.SendConnectionRequestUseCase
import com.wire.kalium.logic.feature.connection.UnblockUserUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationLeaveConditionsUseCase
import com.wire.kalium.logic.feature.conversation.ClearConversationContentUseCase
import com.wire.kalium.logic.feature.conversation.GetOrCreateOneToOneConversationUseCase
import com.wire.kalium.logic.feature.conversation.LeaveConversationUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationDetailsUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationArchivedStatusUseCase
import com.wire.kalium.logic.feature.conversation.UpdateConversationMutedStatusUseCase
import com.wire.kalium.logic.feature.conversation.delete.MarkConversationAsDeletedLocallyUseCase
import com.wire.kalium.logic.feature.conversation.folder.AddConversationToFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFavoritesUseCase
import com.wire.kalium.logic.feature.conversation.folder.RemoveConversationFromFolderUseCase
import com.wire.kalium.logic.feature.team.DeleteTeamConversationUseCase
import com.wire.kalium.logic.feature.user.IsPreventAdminlessGroupsEnabledUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import dev.zacsweers.metro.Inject

interface CommonViewModelGraph : MetroViewModelGraph {
    val commonViewModelFactory: CommonViewModelFactory
}

@Composable
fun securityClassificationViewModel(args: SecurityClassificationArgs): SecurityClassificationViewModel =
    wireMetroViewModelScoped<
            CommonViewModelGraph,
            SecurityClassificationViewModelImpl,
            SecurityClassificationViewModel,
            SecurityClassificationArgs
            >(
        arguments = args
    ) { _, arguments ->
        commonViewModelFactory.securityClassificationViewModel(arguments)
    }

@Composable
fun connectionActionButtonViewModel(args: ConnectionActionButtonArgs): ConnectionActionButtonViewModel =
    wireMetroViewModelScoped<
            CommonViewModelGraph,
            ConnectionActionButtonViewModelImpl,
            ConnectionActionButtonViewModel,
            ConnectionActionButtonArgs
            >(
        arguments = args
    ) { _, arguments ->
        commonViewModelFactory.connectionActionButtonViewModel(arguments)
    }

@Composable
fun conversationOptionsMenuViewModel(): ConversationOptionsMenuViewModel =
    wireMetroViewModelScoped<CommonViewModelGraph, ConversationOptionsMenuViewModelImpl, ConversationOptionsMenuViewModel> {
        commonViewModelFactory.conversationOptionsMenuViewModel()
    }

@Suppress("LongParameterList")
class CommonViewModelFactory @Inject constructor(
    private val currentScreenManager: CurrentScreenManager,
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
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
    private val isPreventAdminlessGroupsEnabled: IsPreventAdminlessGroupsEnabledUseCase,
    private val blockUser: BlockUserUseCase,
    private val unblockUser: UnblockUserUseCase,
    private val clearConversationContent: ClearConversationContentUseCase,
    private val workManager: WorkManager,
    private val dispatchers: DispatcherProvider,
    private val sendConnectionRequest: SendConnectionRequestUseCase,
    private val cancelConnectionRequest: CancelConnectionRequestUseCase,
    private val acceptConnectionRequest: AcceptConnectionRequestUseCase,
    private val ignoreConnectionRequest: IgnoreConnectionRequestUseCase,
    private val getOrCreateOneToOneConversation: GetOrCreateOneToOneConversationUseCase,
) {
    fun commonTopAppBarViewModel(params: CommonTopAppBarParams) = CommonTopAppBarViewModel(
        currentScreenManager = currentScreenManager,
        coreLogic = coreLogic,
        params = params,
    )

    fun securityClassificationViewModel(args: SecurityClassificationArgs) = SecurityClassificationViewModelImpl(
        coreLogic = coreLogic.value,
        args = args,
    )

    fun conversationOptionsMenuViewModel() = ConversationOptionsMenuViewModelImpl(
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
        isPreventAdminlessGroupsEnabled = isPreventAdminlessGroupsEnabled,
        blockUser = blockUser,
        unblockUser = unblockUser,
        clearConversationContent = clearConversationContent,
        workManager = workManager,
        dispatchers = dispatchers,
    )

    internal fun connectionActionButtonViewModel(args: ConnectionActionButtonArgs) = ConnectionActionButtonViewModelImpl(
        dispatchers = dispatchers,
        sendConnectionRequest = sendConnectionRequest,
        cancelConnectionRequest = cancelConnectionRequest,
        acceptConnectionRequest = acceptConnectionRequest,
        ignoreConnectionRequest = ignoreConnectionRequest,
        unblockUser = unblockUser,
        getOrCreateOneToOneConversation = getOrCreateOneToOneConversation,
        args = args,
    )
}
