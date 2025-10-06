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
package com.wire.android.emm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.wire.android.appLogger
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagedConfigurationsReceiver @Inject constructor(
    private val managedConfigurationsManager: ManagedConfigurationsManager,
    private val managedConfigurationsReporter: ManagedConfigurationsReporter,
    dispatcher: DispatcherProvider
) : BroadcastReceiver() {

    private val logger = appLogger.withTextTag(TAG)
    private val scope by lazy {
        CoroutineScope(SupervisorJob() + dispatcher.io())
    }

    override fun onReceive(context: Context, intent: Intent) {
        when (intent.action) {
            Intent.ACTION_APPLICATION_RESTRICTIONS_CHANGED -> {
                scope.launch {
                    logger.i("Received intent to refresh managed configurations")
                    updateServerConfig()
                    updateSSOCodeConfig()
                }
            }

            else -> logger.i("Received unexpected intent action: ${intent.action}")
        }
    }

    private suspend fun updateServerConfig() {
        when (val result = managedConfigurationsManager.refreshServerConfig()) {
            is ServerConfigResult.Failure -> managedConfigurationsReporter.reportErrorState(
                key = ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey(),
                message = result.reason
            )

            is ServerConfigResult.Empty -> managedConfigurationsReporter.reportAppliedState(
                key = ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey(),
                message = "Managed configuration cleared",
                data = String.EMPTY
            )

            // Just the title will be output, the docs state limits for these fields.
            // See: https://developer.android.com/work/app-feedback/overview#keyed-app-state-components
            is ServerConfigResult.Success -> {
                managedConfigurationsReporter.reportAppliedState(
                    key = ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey(),
                    message = "Managed configuration applied",
                    data = result.config.title
                )
            }
        }

    }

    private suspend fun updateSSOCodeConfig() {
        when (val result = managedConfigurationsManager.refreshSSOCodeConfig()) {
            is SSOCodeConfigResult.Failure -> managedConfigurationsReporter.reportErrorState(
                key = ManagedConfigurationsKeys.SSO_CODE.asKey(),
                message = result.reason
            )

            is SSOCodeConfigResult.Empty -> managedConfigurationsReporter.reportAppliedState(
                key = ManagedConfigurationsKeys.SSO_CODE.asKey(),
                message = "Managed configuration cleared",
                data = String.EMPTY
            )

            is SSOCodeConfigResult.Success -> {
                managedConfigurationsReporter.reportAppliedState(
                    key = ManagedConfigurationsKeys.SSO_CODE.asKey(),
                    message = "Managed configuration applied",
                    data = result.config.ssoCode
                )
            }
        }
    }

    companion object {
        private const val TAG = "ManagedConfigurationsReceiver"
    }
}
