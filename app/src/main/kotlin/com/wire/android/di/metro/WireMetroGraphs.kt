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
import com.wire.android.GlobalObserversManager
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.AppModule
import com.wire.android.di.ApplicationContext
import com.wire.android.di.CoreLogicModule
import com.wire.android.di.CoroutinesDispatchersModule
import com.wire.android.di.CoroutinesScopesModule
import com.wire.android.di.ImageLoadingModule
import com.wire.android.di.KaliumConfigsModule
import com.wire.android.di.LogWriterModule
import com.wire.android.di.ManagedConfigurationsModule
import com.wire.android.di.NoSession
import com.wire.android.di.NoSessionAuthenticationModule
import com.wire.android.di.SessionModule
import com.wire.android.di.UiCommonModule
import com.wire.android.di.UseCaseModule
import com.wire.android.di.accountScoped.AppsModule
import com.wire.android.di.accountScoped.AuthenticationModule
import com.wire.android.di.accountScoped.BackupModule
import com.wire.android.di.accountScoped.CallsModule
import com.wire.android.di.accountScoped.CellsModule
import com.wire.android.di.accountScoped.ChannelsModule
import com.wire.android.di.accountScoped.ClientModule
import com.wire.android.di.accountScoped.ConnectionModule
import com.wire.android.di.accountScoped.ConversationModule
import com.wire.android.di.accountScoped.DebugModule
import com.wire.android.di.accountScoped.MessageModule
import com.wire.android.di.accountScoped.SearchModule
import com.wire.android.di.accountScoped.ServicesModule
import com.wire.android.di.accountScoped.TeamModule
import com.wire.android.di.accountScoped.UserModule
import com.wire.android.config.NomadProfilesFeatureConfig
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.StartPersistentWebsocketIfNecessaryUseCase
import com.wire.android.feature.cells.ui.CellsViewModelFactory
import com.wire.android.feature.cells.ui.CellsViewModelGraph
import com.wire.android.feature.meetings.ui.MeetingsViewModelFactory
import com.wire.android.feature.meetings.ui.MeetingsViewModelGraph
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.model.ImageAssetViewModelFactory
import com.wire.android.model.ImageAssetViewModelGraph
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.notification.broadcastreceivers.DynamicReceiversManager
import com.wire.android.notification.CallNotificationManager
import com.wire.android.services.CallServiceManager
import com.wire.android.ui.MiscViewModelFactory
import com.wire.android.ui.MiscViewModelGraph
import com.wire.android.ui.WireActivityViewModel
import com.wire.android.ui.authentication.AuthenticationViewModelFactory
import com.wire.android.ui.authentication.AuthenticationViewModelGraph
import com.wire.android.ui.calling.CallingViewModelFactory
import com.wire.android.ui.calling.CallingViewModelGraph
import com.wire.android.ui.calling.common.ProximitySensorManager
import com.wire.android.ui.common.CommonViewModelFactory
import com.wire.android.ui.common.CommonViewModelGraph
import com.wire.android.ui.debug.DebugInfoViewModelFactory
import com.wire.android.ui.debug.DebugInfoViewModelGraph
import com.wire.android.ui.home.HomeViewModelFactory
import com.wire.android.ui.home.HomeViewModelGraph
import com.wire.android.ui.home.appLock.LockCodeTimeManager
import com.wire.android.ui.home.conversations.ConversationCoreViewModelFactory
import com.wire.android.ui.home.conversations.ConversationCoreViewModelGraph
import com.wire.android.ui.home.conversations.ConversationDetailsViewModelFactory
import com.wire.android.ui.home.conversations.ConversationDetailsViewModelGraph
import com.wire.android.ui.home.conversations.ConversationSearchFolderViewModelFactory
import com.wire.android.ui.home.conversations.ConversationSearchFolderViewModelGraph
import com.wire.android.ui.home.conversations.ScopedMessageViewModelFactory
import com.wire.android.ui.home.conversations.ScopedMessageViewModelGraph
import com.wire.android.ui.home.settings.SettingsViewModelFactory
import com.wire.android.ui.home.settings.SettingsViewModelGraph
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.util.logging.LogFileWriter
import com.wire.android.util.ui.WireSessionImageLoader
import com.wire.android.services.ServicesManager
import com.wire.android.workmanager.WireWorkerFactory
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.android.util.NetworkUtil
import dagger.Lazy
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.GraphExtension
import dev.zacsweers.metro.Provides
import kotlinx.coroutines.CoroutineScope
import javax.inject.Provider
import javax.inject.Scope
import javax.inject.Singleton

@Scope
annotation class SessionScope

