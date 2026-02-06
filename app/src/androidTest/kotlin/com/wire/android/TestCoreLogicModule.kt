/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

package com.wire.android

import android.content.Context
import androidx.work.WorkManager
import com.wire.android.di.CoreLogicModule
import com.wire.android.di.DefaultWebSocketEnabledByDefault
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.di.NoSession
import com.wire.android.util.UserAgentProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.feature.asset.AudioNormalizedLoudnessBuilder
import com.wire.kalium.logic.feature.server.ServerConfigForAccountUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.mocks.requests.ClientRequests
import com.wire.kalium.mocks.requests.FeatureConfigRequests
import com.wire.kalium.mocks.requests.LoginRequests
import com.wire.kalium.mocks.requests.NotificationRequests
import com.wire.kalium.network.NetworkStateObserver
import com.wire.kalium.network.utils.TestRequestHandler
import dagger.Module
import dagger.Provides
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreLogicModule::class],
)
class TestCoreLogicModule {

    @KaliumCoreLogic
    @Singleton
    @Provides
    fun provideCoreLogic(
        @ApplicationContext context: Context,
        kaliumConfigs: KaliumConfigs,
        userAgentProvider: UserAgentProvider
    ): CoreLogic {
        val rootPath = context.getDir("accounts", Context.MODE_PRIVATE).path
        val mockedRequests = mutableListOf<TestRequestHandler>().apply {
            addAll(LoginRequests.loginRequestResponseSuccess)
            addAll(ClientRequests.clientRequestResponseSuccess)
            addAll(FeatureConfigRequests.responseSuccess)
            addAll(NotificationRequests.notificationsRequestResponseSuccess)
        }

        return CoreLogic(
            userAgent = userAgentProvider.defaultUserAgent,
            appContext = context,
            rootPath = rootPath,
            kaliumConfigs = kaliumConfigs.copy(
                mockedRequests = mockedRequests,
                mockNetworkStateObserver = TestNetworkStateObserver.DEFAULT_TEST_NETWORK_STATE_OBSERVER
            )
        )
    }

    @Singleton
    @Provides
    fun provideNetworkStateObserver(): NetworkStateObserver = TestNetworkStateObserver()

    @Provides
    fun provideCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().session.currentSession

    @Provides
    fun deleteSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic) =
        coreLogic.getGlobalScope().deleteSession

    @Provides
    fun provideUpdateCurrentSessionUseCase(@KaliumCoreLogic coreLogic: CoreLogic): UpdateCurrentSessionUseCase =
        coreLogic.getGlobalScope().session.updateCurrentSession

    @Provides
    fun provideGetAllSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): GetSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessions

    @Provides
    fun provideObserveAllSessionsUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ObserveSessionsUseCase =
        coreLogic.getGlobalScope().session.allSessionsFlow

    @Provides
    fun provideServerConfigForAccountUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ServerConfigForAccountUseCase =
        coreLogic.getGlobalScope().serverConfigForAccounts

    @NoSession
    @Singleton
    @Provides
    fun provideNoSessionQualifiedIdMapper(): QualifiedIdMapper = QualifiedIdMapper(null)

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext applicationContext: Context) = WorkManager.getInstance(applicationContext)

    @Provides
    fun provideAudioNormalizedLoudnessBuilder(@KaliumCoreLogic coreLogic: CoreLogic): AudioNormalizedLoudnessBuilder =
        coreLogic.audioNormalizedLoudnessBuilder

    @DefaultWebSocketEnabledByDefault
    @Provides
    fun provideDefaultWebSocketEnabledByDefault(): Boolean = true
}
