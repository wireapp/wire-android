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

interface ManagedConfigurationsRepository {
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
     */
    suspend fun refreshSSOCodeConfig(): String
}

internal class ManagedConfigurationsRepositoryImpl(
    private val context: Context,
    private val dispatchers: DispatcherProvider,
    private val serverConfigProvider: ServerConfigProvider,
) : ManagedConfigurationsRepository {

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

    override suspend fun refreshSSOCodeConfig(): String {
        val managedSSOCodeConfig = getSSOCodeConfig()
        val ssoCode = managedSSOCodeConfig?.ssoCode.orEmpty()
        _currentSSOCodeConfig.set(ssoCode)
        logger.i("SSO code config refreshed: $ssoCode")
        return ssoCode
    }

    private suspend fun getSSOCodeConfig(): ManagedSSOCodeConfig? = withContext(dispatchers.io()) {
        val restrictions = restrictionsManager.applicationRestrictions
        if (restrictions == null || restrictions.isEmpty) {
            logger.i("No application restrictions found")
            return@withContext null
        }

        val ssoCode = getJsonRestrictionByKey<ManagedSSOCodeConfig>(
            ManagedConfigurationsKeys.SSO_CODE.asKey()
        ) ?: run {
            logger.w("No managed SSO code found in restrictions")
            return@withContext null
        }

        return@withContext when {
            ssoCode.isValid -> {
                logger.i("Managed SSO code found: $ssoCode")
                ssoCode
            }

            else -> {
                logger.w("Managed SSO code is not valid: $ssoCode")
                null
            }
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
                null
            }
        }

    companion object {
        const val TAG = "ManagedConfigurationsRepository"
    }
}
