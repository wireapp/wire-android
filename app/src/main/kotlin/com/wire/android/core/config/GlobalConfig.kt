package com.wire.android.core.config

import com.wire.android.BuildConfig

class GlobalConfig() {
    //System
    val OS_VERSION = "Android ${android.os.Build.VERSION.RELEASE}"
    val USER_AGENT = "HttpLibrary ${okhttp3.internal.userAgent}"

    //Client App
    val APP_VERSION = "Wire ${BuildConfig.VERSION_NAME}"

    //Urls and Api

    companion object {
        const val API_BASE_URL = BuildConfig.API_BASE_URL
        const val ACCOUNTS_URL = BuildConfig.ACCOUNTS_URL
    }
}
