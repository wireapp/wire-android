package com.wire.android.core.network

import com.wire.android.core.config.GlobalConfig

data class NetworkConfig(
    val osVersion: String = GlobalConfig.OS_VERSION,
    val appVersion: String = GlobalConfig.APP_VERSION,
    val userAgent: String = GlobalConfig.USER_AGENT,
    val baseUrl: String = GlobalConfig.API_BASE_URL,
    val accountsUrl: String = GlobalConfig.ACCOUNTS_URL) {

    companion object {
        const val USER_AGENT_HEADER_KEY = "User-Agent"
    }
}
