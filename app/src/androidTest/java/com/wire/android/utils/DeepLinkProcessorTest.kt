package com.wire.android.utils

import android.net.Uri
import com.wire.android.util.EMPTY
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test


class DeepLinkProcessorTest {
    private var deepLinkProcessor = DeepLinkProcessor()

    @Test
    fun test1() {
        val result = deepLinkProcessor.invoke(generateRemoteConfigDeeplink(REMOTE_SERVER_URL))
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = REMOTE_SERVER_URL), result)
    }

    private fun generateRemoteConfigDeeplink(url: String) = Uri.Builder().scheme(HOST)
        .authority(REMOTE_CONFIG_AUTHORITY)
        .appendQueryParameter(CONFIG_PARAM, url).build()

    private fun generateSSOLoginSuccessDeeplink(cookie: String, location: String) = Uri.Builder().scheme(HOST)
        .authority(SSO_LOGIN_AUTHORITY)
        .appendPath(SSO_SUCCESS_PATH)
        .appendQueryParameter(COOKIE_PARAM, cookie)
        .appendQueryParameter(LOCATION_PARAM, location)
        .build()

    private fun generateSSOLoginSuccessDeeplink(error: String) = Uri.Builder().scheme(HOST)
        .authority(SSO_LOGIN_AUTHORITY)
        .appendPath(SSO_FAILURE_PATH)
        .appendQueryParameter(ERROR_PARAM, error)
        .build()


    private companion object {
        const val HOST = "wire"
        const val REMOTE_CONFIG_AUTHORITY = "access"
        const val SSO_LOGIN_AUTHORITY = "access"
        const val COOKIE = "SOME_COOKIE"
        const val REMOTE_SERVER_ID = "RANDOM_UUID"
        const val SSO_SUCCESS_PATH = "success"
        const val SSO_FAILURE_PATH = "failure"
        const val REMOTE_SERVER_URL = "SOME_URL"
        const val LOCATION_PARAM = "location"
        const val COOKIE_PARAM = "cookie"
        const val CONFIG_PARAM = "config"
        const val ERROR_PARAM = "config"


    }
}
