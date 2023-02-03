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

package com.wire.android.util.deeplink

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.wire.android.appLogger
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapperImpl
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

sealed class DeepLinkResult {
    object Unknown : DeepLinkResult()
    data class CustomServerConfig(val url: String) : DeepLinkResult()
    sealed class SSOLogin : DeepLinkResult() {
        data class Success(val cookie: String, val serverConfigId: String) : SSOLogin()
        data class Failure(val ssoError: SSOFailureCodes) : SSOLogin()
    }

    data class IncomingCall(val conversationsId: ConversationId) : DeepLinkResult()

    data class OngoingCall(val conversationsId: ConversationId) : DeepLinkResult()
    data class OpenConversation(val conversationsId: ConversationId) : DeepLinkResult()
    data class OpenOtherUserProfile(val userId: QualifiedID) : DeepLinkResult()
}

@Singleton
class DeepLinkProcessor @Inject constructor(
    private val accountSwitch: AccountSwitchUseCase,
    private val currentSession: CurrentSessionUseCase
) {
    private val qualifiedIdMapper = QualifiedIdMapperImpl(null)

    operator fun invoke(uri: Uri, scope: CoroutineScope): DeepLinkResult = when (uri.host) {
        ACCESS_DEEPLINK_HOST -> getCustomServerConfigDeepLinkResult(uri)
        SSO_LOGIN_DEEPLINK_HOST -> getSSOLoginDeepLinkResult(uri)
        INCOMING_CALL_DEEPLINK_HOST -> getIncomingCallDeepLinkResult(uri)
        ONGOING_CALL_DEEPLINK_HOST -> getOngoingCallDeepLinkResult(uri)
        CONVERSATION_DEEPLINK_HOST -> getOpenConversationDeepLinkResult(uri, scope)
        OTHER_USER_PROFILE_DEEPLINK_HOST -> getOpenOtherUserProfileDeepLinkResult(uri)
        else -> DeepLinkResult.Unknown
    }

    @Suppress("TooGenericExceptionCaught")
    private fun getOpenConversationDeepLinkResult(uri: Uri, scope: CoroutineScope): DeepLinkResult {
        return try {
            val conversationId = uri.pathSegments[0]?.toQualifiedID(qualifiedIdMapper)
            val userId = uri.pathSegments[1]?.toQualifiedID(qualifiedIdMapper)
            if (conversationId == null || userId == null) return DeepLinkResult.Unknown

            val shouldSwitchAccount = currentSession().let {
                when (it) {
                    is CurrentSessionResult.Failure.Generic -> true
                    CurrentSessionResult.Failure.SessionNotFound -> true
                    is CurrentSessionResult.Success -> {
                        it.accountInfo.userId != userId
                    }
                }
            }
            if (shouldSwitchAccount) {
                scope.launch { accountSwitch(SwitchAccountParam.SwitchToAccount(userId)) }
            }

            DeepLinkResult.OpenConversation(conversationId)
        } catch (e: IndexOutOfBoundsException) {
            appLogger.e("unknown segment")
            DeepLinkResult.Unknown
        }
    }

    private fun getOpenOtherUserProfileDeepLinkResult(uri: Uri): DeepLinkResult =
        uri.lastPathSegment?.toQualifiedID(qualifiedIdMapper)?.let {
            DeepLinkResult.OpenOtherUserProfile(it)
        } ?: DeepLinkResult.Unknown

    private fun getCustomServerConfigDeepLinkResult(uri: Uri) = uri.getQueryParameter(SERVER_CONFIG_PARAM)?.let {
        DeepLinkResult.CustomServerConfig(it)
    } ?: DeepLinkResult.Unknown

    private fun getIncomingCallDeepLinkResult(uri: Uri) =
        uri.lastPathSegment?.toQualifiedID(qualifiedIdMapper)?.let {
            DeepLinkResult.IncomingCall(it)
        } ?: DeepLinkResult.Unknown

    private fun getOngoingCallDeepLinkResult(uri: Uri) =
        uri.lastPathSegment?.toQualifiedID(qualifiedIdMapper)?.let {
            DeepLinkResult.OngoingCall(it)
        } ?: DeepLinkResult.Unknown

    private fun getSSOLoginDeepLinkResult(uri: Uri): DeepLinkResult {

        val lastPathSegment = uri.lastPathSegment
        val error = uri.getQueryParameter(SSO_LOGIN_ERROR_PARAM)
        val cookie = uri.getQueryParameter(SSO_LOGIN_COOKIE_PARAM)
        val location = uri.getQueryParameter(SSO_LOGIN_SERVER_CONFIG_PARAM)

        return when {
            lastPathSegment == SSO_LOGIN_FAILURE && error != null ->
                DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByLabel(error))

            lastPathSegment == SSO_LOGIN_SUCCESS && cookie != null && location != null ->
                DeepLinkResult.SSOLogin.Success(cookie, location)

            else -> DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown)
        }
    }

    companion object {
        const val DEEP_LINK_SCHEME = "wire"
        const val ACCESS_DEEPLINK_HOST = "access"
        const val SERVER_CONFIG_PARAM = "config"
        const val SSO_LOGIN_DEEPLINK_HOST = "sso-login"
        const val SSO_LOGIN_FAILURE = "failure"
        const val SSO_LOGIN_SUCCESS = "success"
        const val SSO_LOGIN_USERID_PARAM = "userId"
        const val SSO_LOGIN_COOKIE_PARAM = "cookie"
        const val SSO_LOGIN_ERROR_PARAM = "error"
        const val SSO_LOGIN_SERVER_CONFIG_PARAM = "location"
        const val INCOMING_CALL_DEEPLINK_HOST = "incoming-call"
        const val ONGOING_CALL_DEEPLINK_HOST = "ongoing-call"
        const val CONVERSATION_DEEPLINK_HOST = "conversation"
        const val OTHER_USER_PROFILE_DEEPLINK_HOST = "other-user-profile"
    }
}

