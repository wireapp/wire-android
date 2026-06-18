/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.wire.android.feature.aiassistant

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

interface WireLlmClient {
    suspend fun query(serverIp: String, prompt: String): WireLlmQueryResult
}

sealed interface WireLlmQueryResult {
    data class Success(val result: String) : WireLlmQueryResult
    data class Failure(val message: String) : WireLlmQueryResult
}

interface WireLlmConfigStore {
    fun observeServerIp(): Flow<String?>
    suspend fun setServerIp(serverIp: String)
}

object WireLlmServerAddress {
    fun normalize(value: String): String? {
        val trimmed = value.trim()
        if (!IPV4_REGEX.matches(trimmed)) return null
        val octets = trimmed.split('.')
        if (octets.any { it.length > 1 && it.startsWith('0') }) return null
        return trimmed.takeIf { octets.all { it.toIntOrNull() in 0..255 } }
    }

    private val IPV4_REGEX = Regex("""\d{1,3}(?:\.\d{1,3}){3}""")
}

object UnsupportedWireLlmClient : WireLlmClient {
    override suspend fun query(serverIp: String, prompt: String): WireLlmQueryResult =
        WireLlmQueryResult.Failure("Wire LLM is not configured")
}

object EmptyWireLlmConfigStore : WireLlmConfigStore {
    override fun observeServerIp(): Flow<String?> = flowOf(null)
    override suspend fun setServerIp(serverIp: String) = Unit
}
