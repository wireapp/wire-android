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
package com.wire.android.di.metro

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.CurrentAccount
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.ui.authentication.create.summary.CreateAccountSummaryViewModelFactory
import com.wire.android.ui.debug.featureflags.DebugFeatureFlagsViewModelFactory
import com.wire.android.ui.home.settings.about.dependencies.AndroidDependenciesInfoProvider
import com.wire.android.ui.home.settings.about.dependencies.DependenciesInfoProvider
import com.wire.android.ui.home.settings.about.dependencies.DependenciesViewModelFactory
import com.wire.android.ui.home.settings.about.licenses.AndroidLicensesProvider
import com.wire.android.ui.home.settings.about.licenses.LicensesProvider
import com.wire.android.ui.home.settings.about.licenses.LicensesViewModelFactory
import com.wire.android.ui.home.settings.appsettings.networkSettings.AndroidNetworkSettingsDefaultsProvider
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsDefaultsProvider
import com.wire.android.ui.home.settings.appsettings.networkSettings.NetworkSettingsViewModelFactory
import com.wire.android.ui.home.settings.appearance.CustomizationViewModelFactory
import com.wire.android.ui.home.whatsnew.AndroidReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.ReleaseNotesFeedUrlProvider
import com.wire.android.ui.home.whatsnew.WhatsNewViewModelFactory
import com.wire.android.ui.initialsync.InitialSyncViewModelFactory
import com.wire.android.ui.settings.about.AboutThisAppInfoProvider
import com.wire.android.ui.settings.about.AboutThisAppViewModelFactory
import com.wire.android.ui.settings.about.AndroidAboutThisAppInfoProvider
import com.wire.android.ui.home.conversations.media.CheckAssetRestrictionsViewModelFactory
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.lifecycle.AutomatedLoginManager
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.debug.GetFeatureConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory
import kotlinx.coroutines.runBlocking

abstract class WireMetroScope private constructor()

@DependencyGraph(WireMetroScope::class)
@Suppress("TooManyFunctions")
interface WireMetroGraph {
    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@Provides @ApplicationContext context: Context): WireMetroGraph
    }

    val checkAssetRestrictionsViewModelFactory: CheckAssetRestrictionsViewModelFactory
    val aboutThisAppViewModelFactory: AboutThisAppViewModelFactory
    val createAccountSummaryViewModelFactory: CreateAccountSummaryViewModelFactory
    val whatsNewViewModelFactory: WhatsNewViewModelFactory
    val dependenciesViewModelFactory: DependenciesViewModelFactory
    val licensesViewModelFactory: LicensesViewModelFactory
    val debugFeatureFlagsViewModelFactory: DebugFeatureFlagsViewModelFactory
    val customizationViewModelFactory: CustomizationViewModelFactory
    val initialSyncViewModelFactory: InitialSyncViewModelFactory
    val networkSettingsViewModelFactory: NetworkSettingsViewModelFactory

    @Provides
    fun provideWireMetroHiltEntryPoint(
        @ApplicationContext context: Context,
    ): WireMetroHiltEntryPoint =
        EntryPointAccessors.fromApplication(context, WireMetroHiltEntryPoint::class.java)

    @Provides
    fun provideGlobalDataStore(
        @ApplicationContext context: Context,
    ): GlobalDataStore = GlobalDataStore(context)

    @Provides
    fun provideCoreLogic(entryPoint: WireMetroHiltEntryPoint): CoreLogic = entryPoint.coreLogic()

    @Provides
    fun provideUserDataStoreProvider(entryPoint: WireMetroHiltEntryPoint): UserDataStoreProvider =
        entryPoint.userDataStoreProvider()

    @Provides
    fun provideDispatchers(entryPoint: WireMetroHiltEntryPoint): DispatcherProvider = entryPoint.dispatcherProvider()

    @Provides
    fun provideAutomatedLoginManager(entryPoint: WireMetroHiltEntryPoint): AutomatedLoginManager =
        entryPoint.automatedLoginManager()

    @Provides
    fun provideManagedConfigurationsManager(entryPoint: WireMetroHiltEntryPoint): ManagedConfigurationsManager =
        entryPoint.managedConfigurationsManager()

    @CurrentAccount
    @Provides
    fun provideCurrentSession(coreLogic: CoreLogic): UserId =
        runBlocking {
            when (val result = coreLogic.getGlobalScope().session.currentSession()) {
                is CurrentSessionResult.Success -> result.accountInfo.userId
                else -> throw IllegalStateException("no current session was found")
            }
        }

    @Provides
    fun provideObserveSyncStateUseCase(
        coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): ObserveSyncStateUseCase =
        coreLogic.getSessionScope(currentAccount).observeSyncState

    @Provides
    fun provideGetFeatureConfigUseCase(coreLogic: CoreLogic, @CurrentAccount currentAccount: UserId): GetFeatureConfigUseCase =
        coreLogic.getSessionScope(currentAccount).debug.getFeatureConfig

    @Provides
    fun provideNetworkSettingsDefaultsProvider(
        @ApplicationContext context: Context,
    ): NetworkSettingsDefaultsProvider = AndroidNetworkSettingsDefaultsProvider(context)

    @Provides
    fun provideCurrentSessionUseCase(coreLogic: CoreLogic): CurrentSessionUseCase =
        coreLogic.getGlobalScope().session.currentSession

    @Provides
    fun provideObservePersistentWebSocketConnectionStatusUseCase(
        coreLogic: CoreLogic,
    ): ObservePersistentWebSocketConnectionStatusUseCase =
        coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus

    @Provides
    fun providePersistPersistentWebSocketConnectionStatusUseCase(
        coreLogic: CoreLogic,
        @CurrentAccount currentAccount: UserId,
    ): PersistPersistentWebSocketConnectionStatusUseCase =
        coreLogic.getSessionScope(currentAccount).persistPersistentWebSocketConnectionStatus

    @Provides
    fun provideAboutThisAppInfoProvider(
        @ApplicationContext context: Context,
    ): AboutThisAppInfoProvider = AndroidAboutThisAppInfoProvider(context)

    @Provides
    fun provideReleaseNotesFeedUrlProvider(
        @ApplicationContext context: Context,
    ): ReleaseNotesFeedUrlProvider = AndroidReleaseNotesFeedUrlProvider(context)

    @Provides
    fun provideDependenciesInfoProvider(
        @ApplicationContext context: Context,
    ): DependenciesInfoProvider = AndroidDependenciesInfoProvider(context)

    @Provides
    fun provideLicensesProvider(
        @ApplicationContext context: Context,
    ): LicensesProvider = AndroidLicensesProvider(context)
}

fun createWireMetroGraph(context: Context): WireMetroGraph =
    createGraphFactory<WireMetroGraph.Factory>().create(context.applicationContext)

@EntryPoint
@InstallIn(SingletonComponent::class)
interface WireMetroHiltEntryPoint {
    @KaliumCoreLogic
    fun coreLogic(): CoreLogic

    fun userDataStoreProvider(): UserDataStoreProvider

    fun dispatcherProvider(): DispatcherProvider

    fun automatedLoginManager(): AutomatedLoginManager

    fun managedConfigurationsManager(): ManagedConfigurationsManager
}
