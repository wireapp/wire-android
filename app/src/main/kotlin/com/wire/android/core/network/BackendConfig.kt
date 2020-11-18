package com.wire.android.core.network

import com.wire.android.core.config.GlobalConfig

data class BackendConfig(
    val baseUrl: String = GlobalConfig.API_BASE_URL,
    val accountsUrl: String = GlobalConfig.ACCOUNTS_URL
)
