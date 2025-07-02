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
import com.wire.android.config.createServerConfigWithMdm
import com.wire.android.mdm.MdmConfigurationManager
import com.wire.android.ui.authentication.login.sso.MdmAwareSSOUrlConfigHolder
import com.wire.android.ui.authentication.login.sso.SSOUrlConfigHolder
import com.wire.kalium.logic.configuration.server.ServerConfig
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.serialization.json.Json
import javax.inject.Named
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object MdmModule {
    
    @Provides
    @Singleton
    fun provideJson(): Json {
        return Json {
            ignoreUnknownKeys = true
            encodeDefaults = true
        }
    }
    
    @Provides
    @Singleton
    fun provideMdmConfigurationManager(
        @ApplicationContext context: Context,
        json: Json
    ): MdmConfigurationManager {
        return MdmConfigurationManager(context, json)
    }
    
    @Provides
    @Named("mdmAwareServerConfig")
    fun provideMdmAwareServerConfig(
        mdmConfigurationManager: MdmConfigurationManager
    ): ServerConfig.Links {
        val mdmServerConfig = mdmConfigurationManager.getServerConfig()
        return createServerConfigWithMdm(mdmServerConfig)
    }
    
    @Provides
    @Named("mdmAwareSSOUrlConfigHolder")
    fun provideMdmAwareSSOUrlConfigHolder(
        mdmConfigurationManager: MdmConfigurationManager
    ): SSOUrlConfigHolder {
        return MdmAwareSSOUrlConfigHolder(mdmConfigurationManager)
    }
}