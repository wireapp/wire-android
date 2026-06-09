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

import android.content.Context
import com.wire.android.BuildConfig
import com.wire.android.config.ServerConfigProvider
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.emm.AndroidUserContextProvider
import com.wire.android.emm.AndroidUserContextProviderImpl
import com.wire.android.emm.ManagedConfigParser
import com.wire.android.emm.ManagedConfigParserImpl
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.emm.ManagedConfigurationsManagerImpl
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.configuration.server.ServerConfig
import dev.zacsweers.metro.BindingContainer
import dev.zacsweers.metro.Provides
import dev.zacsweers.metro.Named
import dev.zacsweers.metro.AppScope
import dev.zacsweers.metro.SingleIn

@BindingContainer
class ManagedConfigurationsModule {

    @Provides
    @SingleIn(AppScope::class)
    fun provideServerConfigProvider(): ServerConfigProvider = ServerConfigProvider()

    @Provides
    @SingleIn(AppScope::class)
    fun provideAndroidUserContextProvider(): AndroidUserContextProvider =
        AndroidUserContextProviderImpl()

    @Provides
    @SingleIn(AppScope::class)
    fun provideManagedConfigParser(
        userContextProvider: AndroidUserContextProvider
    ): ManagedConfigParser = ManagedConfigParserImpl(userContextProvider)

    @Provides
    @SingleIn(AppScope::class)
    fun provideManagedConfigurationsRepository(
        @ApplicationContext context: Context,
        dispatcherProvider: DispatcherProvider,
        serverConfigProvider: ServerConfigProvider,
        globalDataStore: GlobalDataStore,
        configParser: ManagedConfigParser
    ): ManagedConfigurationsManager {
        return ManagedConfigurationsManagerImpl(
            context,
            dispatcherProvider,
            serverConfigProvider,
            globalDataStore,
            configParser
        )
    }

    @Provides
    fun provideCurrentServerConfig(
        managedConfigurationsManager: ManagedConfigurationsManager
    ): ServerConfig.Links {
        return if (BuildConfig.EMM_SUPPORT_ENABLED) {
            // Returns the current resolved server configuration links, which could be either managed or default
            managedConfigurationsManager.currentServerConfig
        } else {
            // If EMM support is disabled, always return the static default server configuration links
            provideServerConfigProvider().getDefaultServerConfig(null)
        }
    }

    @Provides
    @Named("ssoCodeConfig")
    fun provideCurrentSSOCodeConfig(
        managedConfigurationsManager: ManagedConfigurationsManager
    ): String {
        return if (BuildConfig.EMM_SUPPORT_ENABLED) {
            // Returns the current resolved SSO code from managed configurations, or empty if none
            managedConfigurationsManager.currentSSOCodeConfig
        } else {
            // If EMM support is disabled, always return empty SSO code
            String.EMPTY
        }
    }
}