@Singleton
@DependencyGraph(
    bindingContainers = [
        AppModule::class,
        CoreLogicModule::class,
        CoroutinesScopesModule::class,
        CoroutinesDispatchersModule::class,
        KaliumConfigsModule::class,
        LogWriterModule::class,
        ManagedConfigurationsModule::class,
        NoSessionAuthenticationModule::class,
        UiCommonModule::class,
    ]
)
interface WireAppGraph : AuthenticationViewModelGraph, WireSessionGraph.Factory {
    @com.wire.android.di.KaliumCoreLogic
    val coreLogic: CoreLogic
    val logFileWriter: LogFileWriter
    val syncLifecycleManager: com.wire.android.util.lifecycle.SyncLifecycleManager
    val wireWorkerFactory: WireWorkerFactory
    val globalObserversManager: GlobalObserversManager
    val globalDataStore: GlobalDataStore
    val userDataStoreProvider: UserDataStoreProvider
    @com.wire.android.di.ApplicationScope
    val globalAppScope: CoroutineScope
    val currentScreenManager: CurrentScreenManager
    val analyticsManager: com.wire.android.feature.analytics.AnonymousAnalyticsManager
    val workManager: androidx.work.WorkManager
    val loginTypeSelector: com.wire.android.navigation.LoginTypeSelector
    val lockCodeTimeManager: LockCodeTimeManager
    val switchAccountObserver: SwitchAccountObserver
    val dynamicReceiversManager: DynamicReceiversManager
    val managedConfigurationsManager: ManagedConfigurationsManager
    val dispatcherProvider: DispatcherProvider
    val wireNotificationManager: WireNotificationManager
    val notificationChannelsManager: NotificationChannelsManager
    @NoSession val qualifiedIdMapper: QualifiedIdMapper
    val currentSession: CurrentSessionUseCase
    val accountSwitch: AccountSwitchUseCase
    val nomadProfilesFeatureConfig: NomadProfilesFeatureConfig
    val startPersistentWebsocketIfNecessary: StartPersistentWebsocketIfNecessaryUseCase
    val conversationAudioMessagePlayer: ConversationAudioMessagePlayer
    val networkUtil: NetworkUtil
    val callServiceManager: CallServiceManager
    val servicesManager: ServicesManager
    val callNotificationManager: CallNotificationManager
    val proximitySensorManager: ProximitySensorManager
    val wireActivityViewModel: WireActivityViewModel
    override val authenticationViewModelFactory: AuthenticationViewModelFactory

    @DependencyGraph.Factory
    fun interface Factory {
        fun create(@ApplicationContext @Provides appContext: Context): WireAppGraph
    }
}

@SessionScope
@GraphExtension(
    bindingContainers = [
        SessionModule::class,
        UiCommonModule::class,
        UseCaseModule::class,
        AppsModule::class,
        AuthenticationModule::class,
        BackupModule::class,
        CallsModule::class,
        CellsModule::class,
        ChannelsModule::class,
        ClientModule::class,
        ConnectionModule::class,
        ConversationModule::class,
        DebugModule::class,
        ImageLoadingModule::class,
        MessageModule::class,
        SearchModule::class,
        ServicesModule::class,
        TeamModule::class,
        UserModule::class,
    ]
)
interface WireSessionGraph :
    ImageAssetViewModelGraph,
    CellsViewModelGraph,
    MiscViewModelGraph,
    AuthenticationViewModelGraph,
    CallingViewModelGraph,
    DebugInfoViewModelGraph,
    HomeViewModelGraph,
    SettingsViewModelGraph,
    ConversationCoreViewModelGraph,
    ConversationDetailsViewModelGraph,
    ConversationSearchFolderViewModelGraph,
    MeetingsViewModelGraph,
    ScopedMessageViewModelGraph,
    CommonViewModelGraph {

    @com.wire.android.di.CurrentAccount
    val currentAccount: UserId

    override val viewModelScopeKey: String
        get() = currentAccount.toString()

    override val imageAssetViewModelFactory: ImageAssetViewModelFactory
        get() = ImageAssetViewModelFactory(imageLoader = imageLoaderProvider::get)

    val imageLoaderProvider: Provider<WireSessionImageLoader>

    override val cellsViewModelFactory: CellsViewModelFactory
    override val miscViewModelFactory: MiscViewModelFactory
    override val authenticationViewModelFactory: AuthenticationViewModelFactory
    override val callingViewModelFactory: CallingViewModelFactory
    override val debugInfoViewModelFactory: DebugInfoViewModelFactory
    override val homeViewModelFactory: HomeViewModelFactory
    override val settingsViewModelFactory: SettingsViewModelFactory
    override val conversationCoreViewModelFactory: ConversationCoreViewModelFactory
    override val conversationDetailsViewModelFactory: ConversationDetailsViewModelFactory
    override val conversationSearchFolderViewModelFactory: ConversationSearchFolderViewModelFactory
    override val meetingsViewModelFactory: MeetingsViewModelFactory
    override val scopedMessageViewModelFactory: ScopedMessageViewModelFactory
    override val commonViewModelFactory: CommonViewModelFactory

    @GraphExtension.Factory
    fun interface Factory {
        fun createSessionGraph(
            @com.wire.android.di.CurrentAccount @Provides currentAccount: UserId,
        ): WireSessionGraph
    }
}

fun <T : Any> metroLazy(provider: () -> T): Lazy<T> = Lazy { provider() }
