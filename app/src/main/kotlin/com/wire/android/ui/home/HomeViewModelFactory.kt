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
package com.wire.android.ui.home

import androidx.lifecycle.SavedStateHandle
import com.wire.android.BuildConfig
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStore
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.mapper.UserTypeMapper
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.feature.DisableAppLockUseCase
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversationslist.ConversationListViewModelImpl
import com.wire.android.ui.home.conversationslist.model.ConversationsSource
import com.wire.android.ui.home.drawer.HomeDrawerViewModel
import com.wire.android.ui.home.sync.FeatureFlagNotificationViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.ui.UiTextResolver
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.IsWireCellsEnabledUseCase
import com.wire.kalium.logic.feature.client.NeedsToRegisterClientUseCase
import com.wire.kalium.logic.feature.conversation.ObserveArchivedUnreadConversationsCountUseCase
import com.wire.kalium.logic.feature.conversation.ObserveConversationListDetailsWithEventsUseCase
import com.wire.kalium.logic.feature.conversation.RefreshConversationsWithoutMetadataUseCase
import com.wire.kalium.logic.feature.debug.ObserveDebugCRLExpirationAfterOneMinuteUseCase
import com.wire.kalium.logic.feature.legalhold.ObserveLegalHoldStateForSelfUserUseCase
import com.wire.kalium.logic.feature.personaltoteamaccount.CanMigrateFromPersonalToTeamUseCase
import com.wire.kalium.logic.feature.publicuser.RefreshUsersWithoutMetadataUseCase
import com.wire.kalium.logic.feature.server.GetTeamUrlUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.user.GetSelfTeamIdUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.sync.ForegroundActionsUseCase
import dagger.Lazy
import javax.inject.Inject

@Suppress("LongParameterList")
class HomeViewModelFactory @Inject constructor(
    private val dataStore: UserDataStore,
    private val observeSelf: ObserveSelfUserUseCase,
    private val needsToRegisterClient: NeedsToRegisterClientUseCase,
    private val canMigrateFromPersonalToTeam: CanMigrateFromPersonalToTeamUseCase,
    private val observeLegalHoldStatusForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val foregroundActionsUseCase: ForegroundActionsUseCase,
    private val observeDebugCRLExpirationAfterOneMinute: ObserveDebugCRLExpirationAfterOneMinuteUseCase,
    private val dispatcher: DispatcherProvider,
    private val observeArchivedUnreadConversationsCount: Lazy<ObserveArchivedUnreadConversationsCountUseCase>,
    private val getTeamUrl: GetTeamUrlUseCase,
    private val isWireCellsEnabled: IsWireCellsEnabledUseCase,
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    private val globalDataStore: Lazy<GlobalDataStore>,
    private val disableAppLockUseCase: Lazy<DisableAppLockUseCase>,
    private val getConversationsPaginated: GetConversationsFromSearchUseCase,
    private val observeConversationListDetailsWithEvents: ObserveConversationListDetailsWithEventsUseCase,
    private val refreshUsersWithoutMetadata: RefreshUsersWithoutMetadataUseCase,
    private val refreshConversationsWithoutMetadata: RefreshConversationsWithoutMetadataUseCase,
    private val observeLegalHoldStateForSelfUser: ObserveLegalHoldStateForSelfUserUseCase,
    private val audioMessagePlayer: ConversationAudioMessagePlayer,
    @CurrentAccount private val currentAccount: UserId,
    private val userTypeMapper: UserTypeMapper,
    private val getSelfTeamId: GetSelfTeamIdUseCase,
    private val uiTextResolver: UiTextResolver,
) {
    fun homeViewModel(savedStateHandle: SavedStateHandle) = HomeViewModel(
        savedStateHandle = savedStateHandle,
        dataStore = dataStore,
        observeSelf = observeSelf,
        needsToRegisterClient = needsToRegisterClient,
        canMigrateFromPersonalToTeam = canMigrateFromPersonalToTeam,
        observeLegalHoldStatusForSelfUser = observeLegalHoldStatusForSelfUser,
        currentSessionFlow = currentSessionFlow,
    )

    fun appSyncViewModel() = AppSyncViewModel(
        foregroundActionsUseCase = foregroundActionsUseCase,
        observeDebugCRLExpirationAfterOneMinute = observeDebugCRLExpirationAfterOneMinute,
        dispatcher = dispatcher,
    )

    fun homeDrawerViewModel(savedStateHandle: SavedStateHandle) = HomeDrawerViewModel(
        savedStateHandle = savedStateHandle,
        observeArchivedUnreadConversationsCount = observeArchivedUnreadConversationsCount,
        observeSelfUser = observeSelf,
        getTeamUrl = getTeamUrl,
        isWireCellsEnabled = isWireCellsEnabled,
    )

    fun featureFlagNotificationViewModel() = FeatureFlagNotificationViewModel(
        coreLogic = coreLogic,
        currentSessionFlow = currentSessionFlow,
        globalDataStore = globalDataStore,
        disableAppLockUseCase = disableAppLockUseCase,
    )

    fun conversationListViewModel(
        conversationsSource: ConversationsSource,
        usePagination: Boolean = BuildConfig.PAGINATED_CONVERSATION_LIST_ENABLED,
    ) = ConversationListViewModelImpl(
        conversationsSource = conversationsSource,
        usePagination = usePagination,
        dispatcher = dispatcher,
        getConversationsPaginated = getConversationsPaginated,
        observeConversationListDetailsWithEvents = observeConversationListDetailsWithEvents,
        refreshUsersWithoutMetadata = refreshUsersWithoutMetadata,
        refreshConversationsWithoutMetadata = refreshConversationsWithoutMetadata,
        observeLegalHoldStateForSelfUser = observeLegalHoldStateForSelfUser,
        audioMessagePlayer = audioMessagePlayer,
        currentAccount = currentAccount,
        userTypeMapper = userTypeMapper,
        getSelfTeamId = getSelfTeamId,
        uiTextResolver = uiTextResolver,
    )
}
