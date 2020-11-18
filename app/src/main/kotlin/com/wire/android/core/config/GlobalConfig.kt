package com.wire.android.core.config

import com.wire.android.BuildConfig

//TODO this needs to be well adjusted and more dynamic. Futher solution incoming.
class GlobalConfig {

    //System
    val osVersion = "Android ${android.os.Build.VERSION.RELEASE}"
    val userAgent = "HttpLibrary ${okhttp3.internal.userAgent}"

    //Client App
    val appVersion = "Wire ${BuildConfig.VERSION_NAME}"

    companion object {
        //Urls and Api
        const val API_BASE_URL = BuildConfig.API_BASE_URL
        const val ACCOUNTS_URL = BuildConfig.ACCOUNTS_URL
    }
}
