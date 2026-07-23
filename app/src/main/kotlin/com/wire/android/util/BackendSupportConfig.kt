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
import com.wire.android.datastore.GlobalDataStore
import com.wire.kalium.logic.configuration.server.ServerConfig

object BackendSupportConfig {

    @Volatile
    private var currentBackendApiUrl: String? = null

    fun setCurrentBackend(serverLinks: ServerConfig.Links) {
        currentBackendApiUrl = serverLinks.api.takeIf { it.isNotBlank() }
    }

    suspend fun storeFromServerLinks(
        globalDataStore: GlobalDataStore,
        serverLinks: ServerConfig.Links
    ) {
        setCurrentBackend(serverLinks)
        globalDataStore.setBackendSupportEmail(
            backendApiUrl = serverLinks.api,
            supportEmail = serverLinks.supportEmail
        )
    }

    suspend fun resolveEmail(context: Context, staticSupportEmail: String): String? {
        val staticEmail = staticSupportEmail.trim()
        return when {
            staticEmail.isNotBlank() -> staticEmail
            currentBackendApiUrl != null -> currentBackendApiUrl?.let {
                GlobalDataStore(context.applicationContext).getBackendSupportEmail(it)
            }
            else -> null
        }
    }

    fun supportPageIntent(): Intent? =
        SupportUrlResolver.resolveUrl("")?.let {
            Intent(Intent.ACTION_VIEW, Uri.parse(it))
        }
}
