/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 *
 *
 */

package com.wire.android.util

import android.net.Uri
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

class DeepLinkProcessorTest {
    @MockK
    private lateinit var accountSwitchUseCase: AccountSwitchUseCase

    @MockK
    private lateinit var currentSession: CurrentSessionUseCase

    private lateinit var deepLinkProcessor: DeepLinkProcessor

    @MockK
    private lateinit var uri: Uri

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this)
        coEvery { accountSwitchUseCase(any()) } returns Unit
        deepLinkProcessor = DeepLinkProcessor(accountSwitchUseCase, currentSession)
    }

    @Test
    fun `given a valid remote config deeplink, returns CustomServerConfig object`() = runTest {
        setupWithRemoteConfigDeeplink(FAKE_REMOTE_SERVER_URL)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = FAKE_REMOTE_SERVER_URL), result)
    }

    @Test
    fun `given a remote config deeplink with null parameters, returns DeeplinkResult-Unknown `() = runTest {
        setupWithRemoteConfigDeeplink(null)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }

    @Test
    fun `given a valid success sso login deeplink, returns SSOLogin-Success object`() = runTest {
        setupWithSSOLoginSuccessDeeplink(FAKE_COOKIE, FAKE_REMOTE_SERVER_ID)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Success::class.java, result)
        assertEquals(DeepLinkResult.SSOLogin.Success(FAKE_COOKIE, FAKE_REMOTE_SERVER_ID), result)
    }

    @Test
    fun `given a sso login success deeplink with null parameters, returns SSOLogin-Failure with unknown error`() = runTest {
        setupWithSSOLoginSuccessDeeplink(null, null)
        val loginSuccessNullResult = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, loginSuccessNullResult)
        assertEquals(
            DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByCode(SSOFailureCodes.SSOServerErrorCode.UNKNOWN)),
            loginSuccessNullResult
        )
    }

    @Test
    fun `given a valid failed sso login deeplink, returns SSOLogin-Failure object`() = runTest {
        setupWithSSOLoginFailureDeeplink(FAKE_ERROR)
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, result)
        assertEquals(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByLabel(FAKE_ERROR)), result)
    }

    @Test
    fun `given a sso login failure deeplink with null parameters, returns SSOLogin-Failure with unknown error`() = runTest {
        setupWithSSOLoginFailureDeeplink(null)
        val loginFailureNullResult = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, loginFailureNullResult)
        assertEquals(
            DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByCode(SSOFailureCodes.SSOServerErrorCode.UNKNOWN)),
            loginFailureNullResult
        )
    }

    @Test
    fun `given a incoming call deeplink, returns IncomingCall with conversationId`() = runTest {
        setupWithIncomingCallDeepLink()
        withCurrentSessionSuccess()
        val incomingCallResult = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.IncomingCall::class.java, incomingCallResult)
        assertEquals(
            DeepLinkResult.IncomingCall(ConversationId("some_call", "some_domain")),
            incomingCallResult
        )
    }

    @Test
    fun `given a invalid deeplink, returns Unknown object`() = runTest {
        setupWithInvalidDeeplink()
        val result = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }


    @Test
    fun `given a conversation deeplink, returns Conversation with conversationId`() = runTest {
        setupWithConversationDeepLink()
        withCurrentSessionSuccess()
        val conversationResult = deepLinkProcessor(uri)
        assertInstanceOf(DeepLinkResult.OpenConversation::class.java, conversationResult)
        assertEquals(
            DeepLinkResult.OpenConversation(ConversationId("some_conversation", "some_domain")),
            conversationResult
        )
    }

    private fun setupWithRemoteConfigDeeplink(url: String?) {
        coEvery { uri.host } returns DeepLinkProcessor.ACCESS_DEEPLINK_HOST
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SERVER_CONFIG_PARAM) } returns url
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_COOKIE_PARAM) } returns null
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_SERVER_CONFIG_PARAM) } returns null
        coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
    }

    private fun setupWithSSOLoginSuccessDeeplink(cookie: String?, location: String?) {
        coEvery { uri.host } returns DeepLinkProcessor.SSO_LOGIN_DEEPLINK_HOST
        coEvery { uri.lastPathSegment } returns DeepLinkProcessor.SSO_LOGIN_SUCCESS
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_COOKIE_PARAM) } returns cookie
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_SERVER_CONFIG_PARAM) } returns location
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_ERROR_PARAM) } returns null
        coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
    }

    private fun setupWithSSOLoginFailureDeeplink(error: String?) {
        coEvery { uri.host } returns DeepLinkProcessor.SSO_LOGIN_DEEPLINK_HOST
        coEvery { uri.lastPathSegment } returns DeepLinkProcessor.SSO_LOGIN_FAILURE
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_ERROR_PARAM) } returns error
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_COOKIE_PARAM) } returns null
        coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_SERVER_CONFIG_PARAM) } returns null
        coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
    }

    private fun setupWithInvalidDeeplink() {
        coEvery { uri.host } returns INVALID_DEEPLINK_HOST
        coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
    }

    private fun setupWithIncomingCallDeepLink() {
        coEvery { uri.host } returns DeepLinkProcessor.INCOMING_CALL_DEEPLINK_HOST
        coEvery { uri.lastPathSegment } returns "some_call@some_domain"
        coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns "some_value@some_domain"
    }

    private fun setupWithConversationDeepLink() {
        coEvery { uri.host } returns DeepLinkProcessor.CONVERSATION_DEEPLINK_HOST
        coEvery { uri.lastPathSegment } returns "some_conversation@some_domain"
        coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns "some_value@some_domain"
    }

    private fun withCurrentSession(result: CurrentSessionResult) {
        coEvery { currentSession() } returns result
    }

    private fun withCurrentSessionSuccess() {
        withCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(userId = UserId("test", "domain"))))
    }

    private fun withCurrentSessionError() {
        withCurrentSession(CurrentSessionResult.Failure.SessionNotFound)
    }

    private companion object {
        const val INVALID_DEEPLINK_HOST = "random_host"

        const val FAKE_COOKIE = "SOME_COOKIE"
        const val FAKE_REMOTE_SERVER_ID = "SOME_LOCATION_UUID"
        const val FAKE_REMOTE_SERVER_URL = "SOME_URL"
        const val FAKE_ERROR = "forbidden"
    }
}
