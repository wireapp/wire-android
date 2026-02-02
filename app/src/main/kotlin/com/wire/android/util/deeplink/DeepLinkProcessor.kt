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

package com.wire.android.util.deeplink

import android.content.Intent
import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.feature.SwitchAccountParam
import com.wire.android.feature.SwitchAccountResult
import com.wire.android.util.EMPTY
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.id.QualifiedIdMapper
import com.wire.kalium.logic.data.id.toQualifiedID
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import kotlinx.coroutines.flow.first
import kotlinx.serialization.Serializable
import javax.inject.Inject
import javax.inject.Singleton

sealed class DeepLinkResult {
    data object Unknown : DeepLinkResult()
    data class CustomServerConfig(val url: String, val loginType: LoginType = LoginType.Default) : DeepLinkResult()

    @Serializable
    sealed class SSOLogin : DeepLinkResult() {
        @Serializable
        data class Success(val cookie: String, val serverConfigId: String) : SSOLogin()

        @Serializable
        data class Failure(val ssoError: SSOFailureCodes) : SSOLogin()
    }

    data class OpenConversation(
        val conversationId: ConversationId,
        val switchedAccount: Boolean = false
    ) : DeepLinkResult()

    data class OpenOtherUserProfile(val userId: QualifiedID, val switchedAccount: Boolean = false) : DeepLinkResult()

    data class JoinConversation(
        val code: String,
        val key: String,
        val domain: String?,
        val switchedAccount: Boolean = false
    ) : DeepLinkResult()

    data class MigrationLogin(val userHandle: String) : DeepLinkResult()

    data object SharingIntent : DeepLinkResult()

    data object AuthorizationNeeded : DeepLinkResult()

    sealed class SwitchAccountFailure : DeepLinkResult() {
        data object OngoingCall : SwitchAccountFailure()
        data object Unknown : SwitchAccountFailure()
    }
}

