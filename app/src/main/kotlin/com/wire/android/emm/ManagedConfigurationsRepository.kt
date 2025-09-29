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
import com.wire.android.util.dispatchers.DispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ManagedConfigurationsRepository @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: DispatcherProvider,
) {

    private val json: Json = Json { ignoreUnknownKeys = true }
    private val logger = appLogger.withTextTag(TAG)

    private val restrictionsManager: RestrictionsManager by lazy {
        context.getSystemService(Context.RESTRICTIONS_SERVICE) as RestrictionsManager
    }

    suspend fun getServerConfig(): ManagedServerConfig? = withContext(dispatchers.io()) {
        val restrictions = restrictionsManager.applicationRestrictions

        if (restrictions == null || restrictions.isEmpty) {
            return@withContext null
        }

        val serverConfigJson = getJsonRestrictionByKey<ManagedServerConfig>(
            ManagedConfigurationsKeys.DEFAULT_SERVER_URLS.asKey()
        )
        logger.d("Managed server config: $serverConfigJson")
        return@withContext serverConfigJson
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
