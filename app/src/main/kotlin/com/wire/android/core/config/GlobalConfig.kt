package com.wire.android.core.config

import com.wire.android.BuildConfig

/**
 * Global Wire Android Client Configuration.
 * Non-instantiability is enforced to encourage the creation of
 * injectable mini-config components.
 */
class GlobalConfig private constructor() {
    companion object {
        //OS Config
        val OS_VERSION = "Android ${android.os.Build.VERSION.RELEASE}"

        //Application Config
        const val APP_VERSION = "Wire ${BuildConfig.VERSION_NAME}"

        //Network Config
        const val USER_AGENT = "HttpLibrary ${okhttp3.internal.userAgent}"
        const val API_BASE_URL = BuildConfig.API_BASE_URL
        const val ACCOUNTS_URL = BuildConfig.ACCOUNTS_URL
    }
}
