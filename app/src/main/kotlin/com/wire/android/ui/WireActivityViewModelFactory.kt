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

import androidx.work.WorkManager
import com.wire.android.config.NomadProfilesFeatureConfig
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.IsProfileQRCodeEnabledUseCaseProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.ObserveIfE2EIRequiredDuringLoginUseCaseProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.di.ObserveSelfUserUseCaseProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.navigation.LoginTypeSelector
import com.wire.android.services.ServicesManager
import com.wire.android.sync.MonitorSyncWorkUseCase
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.DoesValidNomadAccountExistUseCase
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
import dagger.Lazy

@Suppress("LongParameterList")
class WireActivityViewModelFactory(
    @KaliumCoreLogic private val coreLogic: Lazy<CoreLogic>,
    private val dispatchers: DispatcherProvider,
    private val currentSessionFlow: Lazy<CurrentSessionFlowUseCase>,
    private val doesValidSessionExist: Lazy<DoesValidSessionExistUseCase>,
    private val getServerConfigUseCase: Lazy<GetServerConfigUseCase>,
    private val intentGateway: Lazy<WireActivityIntentGateway>,
    private val observeSessions: Lazy<ObserveSessionsUseCase>,
    private val accountSwitch: Lazy<AccountSwitchUseCase>,
    private val servicesManager: Lazy<ServicesManager>,
    private val observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory,
    private val observeIfAppUpdateRequired: Lazy<ObserveIfAppUpdateRequiredUseCase>,
    private val observeNewClients: Lazy<ObserveNewClientsUseCase>,
    private val clearNewClientsForUser: Lazy<ClearNewClientsForUserUseCase>,
    private val currentScreenManager: Lazy<CurrentScreenManager>,
    private val observeScreenshotCensoringConfigUseCaseProviderFactory:
    ObserveScreenshotCensoringConfigUseCaseProvider.Factory,
    private val globalDataStore: Lazy<GlobalDataStore>,
    private val observeIfE2EIRequiredDuringLoginUseCaseProviderFactory:
    ObserveIfE2EIRequiredDuringLoginUseCaseProvider.Factory,
    private val workManager: Lazy<WorkManager>,
    private val isProfileQRCodeEnabledFactory: IsProfileQRCodeEnabledUseCaseProvider.Factory,
    private val observeSelfUserFactory: ObserveSelfUserUseCaseProvider.Factory,
    private val monitorSyncWorkUseCase: MonitorSyncWorkUseCase,
    private val managedConfigurationsManager: ManagedConfigurationsManager,
    private val automatedLoginManager: AutomatedLoginManager,
    private val nomadProfilesFeatureConfig: NomadProfilesFeatureConfig,
    private val loginTypeSelector: LoginTypeSelector,
    private val doesValidNomadAccountExist: Lazy<DoesValidNomadAccountExistUseCase>,
) {
    fun create(): WireActivityViewModel = WireActivityViewModel(
        coreLogic = coreLogic,
        dispatchers = dispatchers,
        currentSessionFlow = currentSessionFlow,
        doesValidSessionExist = doesValidSessionExist,
        getServerConfigUseCase = getServerConfigUseCase,
        intentGateway = intentGateway,
        observeSessions = observeSessions,
        accountSwitch = accountSwitch,
        servicesManager = servicesManager,
        observeSyncStateUseCaseProviderFactory = observeSyncStateUseCaseProviderFactory,
        observeIfAppUpdateRequired = observeIfAppUpdateRequired,
        observeNewClients = observeNewClients,
        clearNewClientsForUser = clearNewClientsForUser,
        currentScreenManager = currentScreenManager,
        observeScreenshotCensoringConfigUseCaseProviderFactory = observeScreenshotCensoringConfigUseCaseProviderFactory,
        globalDataStore = globalDataStore,
        observeIfE2EIRequiredDuringLoginUseCaseProviderFactory = observeIfE2EIRequiredDuringLoginUseCaseProviderFactory,
        workManager = workManager,
        isProfileQRCodeEnabledFactory = isProfileQRCodeEnabledFactory,
        observeSelfUserFactory = observeSelfUserFactory,
        monitorSyncWorkUseCase = monitorSyncWorkUseCase,
        managedConfigurationsManager = managedConfigurationsManager,
        automatedLoginManager = automatedLoginManager,
        nomadProfilesFeatureConfig = nomadProfilesFeatureConfig,
        loginTypeSelector = loginTypeSelector,
        doesValidNomadAccountExist = doesValidNomadAccountExist,
    )
}
