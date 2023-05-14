package com.wire.android.util

import android.content.Context
import android.os.Build
import com.wire.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserAgentProvider @Inject constructor(@ApplicationContext private val context: Context) {

    val defaultUserAgent =
        "Wire/${BuildConfig.VERSION_NAME}/${context.getGitBuildId()}/${getAndroidVersion()}"

    private fun getAndroidVersion(): String {
        val sdkVersion: Int = Build.VERSION.SDK_INT
        return "sdk:$sdkVersion"
    }
}
