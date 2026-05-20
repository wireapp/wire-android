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
package com.wire.android.notification.broadcastreceivers

import android.content.Context
import com.wire.android.config.NomadProfilesFeatureConfig
import com.wire.android.di.ApplicationScope
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.di.metro.createWireMetroGraph
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.StartPersistentWebsocketIfNecessaryUseCase
import com.wire.android.media.audiomessage.ConversationAudioMessagePlayer
import com.wire.android.notification.CallNotificationManager
import com.wire.android.util.SwitchAccountObserver
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import kotlinx.coroutines.CoroutineScope

interface BroadcastReceiverDependencies {
    @KaliumCoreLogic
    fun coreLogic(): CoreLogic

    fun dispatcherProvider(): DispatcherProvider

    @NoSession
    fun qualifiedIdMapper(): QualifiedIdMapper

    @ApplicationScope
    fun coroutineScope(): CoroutineScope

    fun callNotificationManager(): CallNotificationManager

    fun conversationAudioMessagePlayer(): ConversationAudioMessagePlayer

    fun currentSession(): CurrentSessionUseCase

    fun accountSwitch(): AccountSwitchUseCase

    fun switchAccountObserver(): SwitchAccountObserver

    fun nomadProfilesFeatureConfig(): NomadProfilesFeatureConfig

    fun startPersistentWebsocketIfNecessary(): StartPersistentWebsocketIfNecessaryUseCase
}

val Context.broadcastReceiverDependencies: BroadcastReceiverDependencies
    get() {
        val graph = createWireMetroGraph(applicationContext)
        return object : BroadcastReceiverDependencies {
            override fun coreLogic(): CoreLogic = graph.coreLogic

            override fun dispatcherProvider(): DispatcherProvider = graph.dispatcherProvider

            override fun qualifiedIdMapper(): QualifiedIdMapper = QualifiedIdMapper(null)

            override fun coroutineScope(): CoroutineScope = graph.applicationScope

            override fun callNotificationManager(): CallNotificationManager = graph.callNotificationManager

            override fun conversationAudioMessagePlayer(): ConversationAudioMessagePlayer =
                graph.conversationAudioMessagePlayer

            override fun currentSession(): CurrentSessionUseCase = graph.currentSession

            override fun accountSwitch(): AccountSwitchUseCase = graph.accountSwitch

            override fun switchAccountObserver(): SwitchAccountObserver = graph.switchAccountObserver

            override fun nomadProfilesFeatureConfig(): NomadProfilesFeatureConfig =
                graph.nomadProfilesFeatureConfig

            override fun startPersistentWebsocketIfNecessary(): StartPersistentWebsocketIfNecessaryUseCase =
                graph.startPersistentWebsocketIfNecessary
        }
    }
