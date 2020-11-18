package com.wire.android.core.config

import com.wire.android.BuildConfig

//TODO this needs to be well adjusted and more dynamic. Futher solution incoming.
class GlobalConfig {
    //System
    val OS_VERSION = "Android ${android.os.Build.VERSION.RELEASE}"
    val USER_AGENT = "HttpLibrary ${okhttp3.internal.userAgent}"

    //Client App
    val APP_VERSION = "Wire ${BuildConfig.VERSION_NAME}"

    companion object {
        //Urls and Api
        const val API_BASE_URL = BuildConfig.API_BASE_URL
        const val ACCOUNTS_URL = BuildConfig.ACCOUNTS_URL
    }
}
