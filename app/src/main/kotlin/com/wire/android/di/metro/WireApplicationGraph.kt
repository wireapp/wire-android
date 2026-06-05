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
import com.wire.android.WireApplication
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.AppModule
import com.wire.android.di.CoreLogicModule
import com.wire.android.di.CoroutinesDispatchersModule
import com.wire.android.di.CoroutinesScopesModule
import com.wire.android.di.ImageLoadingModule
import com.wire.android.di.KaliumConfigsModule
import com.wire.android.di.LogWriterModule
import com.wire.android.di.ManagedConfigurationsModule
import com.wire.android.di.SessionModule
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
import com.wire.android.feature.aiassistant.di.AiAssistantModule
import com.wire.android.notification.broadcastreceivers.EndOngoingCallReceiver
import com.wire.android.notification.broadcastreceivers.IncomingCallActionReceiver
import com.wire.android.notification.broadcastreceivers.NomadLogoutReceiver
import com.wire.android.notification.broadcastreceivers.NotificationReplyReceiver
import com.wire.android.notification.broadcastreceivers.PlayPauseAudioMessageReceiver
import com.wire.android.notification.broadcastreceivers.StopAudioMessageReceiver
import com.wire.android.services.CallService
import com.wire.android.services.PersistentWebSocketService
import com.wire.android.services.PlayingAudioMessageService
import com.wire.android.ui.AppLockActivity
import com.wire.android.ui.WireActivity
import com.wire.android.ui.WireActivityViewModel
import com.wire.android.ui.debug.StartServiceReceiver
import com.wire.android.ui.calling.CallActivity
import com.wire.android.util.NetworkUtil
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.android.workmanager.WireWorkerFactory
import com.wire.kalium.logic.CoreLogic
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.DependencyGraph
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.createGraphFactory

@DependencyGraph(
    scope = AppScope::class,
    bindingContainers = [
        AppModule::class,
        CoreLogicModule::class,
        SessionModule::class,
        UseCaseModule::class,
        ManagedConfigurationsModule::class,
        KaliumConfigsModule::class,
        LogWriterModule::class,
        CoroutinesScopesModule::class,
        CoroutinesDispatchersModule::class,
        ImageLoadingModule::class,
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
        MessageModule::class,
        SearchModule::class,
        ServicesModule::class,
        TeamModule::class,
        UserModule::class,
        AiAssistantModule::class,
    ]
)
@Suppress("TooManyFunctions")
interface WireApplicationGraph {
    val wireWorkerFactory: WireWorkerFactory
    val wireActivityViewModel: WireActivityViewModel
    val authenticationViewModelGraph: AppAuthenticationViewModelGraph
    val imageAssetViewModelGraph: AppImageAssetViewModelGraph
    val sessionViewModelGraph: AppSessionViewModelGraph

    @get:KaliumCoreLogic
    val coreLogic: CoreLogic
    val networkUtil: NetworkUtil
    val dispatcherProvider: DispatcherProvider

    fun inject(application: WireApplication)
    fun inject(activity: WireActivity)
    fun inject(activity: AppLockActivity)
    fun inject(activity: CallActivity)
    fun inject(service: PersistentWebSocketService)
    fun inject(service: CallService)
    fun inject(service: PlayingAudioMessageService)
    fun inject(receiver: StartServiceReceiver)
    fun inject(receiver: IncomingCallActionReceiver)
    fun inject(receiver: NomadLogoutReceiver)
    fun inject(receiver: EndOngoingCallReceiver)
    fun inject(receiver: StopAudioMessageReceiver)
    fun inject(receiver: NotificationReplyReceiver)
    fun inject(receiver: PlayPauseAudioMessageReceiver)

    @DependencyGraph.Factory
    interface Factory {
        fun create(@Provides @com.wire.android.di.ApplicationContext context: Context): WireApplicationGraph
    }
}

fun createWireApplicationGraph(context: Context): WireApplicationGraph =
    createGraphFactory<WireApplicationGraph.Factory>().create(context.applicationContext)

val Context.wireApplicationGraph: WireApplicationGraph
    get() = (applicationContext as WireApplication).appGraph
