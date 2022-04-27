package com.wire.android.util.deeplink

import android.net.Uri
import androidx.annotation.VisibleForTesting
import com.wire.android.R

sealed class DeepLinkResult {
    object Unknown : DeepLinkResult()
    data class CustomServerConfig(val url: String) : DeepLinkResult()
    sealed class SSOLogin : DeepLinkResult() {
        data class Success(val cookie: String, val serverConfigId: String) : SSOLogin()
        data class Failure(val ssoError: SSOFailureCodes) : SSOLogin()
    }
}

class DeepLinkProcessor {
    operator fun invoke(uri: Uri): DeepLinkResult = when (uri.host) {
        ACCESS_DEEPLINK_HOST ->
            uri.getQueryParameter(SERVER_CONFIG_PARAM)?.let {
                DeepLinkResult.CustomServerConfig(it)
            } ?: DeepLinkResult.Unknown
        SSO_LOGIN_DEEPLINK_HOST -> {
            when (uri.lastPathSegment) {
                SSO_LOGIN_FAILURE -> {
                    uri.getQueryParameter(SSO_LOGIN_ERROR_PARAM)?.let { value ->
                        DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByLabel(value))
                    } ?: DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown)
                }
                SSO_LOGIN_SUCCESS -> {
                    val cookie = uri.getQueryParameter(SSO_LOGIN_COOKIE_PARAM)
                    val location = uri.getQueryParameter(SSO_LOGIN_SERVER_CONFIG_PARAM)
                    if (cookie == null || location == null)
                        DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown)
                    else
                        DeepLinkResult.SSOLogin.Success(cookie, location)
                }
                else -> DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown)
            }
        }
        else -> DeepLinkResult.Unknown
    }

    companion object {
        const val ACCESS_DEEPLINK_HOST = "access"
        const val SERVER_CONFIG_PARAM = "config"
        const val SSO_LOGIN_DEEPLINK_HOST = "sso-login"
        const val SSO_LOGIN_FAILURE = "failure"
        const val SSO_LOGIN_SUCCESS = "success"
        const val SSO_LOGIN_USERID_PARAM = "userId"
        const val SSO_LOGIN_COOKIE_PARAM = "cookie"
        const val SSO_LOGIN_ERROR_PARAM = "error"
        const val SSO_LOGIN_SERVER_CONFIG_PARAM = "location"

    }
}

enum class SSOFailureCodes(val label: String, val errorCode: Int, val stringResource: Int) {
    ServerErrorUnsupportedSaml(
        "server-error-unsupported-saml",
        SSOServerErrorCode.SERVER_ERROR_UNSUPPORTED_SAML,
        R.string.sso_error_server_error_unsupported_saml
    ),
    BadSuccessRedirect("bad-success-redirect", SSOServerErrorCode.BAD_SUCCESS_REDIRECT, R.string.sso_error_bad_success_redirect),
    BadFailureRedirect("bad-failure-redirect", SSOServerErrorCode.BAD_FAILURE_REDIRECT, R.string.sso_error_bad_failure_redirect),
    BadUsername("bad-username", SSOServerErrorCode.BAD_USERNAME, R.string.sso_error_bad_username),
    BadUpstream("bad-upstream", SSOServerErrorCode.BAD_UPSTREAM, R.string.sso_error_bad_upstream),
    ServerError("server-error", SSOServerErrorCode.SERVER_ERROR, R.string.sso_error_server_error),
    NotFound("not-found", SSOServerErrorCode.NOT_FOUND, R.string.sso_error_not_found),
    Forbidden("forbidden", SSOServerErrorCode.FORBIDDEN, R.string.sso_error_forbidden),
    NoMatchingAuthReq("no-matching-auth-req", SSOServerErrorCode.NO_MATCHING_AUTH_REQ, R.string.sso_error_no_matching_auth_req),
    InsufficientPermissions(
        "insufficient-permissions",
        SSOServerErrorCode.INSUFFICIENT_PERMISSIONS, R.string.sso_error_insufficient_permissions
    ),
    Unknown("unknown", SSOServerErrorCode.UNKNOWN, R.string.sso_error_unknown);

    companion object {
        fun getByCode(errorCode: Int) = values().first { it.errorCode == errorCode }
        fun getByLabel(label: String) = values().first { it.label == label }
    }
}

class SSOServerErrorCode {
    companion object {
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




