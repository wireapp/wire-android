/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */

package com.wire.android.util

import android.net.Uri
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Test

class DeepLinkProcessorTest {

    @Test
    fun `given a valid remote config deeplink, returns CustomServerConfig object`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withRemoteConfigDeeplink(FAKE_REMOTE_SERVER_URL)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = FAKE_REMOTE_SERVER_URL), result)
    }

    @Test
    fun `given a remote config deeplink with null parameters, returns DeeplinkResult-Unknown `() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withRemoteConfigDeeplink(null)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }

    @Test
    fun `given a valid success sso login deeplink, returns SSOLogin-Success object`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withSSOLoginSuccessDeeplink(FAKE_COOKIE, FAKE_REMOTE_SERVER_ID)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Success::class.java, result)
        assertEquals(DeepLinkResult.SSOLogin.Success(FAKE_COOKIE, FAKE_REMOTE_SERVER_ID), result)
    }

    @Test
    fun `given a sso login success deeplink with null parameters, returns SSOLogin-Failure with unknown error`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withSSOLoginSuccessDeeplink(null, null)
            .arrange()
        val loginSuccessNullResult = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, loginSuccessNullResult)
        assertEquals(
            DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByCode(SSOFailureCodes.SSOServerErrorCode.UNKNOWN)),
            loginSuccessNullResult
        )
    }

    @Test
    fun `given a valid failed sso login deeplink, returns SSOLogin-Failure object`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withSSOLoginFailureDeeplink(FAKE_ERROR)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, result)
        assertEquals(DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByLabel(FAKE_ERROR)), result)
    }

    @Test
    fun `given a sso login failure deeplink with null parameters, returns SSOLogin-Failure with unknown error`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withSSOLoginFailureDeeplink(null)
            .arrange()
        val loginFailureNullResult = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.SSOLogin.Failure::class.java, loginFailureNullResult)
        assertEquals(
            DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByCode(SSOFailureCodes.SSOServerErrorCode.UNKNOWN)),
            loginFailureNullResult
        )
    }

    @Test
    fun `given a invalid deeplink, returns Unknown object`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withInvalidDeeplink()
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }

    @Test
    fun `given a conversation deeplink for current user, returns Conversation with conversationId and not switched account`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withConversationDeepLink(CURRENT_USER_ID)
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .arrange()
        val conversationResult = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.OpenConversation::class.java, conversationResult)
        assertEquals(
            DeepLinkResult.OpenConversation(CONVERSATION_ID, false),
            conversationResult
        )
    }

    @Test
    fun `given a conversation deeplink for other user, returns Conversation with conversationId and switched account`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withConversationDeepLink(OTHER_USER_ID)
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .arrange()
        val conversationResult = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.OpenConversation::class.java, conversationResult)
        assertEquals(
            DeepLinkResult.OpenConversation(CONVERSATION_ID, true),
            conversationResult
        )
    }

    @Test
    fun `given an other profil deeplink for current user, returns Conversation with conversationId and not switched account`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withOtherUserProfileDeepLink(userIdToOpen = OTHER_USER_ID, userId = CURRENT_USER_ID)
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .arrange()
        val conversationResult = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.OpenOtherUserProfile::class.java, conversationResult)
        assertEquals(
            DeepLinkResult.OpenOtherUserProfile(OTHER_USER_ID, false),
            conversationResult
        )
    }

    @Test
    fun `given an other profile deeplink for other user, returns Conversation with conversationId and switched account`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withOtherUserProfileDeepLink(userIdToOpen = OTHER_USER_ID, userId = OTHER_USER_ID)
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .arrange()
        val conversationResult = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.OpenOtherUserProfile::class.java, conversationResult)
        assertEquals(
            DeepLinkResult.OpenOtherUserProfile(OTHER_USER_ID, true),
            conversationResult
        )
    }

    class Arrangement {

        @MockK
        private lateinit var accountSwitchUseCase: AccountSwitchUseCase

        @MockK
        private lateinit var currentSession: CurrentSessionUseCase

        @MockK
        internal lateinit var uri: Uri

        init {
            MockKAnnotations.init(this)
            coEvery { accountSwitchUseCase(any()) } returns SwitchAccountResult.SwitchedToAnotherAccount
        }

        fun arrange() = this to DeepLinkProcessor(accountSwitchUseCase, currentSession)

        fun withRemoteConfigDeeplink(url: String?) = apply {
            coEvery { uri.host } returns DeepLinkProcessor.ACCESS_DEEPLINK_HOST
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SERVER_CONFIG_PARAM) } returns url
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_COOKIE_PARAM) } returns null
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_SERVER_CONFIG_PARAM) } returns null
            coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
        }

        fun withSSOLoginSuccessDeeplink(cookie: String?, location: String?) = apply {
            coEvery { uri.host } returns DeepLinkProcessor.SSO_LOGIN_DEEPLINK_HOST
            coEvery { uri.lastPathSegment } returns DeepLinkProcessor.SSO_LOGIN_SUCCESS
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_COOKIE_PARAM) } returns cookie
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_SERVER_CONFIG_PARAM) } returns location
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_ERROR_PARAM) } returns null
            coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
        }

        fun withSSOLoginFailureDeeplink(error: String?) = apply {
            coEvery { uri.host } returns DeepLinkProcessor.SSO_LOGIN_DEEPLINK_HOST
            coEvery { uri.lastPathSegment } returns DeepLinkProcessor.SSO_LOGIN_FAILURE
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_ERROR_PARAM) } returns error
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_COOKIE_PARAM) } returns null
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SSO_LOGIN_SERVER_CONFIG_PARAM) } returns null
            coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
        }

        fun withInvalidDeeplink() = apply {
            coEvery { uri.host } returns INVALID_DEEPLINK_HOST
            coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns null
        }

        fun withConversationDeepLink(userId: UserId = CURRENT_USER_ID) = apply {
            coEvery { uri.host } returns DeepLinkProcessor.CONVERSATION_DEEPLINK_HOST
            coEvery { uri.lastPathSegment } returns CONVERSATION_ID.toString()
            coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns userId.toString()
        }

        fun withOtherUserProfileDeepLink(userIdToOpen: UserId = OTHER_USER_ID, userId: UserId = CURRENT_USER_ID) = apply {
            coEvery { uri.host } returns DeepLinkProcessor.OTHER_USER_PROFILE_DEEPLINK_HOST
            coEvery { uri.lastPathSegment } returns userIdToOpen.toString()
            coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns userId.toString()
        }

        fun withCurrentSession(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        fun withCurrentSessionSuccess(userId: UserId = CURRENT_USER_ID) = apply {
            withCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(userId = userId)))
        }

        fun withCurrentSessionError() = apply {
            withCurrentSession(CurrentSessionResult.Failure.SessionNotFound)
        }
    }

    private companion object {
        const val INVALID_DEEPLINK_HOST = "random_host"
        const val FAKE_COOKIE = "SOME_COOKIE"
        const val FAKE_REMOTE_SERVER_ID = "SOME_LOCATION_UUID"
        const val FAKE_REMOTE_SERVER_URL = "SOME_URL"
        const val FAKE_ERROR = "forbidden"
        val CONVERSATION_ID = ConversationId("some_conversation", "domain")
        val CURRENT_USER_ID = UserId("some_user", "domain")
        val OTHER_USER_ID = UserId("other_user", "other_domain")
    }
}