@Singleton
class DeepLinkProcessor @Inject constructor(
    private val accountSwitch: AccountSwitchUseCase,
    private val currentSession: CurrentSessionUseCase,
    @KaliumCoreLogic private val coreLogic: CoreLogic,
) {
    private val qualifiedIdMapper = QualifiedIdMapper(null)

    suspend operator fun invoke(uri: Uri? = null, action: String? = null): DeepLinkResult {
        return when (val sessionResult = currentSession()) {
            is CurrentSessionResult.Failure.Generic,
            CurrentSessionResult.Failure.SessionNotFound -> uri?.let { handleNotAuthorizedDeepLinks(uri) } ?: DeepLinkResult.Unknown

            is CurrentSessionResult.Success -> when (action) {
                Intent.ACTION_SEND, Intent.ACTION_SEND_MULTIPLE -> DeepLinkResult.SharingIntent
                else -> uri?.let { handleDeepLinks(uri, sessionResult.accountInfo) } ?: DeepLinkResult.Unknown
            }
        }
    }

    private suspend fun handleDeepLinks(uri: Uri, accountInfo: AccountInfo): DeepLinkResult {
        return when (accountInfo) {
            is AccountInfo.Invalid -> {
                handleNotAuthorizedDeepLinks(uri)
            }

            is AccountInfo.Valid -> {
                return when (val switchStatus = switchAccountIfNeeded(uri, accountInfo)) {
                    SwitchAccountStatus.Switched,
                    SwitchAccountStatus.NoNeeded -> {
                        var deepLinkResult = handleNotAuthorizedDeepLinks(uri)
                        if (deepLinkResult == DeepLinkResult.AuthorizationNeeded) {
                            deepLinkResult = handleAuthorizedDeepLinks(
                                uri = uri,
                                accountInfo = accountInfo,
                                switchedAccount = switchStatus == SwitchAccountStatus.Switched
                            )
                        }
                        deepLinkResult
                    }

                    SwitchAccountStatus.FailedDueToCall -> DeepLinkResult.SwitchAccountFailure.OngoingCall
                    SwitchAccountStatus.FailedDueToUnknownError -> DeepLinkResult.SwitchAccountFailure.Unknown
                }
            }
        }
    }

    private fun handleNotAuthorizedDeepLinks(uri: Uri): DeepLinkResult {
        return when (uri.host) {
            ACCESS_DEEPLINK_HOST -> getCustomServerConfigDeepLinkResult(uri)
            SSO_LOGIN_DEEPLINK_HOST -> getSSOLoginDeepLinkResult(uri)
            MIGRATION_LOGIN_HOST -> getOpenMigrationLoginDeepLinkResult(uri)
            else -> DeepLinkResult.AuthorizationNeeded
        }
    }

    private fun handleAuthorizedDeepLinks(uri: Uri, accountInfo: AccountInfo.Valid, switchedAccount: Boolean): DeepLinkResult {
        return when (uri.host) {
            CONVERSATION_DEEPLINK_HOST -> getOpenConversationDeepLinkResult(uri, switchedAccount)
            OTHER_USER_PROFILE_DEEPLINK_HOST -> getOpenOtherUserProfileDeepLinkResult(uri, switchedAccount)
            JOIN_CONVERSATION_DEEPLINK_HOST -> getJoinConversationDeepLinkResult(uri, switchedAccount)
            OPEN_USER_PROFILE_DEEPLINK_HOST -> getConnectingUserProfile(uri, switchedAccount, accountInfo)
            else -> DeepLinkResult.Unknown
        }
    }

    /**
     * Format of deeplink to parse: wire://user/domain/user-id
     */
    private fun getConnectingUserProfile(uri: Uri, switchedAccount: Boolean, accountInfo: AccountInfo.Valid): DeepLinkResult {
        return when (val result = UserLinkQRMapper.fromDeepLinkToQualifiedId(uri, accountInfo.userId.domain)) {
            is UserLinkQRMapper.UserLinkQRResult.Failure -> DeepLinkResult.Unknown
            is UserLinkQRMapper.UserLinkQRResult.Success -> DeepLinkResult.OpenOtherUserProfile(result.qualifiedUserId, switchedAccount)
        }
    }

    private suspend fun switchAccountIfNeeded(uri: Uri, accountInfo: AccountInfo.Valid): SwitchAccountStatus =
        uri.getQueryParameter(USER_TO_USE_QUERY_PARAM)?.toQualifiedID(qualifiedIdMapper)
            ?.let { userId ->
                return when {
                    accountInfo.userId == userId -> {
                        SwitchAccountStatus.NoNeeded
                    }

                    coreLogic.getSessionScope(accountInfo.userId).calls.establishedCall().first().isNotEmpty() -> {
                        SwitchAccountStatus.FailedDueToCall
                    }

                    else -> {
                        when (accountSwitch(SwitchAccountParam.SwitchToAccount(userId))) {
                            SwitchAccountResult.Failure -> SwitchAccountStatus.FailedDueToUnknownError
                            SwitchAccountResult.GivenAccountIsInvalid -> SwitchAccountStatus.FailedDueToUnknownError
                            SwitchAccountResult.NoOtherAccountToSwitch -> SwitchAccountStatus.NoNeeded
                            SwitchAccountResult.SwitchedToAnotherAccount -> SwitchAccountStatus.Switched
                        }
                    }
                }
            } ?: SwitchAccountStatus.NoNeeded

    private fun getOpenConversationDeepLinkResult(
        uri: Uri,
        switchedAccount: Boolean
    ): DeepLinkResult =
        uri.lastPathSegment?.toQualifiedID(qualifiedIdMapper)?.let { conversationId ->
            DeepLinkResult.OpenConversation(conversationId, switchedAccount)
        } ?: DeepLinkResult.Unknown

    private fun getOpenOtherUserProfileDeepLinkResult(
        uri: Uri,
        switchedAccount: Boolean
    ): DeepLinkResult =
        uri.lastPathSegment?.toQualifiedID(qualifiedIdMapper)?.let {
            DeepLinkResult.OpenOtherUserProfile(it, switchedAccount)
        } ?: DeepLinkResult.Unknown

    private fun getOpenMigrationLoginDeepLinkResult(uri: Uri): DeepLinkResult =
        uri.lastPathSegment?.let {
            if (it == MIGRATION_LOGIN_HOST) {
                DeepLinkResult.MigrationLogin(String.EMPTY)
            } else {
                DeepLinkResult.MigrationLogin(it)
            }
        } ?: DeepLinkResult.Unknown

    private fun getCustomServerConfigDeepLinkResult(uri: Uri) =
        uri.getQueryParameter(SERVER_CONFIG_PARAM)?.let {
            val loginType = LoginType.getByName(uri.getQueryParameter(SERVER_CONFIG_LOGIN_TYPE_PARAM) ?: LoginType.Default.name)
            DeepLinkResult.CustomServerConfig(it, loginType)
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

    private fun getJoinConversationDeepLinkResult(uri: Uri, switchedAccount: Boolean): DeepLinkResult {
        val code = uri.getQueryParameter(JOIN_CONVERSATION_CODE_PARAM)
        val key = uri.getQueryParameter(JOIN_CONVERSATION_KEY_PARAM)
        val domain = uri.getQueryParameter(JOIN_CONVERSATION_DOMAIN_PARAM)
        if (code == null || key == null) return DeepLinkResult.Unknown
        return DeepLinkResult.JoinConversation(code, key, domain, switchedAccount)
    }

    companion object {
        const val DEEP_LINK_SCHEME = "wire"
        const val E2EI_DEEPLINK_HOST = "e2ei"
        const val E2EI_DEEPLINK_OAUTH_REDIRECT_PATH = "oauth2redirect"
        const val ACCESS_DEEPLINK_HOST = "access"
        const val SERVER_CONFIG_PARAM = "config"
        const val SERVER_CONFIG_LOGIN_TYPE_PARAM = "login-type"
        const val SSO_LOGIN_DEEPLINK_HOST = "sso-login"
        const val SSO_LOGIN_FAILURE = "failure"
        const val SSO_LOGIN_SUCCESS = "success"
        const val SSO_LOGIN_USERID_PARAM = "userId"
        const val SSO_LOGIN_COOKIE_PARAM = "cookie"
        const val SSO_LOGIN_ERROR_PARAM = "error"
        const val SSO_LOGIN_SERVER_CONFIG_PARAM = "location"
        const val CONVERSATION_DEEPLINK_HOST = "conversation"
        const val OTHER_USER_PROFILE_DEEPLINK_HOST = "other-user-profile"
        const val MIGRATION_LOGIN_HOST = "migration-login"
        const val JOIN_CONVERSATION_DEEPLINK_HOST = "conversation-join"
        const val JOIN_CONVERSATION_CODE_PARAM = "code"
        const val JOIN_CONVERSATION_KEY_PARAM = "key"
        const val JOIN_CONVERSATION_DOMAIN_PARAM = "domain"
        const val USER_TO_USE_QUERY_PARAM = "user-to-use"
        const val OPEN_USER_PROFILE_DEEPLINK_HOST = "user"
    }
}

enum class SSOFailureCodes(val label: String, val errorCode: Int) {
    ServerErrorUnsupportedSaml(
        "server-error-unsupported-saml",
        SSOServerErrorCode.SERVER_ERROR_UNSUPPORTED_SAML
    ),
    BadSuccessRedirect("bad-success-redirect", SSOServerErrorCode.BAD_SUCCESS_REDIRECT),
    BadFailureRedirect("bad-failure-redirect", SSOServerErrorCode.BAD_FAILURE_REDIRECT),
    BadUsername("bad-username", SSOServerErrorCode.BAD_USERNAME),
    BadUpstream("bad-upstream", SSOServerErrorCode.BAD_UPSTREAM),
    ServerError("server-error", SSOServerErrorCode.SERVER_ERROR),
    NotFound("not-found", SSOServerErrorCode.NOT_FOUND),
    Forbidden("forbidden", SSOServerErrorCode.FORBIDDEN),
    NoMatchingAuthReq("no-matching-auth-req", SSOServerErrorCode.NO_MATCHING_AUTH_REQ),
    InsufficientPermissions(
        "insufficient-permissions",
        SSOServerErrorCode.INSUFFICIENT_PERMISSIONS
    ),
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

enum class SwitchAccountStatus {
    Switched,
    NoNeeded,
    FailedDueToCall,
    FailedDueToUnknownError
}

enum class LoginType {
    Default, Old, New;

    companion object {
        fun getByName(value: String) = entries.firstOrNull { it.name.lowercase() == value.lowercase() } ?: Default
    }
}
