package com.wire.android.util

import android.content.Context
import android.os.Build
import com.wire.android.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton class UserAgentProvider@Inject constructor(@ApplicationContext private val context: Context) {

    val defaultUserAgent =
        "Wire/${BuildConfig.VERSION_NAME}/${context.getGitBuildId()}/${getAndroidVersion()}/${systemAgent() ?: "unknownSystemAgent"}"

    private fun getAndroidVersion(): String {
        val release: String = Build.VERSION.RELEASE
        val sdkVersion: Int = Build.VERSION.SDK_INT
        return "android:$release/sdk:$sdkVersion"
    }

    @Suppress("TooGenericExceptionCaught")
    private fun systemAgent(): String? = try {
        System.getProperty("http.agent")
    } catch (e: Exception) {
        null
    }
}
