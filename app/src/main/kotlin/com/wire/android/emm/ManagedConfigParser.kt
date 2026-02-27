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

import com.wire.android.appLogger
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.decodeFromJsonElement
import kotlinx.serialization.json.jsonObject

/**
 * Parser for MDM managed configurations that supports both unified and context-mapped formats.
 *
 * **Unified Format (backward compatible):**
 * ```json
 * {
 *   "title": "Enterprise Server",
 *   "endpoints": { ... }
 * }
 * ```
 *
 * **Context-Mapped Format (multi-app support):**
 * ```json
 * {
 *   "0": { "title": "Secure Server", "endpoints": { ... } },
 *   "default": { "title": "General Server", "endpoints": { ... } }
 * }
 * ```
 *
 * The parser automatically detects the format and resolves the appropriate configuration
 * based on the current Android user context.
 */
interface ManagedConfigParser {
    /**
     * Parses server configuration from raw JSON string.
     *
     * @param rawJson The raw JSON string from MDM restrictions
     * @return Parsed [ManagedServerConfig] or null if parsing fails or no config found
     * @throws InvalidManagedConfig if JSON is malformed
     */
    fun parseServerConfig(rawJson: String): ManagedServerConfig?

    /**
     * Parses SSO code configuration from raw JSON string.
     *
     * @param rawJson The raw JSON string from MDM restrictions
     * @return Parsed [ManagedSSOCodeConfig] or null if parsing fails or no config found
     * @throws InvalidManagedConfig if JSON is malformed
     */
    fun parseSSOCodeConfig(rawJson: String): ManagedSSOCodeConfig?
}

internal class ManagedConfigParserImpl(
    private val userContextProvider: AndroidUserContextProvider
) : ManagedConfigParser {

    private val json: Json = Json { ignoreUnknownKeys = true }
    private val logger = appLogger.withTextTag(TAG)

    override fun parseServerConfig(rawJson: String): ManagedServerConfig? {
        return parseConfig(
            rawJson = rawJson,
            configType = "server",
            isUnifiedFormat = ::isUnifiedServerFormat,
            parseUnified = { json.decodeFromString<ManagedServerConfig>(rawJson) },
            parseFromObject = { json.decodeFromJsonElement<ManagedServerConfig>(it) }
        )
    }

    override fun parseSSOCodeConfig(rawJson: String): ManagedSSOCodeConfig? {
        return parseConfig(
            rawJson = rawJson,
            configType = "SSO",
            isUnifiedFormat = ::isUnifiedSSOFormat,
            parseUnified = { json.decodeFromString<ManagedSSOCodeConfig>(rawJson) },
            parseFromObject = { json.decodeFromJsonElement<ManagedSSOCodeConfig>(it) }
        )
    }

    @Suppress("TooGenericExceptionCaught")
    private inline fun <T> parseConfig(
        rawJson: String,
        configType: String,
        isUnifiedFormat: (JsonObject) -> Boolean,
        parseUnified: () -> T,
        parseFromObject: (JsonObject) -> T
    ): T? {
        return try {
            val jsonObject = json.parseToJsonElement(rawJson).jsonObject

            if (isUnifiedFormat(jsonObject)) {
                logger.i("Detected unified $configType config format")
                parseUnified()
            } else {
                logger.i("Detected context-mapped $configType config format")
                resolveContextMappedConfig(jsonObject, parseFromObject)
            }
        } catch (e: Exception) {
            throw InvalidManagedConfig("Failed to parse managed $configType config: ${e.message}")
        }
    }

    private inline fun <T> resolveContextMappedConfig(
        jsonObject: JsonObject,
        parseFromObject: (JsonObject) -> T
    ): T? {
        val userIdKey = userContextProvider.getCurrentUserIdKey()
        logger.i("Resolving context-mapped config for user ID key: $userIdKey")

        // Try to find config by user ID key
        val configObject = jsonObject[userIdKey]?.jsonObject
            ?: jsonObject[AndroidUserContextProvider.DEFAULT_KEY]?.jsonObject

        return if (configObject != null) {
            val resolvedKey = if (jsonObject.containsKey(userIdKey)) userIdKey else AndroidUserContextProvider.DEFAULT_KEY
            logger.i("Resolved config using key: $resolvedKey")
            parseFromObject(configObject)
        } else {
            logger.w("No config found for user ID key '$userIdKey' and no '${AndroidUserContextProvider.DEFAULT_KEY}' fallback")
            null
        }
    }

    /**
     * Unified server format has "endpoints" and "title" at the root level.
     */
    private fun isUnifiedServerFormat(jsonObject: JsonObject): Boolean =
        jsonObject.containsKey(KEY_ENDPOINTS) && jsonObject.containsKey(KEY_TITLE)

    /**
     * Unified SSO format has "sso_code" at the root level.
     */
    private fun isUnifiedSSOFormat(jsonObject: JsonObject): Boolean =
        jsonObject.containsKey(KEY_SSO_CODE)

    companion object {
        private const val TAG = "ManagedConfigParser"
        private const val KEY_ENDPOINTS = "endpoints"
        private const val KEY_TITLE = "title"
        private const val KEY_SSO_CODE = "sso_code"
    }
}
