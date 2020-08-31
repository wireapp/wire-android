package com.wire.android.core.network

private const val STAGING_BASE_URL = "https://staging-nginz-https.zinfra.io"
private const val STAGING_ACCOUNTS_URL = "https://wire-account-staging.zinfra.io"

//TODO: currently only points to staging.
data class BackendConfig(
    val baseUrl: String = STAGING_BASE_URL,
    val accountsUrl: String = STAGING_ACCOUNTS_URL
)
