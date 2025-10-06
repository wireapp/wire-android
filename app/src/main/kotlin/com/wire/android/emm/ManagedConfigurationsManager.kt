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

import android.content.Context
import android.content.RestrictionsManager
import com.wire.android.appLogger
import com.wire.android.config.ServerConfigProvider
import com.wire.android.util.EMPTY
import com.wire.android.util.dispatchers.DispatcherProvider
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import java.util.concurrent.atomic.AtomicReference

interface ManagedConfigurationsManager {
    /**
     * Current server config that ViewModels can access.
     * This is thread-safe and will be updated when app resumes or broadcast receiver is triggered.
     *
     * @see refreshServerConfig
     */
    val currentServerConfig: ServerConfig.Links

    /**
     * Current SSO code if provided via managed configurations, empty string otherwise.
     */

    val currentSSOCodeConfig: String

    /**
     * Initialize the server config on first access or when explicitly called.
     * This should be called when the app starts, resumes, or when broadcast receiver triggers.
     */
    suspend fun refreshServerConfig(): ServerConfig.Links

    /**
     * Initialize the SSO code config on first access or when explicitly called.
     * This should be called when the app starts, resumes, or when broadcast receiver triggers.
     *
     * The result indicates whether a valid config was found or if there was an error.
     * Nevertheless, the config is either updated or defaulted to empty.
     *
     * @return result of the update attempt, either success with the config,
     * empty if no config found or cleared, or failure with reason.
     */
    suspend fun refreshSSOCodeConfig(): SSOCodeConfigResult
}

internal class ManagedConfigurationsManagerImpl(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val serverConfigProvider: ServerConfigProvider,
) : ManagedConfigurationsManager {

    private val json: Json = Json { ignoreUnknownKeys = true }
    private val logger = appLogger.withTextTag(TAG)
    private val restrictionsManager: RestrictionsManager by lazy {
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    }

    private val _currentServerConfig = AtomicReference<ServerConfig.Links?>(null)
    private val _currentSSOCodeConfig = AtomicReference(String.EMPTY)

    override val currentServerConfig: ServerConfig.Links
        get() = _currentServerConfig.get() ?: serverConfigProvider.getDefaultServerConfig()

    override val currentSSOCodeConfig: String
        get() = _currentSSOCodeConfig.get()

    override suspend fun refreshServerConfig(): ServerConfig.Links = withContext(dispatchers.io()) {
        val managedServerConfig = getServerConfig()
        val serverConfig = serverConfigProvider.getDefaultServerConfig(managedServerConfig)
        _currentServerConfig.set(serverConfig)
        logger.i("Server config refreshed: $serverConfig")
        serverConfig
    }

    override suspend fun refreshSSOCodeConfig(): SSOCodeConfigResult =
        withContext(dispatchers.io()) {
            val managedSSOCodeConfig = getSSOCodeConfig()
            val ssoCode: String = when (managedSSOCodeConfig) {
                is SSOCodeConfigResult.Empty -> String.EMPTY
                is SSOCodeConfigResult.Failure -> String.EMPTY
                is SSOCodeConfigResult.Success -> managedSSOCodeConfig.config.ssoCode
            }

            _currentSSOCodeConfig.set(ssoCode)
            logger.i("SSO code config refreshed to: $ssoCode")
            managedSSOCodeConfig
        }

    private suspend fun getSSOCodeConfig(): SSOCodeConfigResult =
        withContext(dispatchers.io()) {
            val restrictions = restrictionsManager.applicationRestrictions
            if (restrictions == null || restrictions.isEmpty) {
                logger.i("No application restrictions found")
                return@withContext SSOCodeConfigResult.Empty
            }

            try {
                val ssoCode = getJsonRestrictionByKey<ManagedSSOCodeConfig>(
                    ManagedConfigurationsKeys.SSO_CODE.asKey()
                )

                if (ssoCode?.isValid == true) {
                    logger.i("Managed SSO code found: $ssoCode")
                    SSOCodeConfigResult.Success(ssoCode)
                } else {
                    logger.w("Managed SSO code is not valid: $ssoCode")
                    SSOCodeConfigResult.Failure("Managed SSO code is not a valid config. Check the format.")
                }
            } catch (e: InvalidManagedConfig) {
                logger.w("Invalid managed SSO code config: ${e.reason}")
                SSOCodeConfigResult.Failure(e.reason)
            }
        }

    private suspend fun getServerConfig(): ManagedServerConfig? = withContext(dispatchers.io()) {
        val restrictions = restrictionsManager.applicationRestrictions
        if (restrictions == null || restrictions.isEmpty) {
            logger.i("No application restrictions found")
            return@withContext null
        }

        val managedServerConfig = getJsonRestrictionByKey<ManagedServerConfig>(
            ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey()
        ) ?: run {
            logger.w("No managed server config found in restrictions")
            return@withContext null
        }

        return@withContext when {
            managedServerConfig.endpoints.isValid -> {
                logger.i("Managed server config found: $managedServerConfig")
                managedServerConfig
            }

            else -> {
                logger.w("Managed server config is not valid: $managedServerConfig")
                null
            }
        }
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <reified T> getJsonRestrictionByKey(key: String): T? =
        restrictionsManager.applicationRestrictions.getString(key)?.let {
            try {
                json.decodeFromString<T>(it)
            } catch (e: Exception) {
                throw InvalidManagedConfig("Failed to parse managed config for key $key: ${e.message}")
            }
        }

    companion object {
        private const val TAG = "ManagedConfigurationsManager"
    }
}

data class InvalidManagedConfig(val reason: String) : Throwable(reason)

sealed interface SSOCodeConfigResult {
    data class Success(val config: ManagedSSOCodeConfig) : SSOCodeConfigResult
    data object Empty : SSOCodeConfigResult
    data class Failure(val reason: String) : SSOCodeConfigResult
}

sealed interface ServerConfigResult {
    data class Success(val config: ManagedServerConfig) : ServerConfigResult
    data object Empty : ServerConfigResult
    data class Failure(val reason: String) : ServerConfigResult
}
