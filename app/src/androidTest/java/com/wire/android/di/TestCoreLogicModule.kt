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
package com.wire.android.di

import android.content.Context
import androidx.work.Configuration
import androidx.work.WorkManager
import androidx.work.testing.SynchronousExecutor
import androidx.work.testing.WorkManagerTestInitHelper
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.util.UserAgentProvider
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.server.ServerConfigForAccountUseCase
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.session.UpdateCurrentSessionUseCase
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import com.wire.kalium.network.NetworkStateObserver
import dagger.Module
import dagger.Provides
import dagger.hilt.android.components.ViewModelComponent
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [CoreLogicModule::class]
)
class TestCoreLogicModule {
    @KaliumCoreLogic
    @Provides
    fun provideCoreLogic(
        @ApplicationContext context: Context,
        kaliumConfigs: KaliumConfigs,
        userAgentProvider: UserAgentProvider
    ): CoreLogic = CoreLogic(
        "TestUserAgent",
        context,
        "accounts",
        kaliumConfigs,
    )

    @Singleton
    @Provides
    fun provideNetworkStateObserver(@KaliumCoreLogic coreLogic: CoreLogic): NetworkStateObserver =
        coreLogic.networkStateObserver

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
    fun provideServerConfigForAccountUseCase(@KaliumCoreLogic coreLogic: CoreLogic): ServerConfigForAccountUseCase =
        coreLogic.getGlobalScope().serverConfigForAccounts

    @NoSession
    @Singleton
    @Provides
    fun provideNoSessionQualifiedIdMapper(): QualifiedIdMapper = QualifiedIdMapperImpl(null)

    @Singleton
    @Provides
    fun provideWorkManager(@ApplicationContext applicationContext: Context): WorkManager {
        val config = Configuration.Builder()
            .setExecutor(SynchronousExecutor())
            .build()
        WorkManagerTestInitHelper.initializeTestWorkManager(applicationContext, config)
        return WorkManager.getInstance(applicationContext)
    }
}

@Module
@TestInstallIn(
    components = [ViewModelComponent::class],
    replaces = [SessionModule::class]
)
class TestSessionModule {
    // TODO: can be improved by caching the current session in kalium or changing the scope to ActivityRetainedScoped
    @CurrentAccount
    @ViewModelScoped
    @Provides
    fun provideCurrentSession(@KaliumCoreLogic coreLogic: CoreLogic): UserId {
        return UserId("user", "domain")
    }

    @ViewModelScoped
    @Provides
    fun provideCurrentAccountUserDataStore(@CurrentAccount currentAccount: UserId, userDataStoreProvider: UserDataStoreProvider) =
        userDataStoreProvider.getOrCreate(currentAccount)
}
