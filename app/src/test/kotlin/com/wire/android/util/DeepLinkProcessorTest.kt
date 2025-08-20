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

import android.content.Intent
import android.net.Uri
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModelTest
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.deeplink.LoginType
import com.wire.android.util.deeplink.SSOFailureCodes
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
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
    fun `given a valid remote config deeplink with old login type, returns CustomServerConfig with old login type`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withRemoteConfigDeeplink(url = FAKE_REMOTE_SERVER_URL, loginType = LoginType.Old.name)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = FAKE_REMOTE_SERVER_URL, loginType = LoginType.Old), result)
    }

    @Test
    fun `given a valid remote config deeplink with new login type, returns CustomServerConfig with new login type`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withRemoteConfigDeeplink(url = FAKE_REMOTE_SERVER_URL, loginType = LoginType.New.name)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = FAKE_REMOTE_SERVER_URL, loginType = LoginType.New), result)
    }

    @Test
    fun `given a valid remote config deeplink without login type, returns CustomServerConfig with default login type`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withRemoteConfigDeeplink(url = FAKE_REMOTE_SERVER_URL, loginType = null)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = FAKE_REMOTE_SERVER_URL, loginType = LoginType.Default), result)
    }

    @Test
    fun `given a valid remote config deeplink with invalid login type, returns CustomServerConfig with default login type`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withRemoteConfigDeeplink(url = FAKE_REMOTE_SERVER_URL, loginType = "some-invalid-login-type")
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(url = FAKE_REMOTE_SERVER_URL, loginType = LoginType.Default), result)
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
    fun `given a invalid deeplink with no specific action, returns Unknown object`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withInvalidDeeplink()
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }

    @Test
    fun `given a invalid deeplink with view action, returns Unknown object`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withInvalidDeeplink()
            .arrange()
        val result = deepLinkProcessor(arrangement.uri, Intent.ACTION_VIEW)
        assertInstanceOf(DeepLinkResult.Unknown::class.java, result)
        assertEquals(DeepLinkResult.Unknown, result)
    }

    @Test
    fun `given a invalid deeplink with sharing intent action, returns sharing intent`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withInvalidDeeplink()
            .arrange()
        val result = deepLinkProcessor(arrangement.uri, Intent.ACTION_SEND)
        assertInstanceOf(DeepLinkResult.SharingIntent::class.java, result)
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
    fun `given an other profile deeplink for current user, returns Conversation with conversationId and not switched account`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withOtherUserProfileDeepLink(userIdToOpen = OTHER_USER_ID, userId = CURRENT_USER_ID)
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .withSwitchedAccount(SwitchAccountResult.SwitchedToAnotherAccount)
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

    @Test
    fun `given a deeplink requiring account switch during an ongoing call, returns Failure with OngoingCall reason`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .withConversationDeepLink(userId = OTHER_USER_ID)
            .withOngoingCall()
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.SwitchAccountFailure::class.java, result)
        assertEquals(DeepLinkResult.SwitchAccountFailure.OngoingCall, result)
    }

    @Test
    fun `given a deeplink requiring account switch that fails due to unknown error, returns Failure with Unknown reason`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .withConversationDeepLink(userId = OTHER_USER_ID)
            .withSwitchedAccount(SwitchAccountResult.Failure)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.SwitchAccountFailure::class.java, result)
        assertEquals(DeepLinkResult.SwitchAccountFailure.Unknown, result)
    }

    @Test
    fun `given a deeplink accessed by an unauthorized user, returns AuthorizationNeeded`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withCurrentSessionError(CurrentSessionResult.Failure.SessionNotFound)
            .withConversationDeepLink(userId = OTHER_USER_ID)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.AuthorizationNeeded::class.java, result)
    }

    @Test
    fun `given a valid deeplink for an authorized session without account switch, returns appropriate deep link result`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .withRemoteConfigDeeplink(FAKE_REMOTE_SERVER_URL)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.CustomServerConfig::class.java, result)
        assertEquals(DeepLinkResult.CustomServerConfig(FAKE_REMOTE_SERVER_URL), result)
    }

    @Test
    fun `given a deeplink with a sharing intent action, returns SharingIntent result`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .arrange()
        val result = deepLinkProcessor(arrangement.uri, Intent.ACTION_SEND)
        assertInstanceOf(DeepLinkResult.SharingIntent::class.java, result)
        assertEquals(DeepLinkResult.SharingIntent, result)
    }

    @Test
    fun `given an other profile deeplink from QR code, returns Conversation with conversationId`() = runTest {
        val (arrangement, deepLinkProcessor) = Arrangement()
            .withOtherUserProfileQRDeepLink(userIdToOpen = OTHER_USER_ID, userId = CURRENT_USER_ID)
            .withCurrentSessionSuccess(CURRENT_USER_ID)
            .arrange()
        val conversationResult = deepLinkProcessor(arrangement.uri)
        assertInstanceOf(DeepLinkResult.OpenOtherUserProfile::class.java, conversationResult)
        assertEquals(
            DeepLinkResult.OpenOtherUserProfile(UserId("other_user", "other_domain"), false),
            conversationResult
        )
    }

    class Arrangement {

        @MockK
        private lateinit var accountSwitchUseCase: AccountSwitchUseCase

        @MockK
        private lateinit var currentSession: CurrentSessionUseCase

        @MockK
        private lateinit var coreLogic: CoreLogic

        @MockK
        private lateinit var establishedCallsUseCase: ObserveEstablishedCallsUseCase

        @MockK
        internal lateinit var uri: Uri

        init {
            MockKAnnotations.init(this)
            coEvery { accountSwitchUseCase(any()) } returns SwitchAccountResult.SwitchedToAnotherAccount
            coEvery { currentSession() } returns CurrentSessionResult.Success(AccountInfo.Valid(CURRENT_USER_ID))
            coEvery { establishedCallsUseCase.invoke() } returns flowOf(emptyList())
            every { coreLogic.getSessionScope(any()).calls.establishedCall } returns establishedCallsUseCase
        }

        fun arrange() = this to DeepLinkProcessor(accountSwitchUseCase, currentSession, coreLogic)

        fun withSwitchedAccount(switchAccountResult: SwitchAccountResult) = apply {
            coEvery { accountSwitchUseCase(any()) } returns switchAccountResult
        }

        fun withRemoteConfigDeeplink(url: String?, loginType: String? = null) = apply {
            coEvery { uri.host } returns DeepLinkProcessor.ACCESS_DEEPLINK_HOST
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SERVER_CONFIG_PARAM) } returns url
            coEvery { uri.getQueryParameter(DeepLinkProcessor.SERVER_CONFIG_LOGIN_TYPE_PARAM) } returns loginType
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

        fun withOtherUserProfileQRDeepLink(userIdToOpen: UserId = OTHER_USER_ID, userId: UserId = CURRENT_USER_ID) = apply {
            coEvery { uri.host } returns DeepLinkProcessor.OPEN_USER_PROFILE_DEEPLINK_HOST
            coEvery { uri.pathSegments } returns listOf(userIdToOpen.domain, userIdToOpen.value)
            coEvery { uri.getQueryParameter(DeepLinkProcessor.USER_TO_USE_QUERY_PARAM) } returns userId.toString()
        }

        fun withCurrentSession(result: CurrentSessionResult) = apply {
            coEvery { currentSession() } returns result
        }

        fun withCurrentSessionSuccess(userId: UserId = CURRENT_USER_ID) = apply {
            withCurrentSession(CurrentSessionResult.Success(AccountInfo.Valid(userId = userId)))
        }

        fun withCurrentSessionError(failure: CurrentSessionResult.Failure) = apply {
            withCurrentSession(failure)
        }

        fun withOngoingCall() = apply {
            coEvery { establishedCallsUseCase.invoke() } returns flowOf(listOf(ONGOING_CALL))
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
        val ONGOING_CALL = Call(
            CommonTopAppBarViewModelTest.conversationId,
            CallStatus.ESTABLISHED,
            isMuted = true,
            isCameraOn = false,
            isCbrEnabled = false,
            callerId = UserId("caller", "domain"),
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.OneOnOne,
            callerName = "otherUsername",
            callerTeamName = "team1"
        )
    }
}
