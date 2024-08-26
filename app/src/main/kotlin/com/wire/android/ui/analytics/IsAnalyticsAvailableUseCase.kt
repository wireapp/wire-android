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
package com.wire.android.ui.analytics

import com.wire.android.datastore.UserDataStore
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.first
import javax.inject.Inject

/**
 * UseCase that determines if Analytics is available for current Build and user.
 * Use it for checking if Analytics UI (e.x. asking user for some feedback that will be sent to Analytics) should be shown to user or not.
 */
@ViewModelScoped
class IsAnalyticsAvailableUseCase @Inject constructor(
    private val analyticsEnabled: AnalyticsConfiguration,
    private val dataStore: UserDataStore,
    private val selfServerConfig: SelfServerConfigUseCase
) {

    suspend operator fun invoke(): Boolean {
        val isAnalyticsUsageEnabled = dataStore.isAnonymousUsageDataEnabled().first()
        val isAnalyticsConfigurationEnabled = analyticsEnabled is AnalyticsConfiguration.Enabled
        val isProdBackend = when (val serverConfig = selfServerConfig()) {
            is SelfServerConfigUseCase.Result.Success ->
                serverConfig.serverLinks.links.api == ServerConfig.PRODUCTION.api
                        || serverConfig.serverLinks.links.api == ServerConfig.STAGING.api

            is SelfServerConfigUseCase.Result.Failure -> false
        }

        return isProdBackend && isAnalyticsUsageEnabled && isAnalyticsConfigurationEnabled
    }
}