enum class SSOFailureCodes(val label: String, val errorCode: Int) {
    ServerErrorUnsupportedSaml("server-error-unsupported-saml", SSOServerErrorCode.SERVER_ERROR_UNSUPPORTED_SAML),
    BadSuccessRedirect("bad-success-redirect", SSOServerErrorCode.BAD_SUCCESS_REDIRECT),
    BadFailureRedirect("bad-failure-redirect", SSOServerErrorCode.BAD_FAILURE_REDIRECT),
    BadUsername("bad-username", SSOServerErrorCode.BAD_USERNAME),
    BadUpstream("bad-upstream", SSOServerErrorCode.BAD_UPSTREAM),
    ServerError("server-error", SSOServerErrorCode.SERVER_ERROR),
    NotFound("not-found", SSOServerErrorCode.NOT_FOUND),
    Forbidden("forbidden", SSOServerErrorCode.FORBIDDEN),
    NoMatchingAuthReq("no-matching-auth-req", SSOServerErrorCode.NO_MATCHING_AUTH_REQ),
    InsufficientPermissions("insufficient-permissions", SSOServerErrorCode.INSUFFICIENT_PERMISSIONS),
    Unknown("unknown", SSOServerErrorCode.UNKNOWN);

    companion object {
        fun getByCode(errorCode: Int) = values().first { it.errorCode == errorCode }
        fun getByLabel(label: String) = values().first { it.label == label }
    }

    object SSOServerErrorCode {
        const val SERVER_ERROR_UNSUPPORTED_SAML = 1
        const val BAD_SUCCESS_REDIRECT = 2
        const val BAD_FAILURE_REDIRECT = 3
        const val BAD_USERNAME = 4
        const val BAD_UPSTREAM = 5
        const val SERVER_ERROR = 6
        const val NOT_FOUND = 7
        const val FORBIDDEN = 8
        const val NO_MATCHING_AUTH_REQ = 9
        const val INSUFFICIENT_PERMISSIONS = 10

        @VisibleForTesting
        const val UNKNOWN = 0
    }
}
