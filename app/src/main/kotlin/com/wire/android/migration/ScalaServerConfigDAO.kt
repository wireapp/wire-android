package com.wire.android.migration

import com.wire.kalium.logic.configuration.server.ServerConfig
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaServerConfigDAO @Inject constructor(private val scalaBackendPreferences: ScalaBackendPreferences) {

    private fun versionInfo(): VersionInfo? = scalaBackendPreferences.apiVersion?.let { Json.decodeFromString(it) }

    private fun links(): ServerConfig.Links? {
        val apiBaseUrl: String? = scalaBackendPreferences.baseUrl
        val accountsBaseUrl: String? = scalaBackendPreferences.accountsUrl
        val webSocketBaseUrl: String? = scalaBackendPreferences.websocketUrl
        val blackListUrl: String? = scalaBackendPreferences.blacklistUrl
        val teamsUrl: String? = scalaBackendPreferences.teamsUrl
        val websiteUrl: String? = scalaBackendPreferences.websiteUrl
        val title: String? = scalaBackendPreferences.environment

        return if (!apiBaseUrl.isNullOrEmpty() && !accountsBaseUrl.isNullOrEmpty() && !webSocketBaseUrl.isNullOrEmpty() &&
            !blackListUrl.isNullOrEmpty() && !teamsUrl.isNullOrEmpty() && !websiteUrl.isNullOrEmpty() && !title.isNullOrEmpty()
        ) ServerConfig.Links(apiBaseUrl, accountsBaseUrl, webSocketBaseUrl, blackListUrl, teamsUrl, websiteUrl, title, false)
        else null
    }

    val scalaServerConfig: ScalaServerConfig
        get() {
            val links: ServerConfig.Links? = links()
            val versionInfo: VersionInfo? = versionInfo()
            val customConfigUrl: String? = scalaBackendPreferences.customConfigUrl

            return when {
                links != null && versionInfo != null -> ScalaServerConfig.Full(links, versionInfo)
                links != null -> ScalaServerConfig.Links(links)
                customConfigUrl != null -> ScalaServerConfig.ConfigUrl(customConfigUrl)
                else -> ScalaServerConfig.NoData
            }
        }
}

@Serializable
data class VersionInfo(
    @SerialName("domain") val domain: String?,
    @SerialName("federation") val federation: Boolean,
    @SerialName("supported") val supported: List<Int>
)

sealed class ScalaServerConfig {
    data class Full(val links: ServerConfig.Links, val versionInfo: VersionInfo) : ScalaServerConfig()
    data class Links(val links: ServerConfig.Links) : ScalaServerConfig()
    data class ConfigUrl(val customConfigUrl: String) : ScalaServerConfig()
    object NoData : ScalaServerConfig()
}
