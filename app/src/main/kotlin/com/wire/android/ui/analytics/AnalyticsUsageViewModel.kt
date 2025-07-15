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

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.android.datastore.UserDataStore
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import dagger.Lazy
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AnalyticsUsageViewModel @Inject constructor(
    private val analyticsEnabled: AnalyticsConfiguration,
    private val dataStore: Lazy<UserDataStore>,
    private val selfServerConfig: Lazy<SelfServerConfigUseCase>,
) : ViewModel() {

    var state by mutableStateOf(AnalyticsUsageState())

    init {
        viewModelScope.launch {
            val isDialogSeen = dataStore.get().isAnalyticsDialogSeen().first()
            val isAnalyticsUsageEnabled = dataStore.get().isAnonymousUsageDataEnabled().first()
            val isAnalyticsConfigurationEnabled = analyticsEnabled is AnalyticsConfiguration.Enabled
            val isValidBackend = when (val serverConfig = selfServerConfig.get().invoke()) {
                is SelfServerConfigUseCase.Result.Success ->
                    serverConfig.serverLinks.links.api == ServerConfig.PRODUCTION.api
                            || serverConfig.serverLinks.links.api == ServerConfig.STAGING.api
                is SelfServerConfigUseCase.Result.Failure -> false
            }

            val shouldShowDialog = isValidBackend && !isAnalyticsUsageEnabled && isAnalyticsConfigurationEnabled && !isDialogSeen

            state = state.copy(
                shouldDisplayDialog = shouldShowDialog
            )
        }
    }

    fun agreeAnalyticsUsage() {
        viewModelScope.launch {
            dataStore.get().setIsAnonymousAnalyticsEnabled(enabled = true)
            dataStore.get().setIsAnalyticsDialogSeen()

            hideDialog()
        }
    }

    fun declineAnalyticsUsage() {
        viewModelScope.launch {
            dataStore.get().setIsAnonymousAnalyticsEnabled(enabled = false)
            dataStore.get().setIsAnalyticsDialogSeen()

            hideDialog()
        }
    }

    private fun hideDialog() {
        state = state.copy(
            shouldDisplayDialog = false
        )
    }
}
