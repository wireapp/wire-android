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
 */
package com.wire.android.feature

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import android.util.Log
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ClientSecretBasic
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.browser.BrowserAllowList
import net.openid.appauth.browser.VersionedBrowserMatcher
import java.security.MessageDigest
import java.security.SecureRandom

class OAuthUseCase(context: Context, authUrl: String) {
    private var authState: AuthState = AuthState()
    private var authorizationService: AuthorizationService
    private var authServiceConfig: AuthorizationServiceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse(authUrl + URL_AUTHORIZATION), Uri.parse(authUrl + URL_TOKEN_EXCHANGE), null, Uri.parse(URL_LOGOUT)
    )
    val openIdConfigurationUrl = "https://idp.hogwash.work:5556/dex/.well-known/openid-configuration"

    private var appAuthConfiguration: AppAuthConfiguration = AppAuthConfiguration.Builder().setBrowserMatcher(
        BrowserAllowList(
            VersionedBrowserMatcher.CHROME_CUSTOM_TAB, VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
        )
    ).build()

    init {
        authorizationService = AuthorizationService(context, appAuthConfiguration)

    }

    private fun getAuthorizationRequestIntent(): Intent =
        authorizationService.getAuthorizationRequestIntent(getAuthorizationRequest())

    fun launch(activityResultRegistry: ActivityResultRegistry, resultHandler: (OAuthResult) -> Unit) {
        val resultLauncher = activityResultRegistry.register(
            OAUTH_ACTIVITY_RESULT_KEY, ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleActivityResult(result, resultHandler)
        }
//        AuthorizationServiceConfiguration.fetchFromUrl(
//            Uri.parse(openIdConfigurationUrl)
//        ) { configuration, ex ->
//            if (ex == null) {
//                // Configuration fetched successfully, you can now use it
//                // for your OAuth 2.0 authorization flows
//                val authorizationEndpoint = configuration?.authorizationEndpoint.toString()
//                val tokenEndpoint = configuration?.tokenEndpoint.toString()
//
//                // Now you can proceed with the authorization process
//                authServiceConfig = configuration!!
//            } else {
//                // Handle the error if configuration fetching failed
//                // Examine the 'ex' parameter for details about the error
//            }
//        }

        resultLauncher.launch(getAuthorizationRequestIntent())
    }

    private fun handleActivityResult(result: ActivityResult, resultHandler: (OAuthResult) -> Unit) {
        if (result.resultCode == Activity.RESULT_OK) {
            handleAuthorizationResponse(result.data!!, resultHandler)
        } else {
            resultHandler(OAuthResult.Failed.InvalidActivityResult(result.toString()))
        }
    }

    private fun handleAuthorizationResponse(intent: Intent, resultHandler: (OAuthResult) -> Unit) {
        val authorizationResponse: AuthorizationResponse? = AuthorizationResponse.fromIntent(intent)
        val clientAuth: ClientAuthentication = ClientSecretBasic("dUpVSGx2dVdFdGQ0dmsxWGhDalQ0SldU")

        val error = AuthorizationException.fromIntent(intent)

        authState = AuthState(authorizationResponse, error)

        val tokenExchangeRequest = authorizationResponse?.createTokenExchangeRequest()
        tokenExchangeRequest?.let { request ->

            authorizationService.performTokenRequest(request, clientAuth) { response, exception ->
                if (exception != null) {
                    authState = AuthState()
                    resultHandler(OAuthResult.Failed(exception.toString()))
                } else {
                    if (response != null) {
                        authState.update(response, exception)
                        Log.e("idToken", response.idToken.toString())
                        resultHandler(OAuthResult.Success(response.idToken.toString()))
                    } else {
                        resultHandler(OAuthResult.Failed.EmptyResponse)
                    }
                }
            }
        } ?: resultHandler(OAuthResult.Failed.Unknown)
    }

    private fun getAuthorizationRequest() = AuthorizationRequest.Builder(
        authServiceConfig, CLIENT_ID, ResponseTypeValues.CODE, Uri.parse(URL_AUTH_REDIRECT)
    ).setCodeVerifier().setScopes(SCOPE_OPENID, SCOPE_EMAIL, SCOPE_PROFILE).build()

    private fun AuthorizationRequest.Builder.setCodeVerifier(): AuthorizationRequest.Builder {
        val codeVerifier = getCodeVerifier()
        setCodeVerifier(
            codeVerifier, getCodeChallenge(codeVerifier), CODE_VERIFIER_CHALLENGE_METHOD
        )
        return this
    }

    @Suppress("MagicNumber")
    private fun getCodeVerifier(): String {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)
        return Base64.encodeToString(bytes, ENCODING)
    }

    private fun getCodeChallenge(codeVerifier: String): String {
        val hash = MESSAGE_DIGEST.digest(codeVerifier.toByteArray())
        return Base64.encodeToString(hash, ENCODING)
    }

    sealed class OAuthResult {
        data class Success(val idToken: String) : OAuthResult()
        open class Failed(val reason: String) : OAuthResult() {
            object Unknown : Failed("Unknown")
            class InvalidActivityResult(reason: String) : Failed(reason)
            object EmptyResponse : Failed("Empty Response")
        }
    }

    companion object {
        const val OAUTH_ACTIVITY_RESULT_KEY = "OAuthActivityResult"

        const val SCOPE_PROFILE = "profile"
        const val SCOPE_EMAIL = "email"
        const val SCOPE_OPENID = "openid"

        const val CLIENT_ID = "wireapp"
        const val CODE_VERIFIER_CHALLENGE_METHOD = "S256"
        const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"
        val MESSAGE_DIGEST = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)
        const val ENCODING = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

        const val URL_AUTHORIZATION = "/auth"
        const val URL_TOKEN_EXCHANGE = "/token"
        const val URL_AUTH_REDIRECT = "com.wire.android.internal.debug:/oauth2redirect"
        const val URL_LOGOUT = "https://accounts.google.com/o/oauth2/revoke?token="
    }
}
