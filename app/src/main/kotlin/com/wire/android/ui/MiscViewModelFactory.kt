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
package com.wire.android.ui

import androidx.lifecycle.SavedStateHandle
import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.ui.analytics.AnalyticsConfiguration
import com.wire.android.ui.analytics.AnalyticsUsageViewModel
import com.wire.android.ui.e2eiEnrollment.E2EIEnrollmentViewModel
import com.wire.android.ui.e2eiEnrollment.GetE2EICertificateViewModel
import com.wire.android.ui.initialsync.InitialSyncViewModel
import com.wire.android.ui.joinConversation.JoinConversationViaCodeViewModel
import com.wire.android.ui.legalhold.dialog.deactivated.LegalHoldDeactivatedViewModel
import com.wire.android.ui.legalhold.dialog.requested.LegalHoldRequestedViewModel
import com.wire.android.ui.settings.devices.e2ei.E2eiCertificateDetailsViewModel
import com.wire.android.ui.sharing.ImportMediaAuthenticatedViewModel
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.ValidatePasswordUseCase
import com.wire.kalium.logic.feature.client.FinalizeMLSClientAfterE2EIEnrollmentUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.GetSelfUserUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.Lazy
import javax.inject.Inject

@Suppress("LongParameterList")
class MiscViewModelFactory @Inject constructor(
    private val analyticsEnabled: AnalyticsConfiguration,
    private val dataStore: Lazy<UserDataStore>,
    private val selfServerConfig: Lazy<SelfServerConfigUseCase>,
    private val observeSyncState: ObserveSyncStateUseCase,
    private val userDataStoreProvider: UserDataStoreProvider,
    @CurrentAccount private val userId: UserId,
    private val dispatchers: DispatcherProvider,
    private val automatedLoginManager: AutomatedLoginManager,
    private val validatePassword: ValidatePasswordUseCase,
    @KaliumCoreLogic private val coreLogicLazy: Lazy<CoreLogic>,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val finalizeMLSClientAfterE2EIEnrollment: FinalizeMLSClientAfterE2EIEnrollmentUseCase,
    private val currentSession: CurrentSessionUseCase,
    private val getSelfUser: GetSelfUserUseCase,
    private val getSelf: ObserveSelfUserUseCase,
    private val getConversationsPaginated: GetConversationsFromSearchUseCase,
    private val handleUriAsset: HandleUriAssetUseCase,
    private val persistNewSelfDeletionTimer: PersistNewSelfDeletionTimerUseCase,
    private val observeSelfDeletionSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase,
    private val joinConversationViaCode: JoinConversationViaCodeUseCase,
) {
    fun analyticsUsageViewModel() = AnalyticsUsageViewModel(
        analyticsEnabled = analyticsEnabled,
        dataStore = dataStore,
        selfServerConfig = selfServerConfig,
    )

    fun initialSyncViewModel() = InitialSyncViewModel(
        observeSyncState = observeSyncState,
        userDataStoreProvider = userDataStoreProvider,
        userId = userId,
        dispatchers = dispatchers,
        automatedLoginManager = automatedLoginManager,
    )

    fun legalHoldRequestedViewModel() = LegalHoldRequestedViewModel(
        validatePassword = validatePassword,
        coreLogic = coreLogicLazy,
    )

    fun legalHoldDeactivatedViewModel() = LegalHoldDeactivatedViewModel(
        coreLogic = coreLogicLazy,
    )

    fun e2EIEnrollmentViewModel() = E2EIEnrollmentViewModel(
        finalizeMLSClientAfterE2EIEnrollment = finalizeMLSClientAfterE2EIEnrollment,
    )

    fun getE2EICertificateViewModel() = GetE2EICertificateViewModel(
        coreLogic = coreLogic,
        currentSession = currentSession,
        dispatcherProvider = dispatchers,
    )

    fun e2eiCertificateDetailsViewModel(savedStateHandle: SavedStateHandle) = E2eiCertificateDetailsViewModel(
        savedStateHandle = savedStateHandle,
        getSelfUser = getSelfUser,
    )

    fun importMediaAuthenticatedViewModel() = ImportMediaAuthenticatedViewModel(
        getSelf = getSelf,
        getConversationsPaginated = getConversationsPaginated,
        handleUriAsset = handleUriAsset,
        persistNewSelfDeletionTimerUseCase = persistNewSelfDeletionTimer,
        observeSelfDeletionSettingsForConversation = observeSelfDeletionSettingsForConversation,
        dispatchers = dispatchers,
    )

    fun joinConversationViaCodeViewModel() = JoinConversationViaCodeViewModel(
        joinViaCode = joinConversationViaCode,
    )
}
