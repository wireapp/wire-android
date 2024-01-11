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

package com.wire.android.migration.preference

import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.serialization.SerializationException
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaServerConfigDAO @Inject constructor(private val scalaBackendPreferences: ScalaBackendPreferences) {
    private fun versionInfo(): ServerConfig.VersionInfo? = scalaBackendPreferences.apiVersion?.let {
        try {
            Json.decodeFromString(it)
        } catch (e: SerializationException) {
            null
        }
    }

    private fun links(): ServerConfig.Links? {
        val apiBaseUrl: String? = scalaBackendPreferences.baseUrl
        val accountsBaseUrl: String? = scalaBackendPreferences.accountsUrl
        val webSocketBaseUrl: String? = scalaBackendPreferences.websocketUrl
        val blackListUrl: String? = scalaBackendPreferences.blacklistUrl
        val teamsUrl: String? = scalaBackendPreferences.teamsUrl
        val websiteUrl: String? = scalaBackendPreferences.websiteUrl
        val title: String? = scalaBackendPreferences.environment

        @Suppress("ComplexCondition")
        return if (!apiBaseUrl.isNullOrEmpty() && !accountsBaseUrl.isNullOrEmpty() && !webSocketBaseUrl.isNullOrEmpty() &&
            !blackListUrl.isNullOrEmpty() && !teamsUrl.isNullOrEmpty() && !websiteUrl.isNullOrEmpty() && !title.isNullOrEmpty()
        ) ServerConfig.Links(apiBaseUrl, accountsBaseUrl, webSocketBaseUrl, blackListUrl, teamsUrl, websiteUrl, title, false, null)
        else null
    }

    val scalaServerConfig: ScalaServerConfig
        get() {
            val links: ServerConfig.Links? = links()
            val versionInfo: ServerConfig.VersionInfo? = versionInfo()
            val customConfigUrl: String? = scalaBackendPreferences.customConfigUrl

            return when {
                links != null && versionInfo != null -> ScalaServerConfig.Full(links, versionInfo)
                links != null -> ScalaServerConfig.Links(links)
                customConfigUrl != null -> ScalaServerConfig.ConfigUrl(customConfigUrl)
                else -> ScalaServerConfig.NoData
            }
        }
}

sealed class ScalaServerConfig {
    data class Full(val links: ServerConfig.Links, val versionInfo: ServerConfig.VersionInfo) : ScalaServerConfig()
    data class Links(val links: ServerConfig.Links) : ScalaServerConfig()
    data class ConfigUrl(val customConfigUrl: String) : ScalaServerConfig()
    object NoData : ScalaServerConfig()
}
