package com.wire.android.util

import android.net.Uri
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.junit.jupiter.api.Test
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import com.wire.kalium.logic.data.id.ConversationId

class DeepLinkProcessorTest {

    private val deepLinkProcessor = DeepLinkProcessor()

    @MockK
    private lateinit var uri: Uri

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
    }

    @Test
    fun `given a valid remote config deeplink, returns CustomServerConfig object`() {
        setupWithRemoteConfigDeeplink(FAKE_REMOTE_SERVER_URL)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = FAKE_REMOTE_SERVER_URL), result)
    }

    @Test
    fun `given a remote config deeplink with null parameters, returns DeeplinkResult-Unknown `() {
        setupWithRemoteConfigDeeplink(null)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }

    @Test
    fun `given a valid success sso login deeplink, returns SSOLogin-Success object`() {
        setupWithSSOLoginSuccessDeeplink(FAKE_COOKIE, FAKE_REMOTE_SERVER_ID)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Success::class.java, result)
        assertEquals(DeepLinkResult.SSOLogin.Success(FAKE_COOKIE, FAKE_REMOTE_SERVER_ID), result)
    }

    @Test
    fun `given a sso login success deeplink with null parameters, returns SSOLogin-Failure with unknown error`() {
        setupWithSSOLoginSuccessDeeplink(null, null)
        val loginSuccessNullResult = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, loginSuccessNullResult)
        assertEquals(
            DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByCode(SSOFailureCodes.SSOServerErrorCode.UNKNOWN)),
            loginSuccessNullResult
        )
    }

    @Test
    fun `given a valid failed sso login deeplink, returns SSOLogin-Failure object`() {
        setupWithSSOLoginFailureDeeplink(FAKE_ERROR)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, result)
        assertEquals(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByLabel(FAKE_ERROR)), result)
    }

    @Test
    fun `given a sso login failure deeplink with null parameters, returns SSOLogin-Failure with unknown error`() {
        setupWithSSOLoginFailureDeeplink(null)
        val loginFailureNullResult = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, loginFailureNullResult)
        assertEquals(
            DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByCode(SSOFailureCodes.SSOServerErrorCode.UNKNOWN)),
            loginFailureNullResult
        )
    }

    @Test
    fun `given a incoming call deeplink, returns IncomingCall with conversationId`() {
        setupWithIncomingCallDeepLink()
        val incomingCallResult = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.IncomingCall::class.java, incomingCallResult)
        assertEquals(
            DeepLinkResult.IncomingCall(ConversationId("some_value", "some_domain")),
            incomingCallResult
        )
    }

    @Test
    fun `given a invalid deeplink, returns Unknown object`() {
        setupWithInvalidDeeplink()
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }

    private fun setupWithRemoteConfigDeeplink(url: String?) {
        coEvery { uri.host } returns REMOTE_CONFIG_HOST
        coEvery { uri.getQueryParameter(CONFIG_PARAM) } returns url
    }

    private fun setupWithSSOLoginSuccessDeeplink(cookie: String?, location: String?) {
        coEvery { uri.host } returns SSO_LOGIN_HOST
        coEvery { uri.lastPathSegment } returns SSO_SUCCESS_PATH
        coEvery { uri.getQueryParameter(COOKIE_PARAM) } returns cookie
        coEvery { uri.getQueryParameter(LOCATION_PARAM) } returns location
    }

    private fun setupWithSSOLoginFailureDeeplink(error: String?) {
        coEvery { uri.host } returns SSO_LOGIN_HOST
        coEvery { uri.lastPathSegment } returns SSO_FAILURE_PATH
        coEvery { uri.getQueryParameter(ERROR_PARAM) } returns error
    }

    private fun setupWithInvalidDeeplink() {
        coEvery { uri.host } returns INVALID_DEEPLINK_HOST
    }

    private fun setupWithIncomingCallDeepLink() {
        coEvery { uri.host } returns INCOMING_CALL_HOST
        coEvery { uri.lastPathSegment } returns "some_value@some_domain"
    }

    private companion object {
        const val INVALID_DEEPLINK_HOST = "random_host"
        const val REMOTE_CONFIG_HOST = "access"
        const val SSO_LOGIN_HOST = "sso-login"
        const val SSO_SUCCESS_PATH = "success"
        const val SSO_FAILURE_PATH = "failure"
        const val LOCATION_PARAM = "location"
        const val COOKIE_PARAM = "cookie"
        const val CONFIG_PARAM = "config"
        const val ERROR_PARAM = "error"
        const val INCOMING_CALL_HOST = "incoming-call"
        const val INCOMING_CALL_CONVERSATION_ID_PARAM = "conversation_id"

        const val FAKE_COOKIE = "SOME_COOKIE"
        const val FAKE_REMOTE_SERVER_ID = "SOME_LOCATION_UUID"
        const val FAKE_REMOTE_SERVER_URL = "SOME_URL"
        const val FAKE_ERROR = "forbidden"
    }
}
