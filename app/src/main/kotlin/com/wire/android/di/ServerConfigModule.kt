/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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

import com.wire.android.config.ServerConfigProvider
import com.wire.android.emm.ManagedConfigurationsRepository
import com.wire.kalium.logic.configuration.server.ServerConfig
import dagger.Module
import dagger.Provides
import dagger.Reusable
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
class ServerConfigModule {

    @Provides
    @Singleton
    fun provideServerConfigProvider(): ServerConfigProvider = ServerConfigProvider()

    @Provides
    @Reusable
    fun provideCurrentServerConfig(
        serverConfigProvider: ServerConfigProvider,
        managedConfigurationsRepository: ManagedConfigurationsRepository
    ): ServerConfig.Links {
        val managedServerConfig = runBlocking(Dispatchers.IO) {
            managedConfigurationsRepository.getServerConfig()
        }
        return serverConfigProvider.getDefaultServerConfig(managedServerConfig)
    }
}
