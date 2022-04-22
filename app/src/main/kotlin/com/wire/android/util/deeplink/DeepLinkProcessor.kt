package com.wire.android.util.deeplink

import android.net.Uri
import com.wire.android.R

sealed class DeepLinkResult{
    object Unknown: DeepLinkResult()
    data class CustomServerConfig(val url:String): DeepLinkResult()
    sealed class SSOLogin: DeepLinkResult(){
        data class Success(val cookie: String, val serverConfigId: String): SSOLogin()
        data class Failure(val ssoError: SSOFailureCodes): SSOLogin()
    }
}

class DeepLinkProcessor{
    operator fun invoke(uri: Uri): DeepLinkResult = when(uri.host){
        ACCESS_DEEPLINK_HOST ->
            uri.getQueryParameter(SERVER_CONFIG_PARAM)?.let {
                DeepLinkResult.CustomServerConfig(it)
            }?: DeepLinkResult.Unknown
        SSO_LOGIN_DEEPLINK_HOST -> {
            when(uri.lastPathSegment){
                SSO_LOGIN_FAILURE ->{
                    uri.getQueryParameter(SSO_LOGIN_ERROR_PARAM)?.let { value ->
                        DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.getByLabel(value))
                    }?: DeepLinkResult.SSOLogin.Failure(SSOFailureCodes.Unknown)
                }
                SSO_LOGIN_SUCCESS -> {
                    val cookie = uri.getQueryParameter(SSO_LOGIN_COOKIE_PARAM)
                    val location = uri.getQueryParameter(SSO_LOGIN_SERVER_CONFIG_PARAM)
                    if(cookie==null || location==null)
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

enum class SSOFailureCodes(val label:String, val errorCode: Int,val stringResource: Int){
    ServerErrorUnsupportedSaml("server-error-unsupported-saml",ERROR_CODE_ServerErrorUnsupportedSaml, R.string.sso_error_server_error_unsupported_saml),
    BadSuccessRedirect("bad-success-redirect",ERROR_CODE_BadSuccessRedirect, R.string.sso_error_bad_success_redirect),
    BadFailureRedirect("bad-failure-redirect",ERROR_CODE_BadFailureRedirect, R.string.sso_error_bad_failure_redirect),
    BadUsername("bad-username",ERROR_CODE_BadUsername, R.string.sso_error_bad_username),
    BadUpstream("bad-upstream",ERROR_CODE_BadUpstream, R.string.sso_error_bad_upstream),
    ServerError("server-error",ERROR_CODE_ServerError, R.string.sso_error_server_error),
    NotFound("not-found",ERROR_CODE_NotFound, R.string.sso_error_not_found),
    Forbidden("forbidden",ERROR_CODE_Forbidden, R.string.sso_error_forbidden),
    NoMatchingAuthReq("no-matching-auth-req",ERROR_CODE_NoMatchingAuthReq, R.string.sso_error_no_matching_auth_req),
    InsufficientPermissions("insufficient-permissions",ERROR_CODE_InsufficientPermissions, R.string.sso_error_insufficient_permissions),
    Unknown("unknown",ERROR_CODE_Unknown, R.string.sso_error_unknown);
    companion object {
        fun getByCode(errorCode: Int) = values().first { it.errorCode == errorCode }
        fun getByLabel(label: String) = values().first { it.label == label }
    }
}

private const val ERROR_CODE_ServerErrorUnsupportedSaml=1
private const val ERROR_CODE_BadSuccessRedirect=2
private const val ERROR_CODE_BadFailureRedirect=3
private const val ERROR_CODE_BadUsername=4
private const val ERROR_CODE_BadUpstream=5
private const val ERROR_CODE_ServerError=6
private const val ERROR_CODE_NotFound=7
private const val ERROR_CODE_Forbidden=8
private const val ERROR_CODE_NoMatchingAuthReq=9
private const val ERROR_CODE_InsufficientPermissions=10
private const val ERROR_CODE_Unknown=0



