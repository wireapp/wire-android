package com.wire.android.core.config

import com.wire.android.BuildConfig

object GlobalConfig {
    //System
    val OS_VERSION = "Android ${android.os.Build.VERSION.RELEASE}"
    const val USER_AGENT = "HttpLibrary ${okhttp3.internal.userAgent}"

    //Client App
    const val APP_VERSION = "Wire ${BuildConfig.VERSION_NAME}"

    //Urls and Api
    const val API_BASE_URL = BuildConfig.API_BASE_URL
    const val ACCOUNTS_URL = BuildConfig.ACCOUNTS_URL
}
