/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.net.HttpURLConnection
import java.net.URL

object BackendSupportConfig {

    private const val CONNECT_TIMEOUT_MILLIS = 5_000
    private const val READ_TIMEOUT_MILLIS = 5_000

    private val json = Json { ignoreUnknownKeys = true }

    @Volatile
    private var currentBackendApiUrl: String? = null

    fun setCurrentBackend(serverLinks: ServerConfig.Links) {
        currentBackendApiUrl = serverLinks.api.takeIf { it.isNotBlank() }
    }

    suspend fun storeFromConfigUrl(
        globalDataStore: GlobalDataStore,
        serverLinks: ServerConfig.Links,
        configUrl: String
    ) {
        setCurrentBackend(serverLinks)
        globalDataStore.setBackendSupportEmail(
            backendApiUrl = serverLinks.api,
            supportEmail = fetchSupportEmail(configUrl)
        )
    }

    suspend fun resolveEmail(context: Context, staticSupportEmail: String): String? {
        val staticEmail = staticSupportEmail.trim()
        if (staticEmail.isNotBlank()) return staticEmail

        val backendApiUrl = currentBackendApiUrl ?: return null
        return GlobalDataStore(context.applicationContext).getBackendSupportEmail(backendApiUrl)
    }

    fun supportPageIntent(): Intent? =
        CustomTabsHelper.resolveUrl("")?.let {
            Intent(Intent.ACTION_VIEW, Uri.parse(it))
        }

    @Suppress("TooGenericExceptionCaught")
    private suspend fun fetchSupportEmail(configUrl: String): String? = withContext(Dispatchers.IO) {
        try {
            val connection = URL(configUrl).openConnection() as HttpURLConnection
            connection.connectTimeout = CONNECT_TIMEOUT_MILLIS
            connection.readTimeout = READ_TIMEOUT_MILLIS
            try {
                connection.inputStream.bufferedReader().use { reader ->
                    json.decodeFromString(SupportConfig.serializer(), reader.readText()).supportEmail
                        ?.trim()
                        ?.takeIf { it.isNotBlank() }
                }
            } finally {
                connection.disconnect()
            }
        } catch (exception: Exception) {
            appLogger.w("Failed to read backend support email from config", exception)
            null
        }
    }

    @Serializable
    private data class SupportConfig(
        @SerialName("supportEmail")
        val supportEmail: String? = null
    )
}
