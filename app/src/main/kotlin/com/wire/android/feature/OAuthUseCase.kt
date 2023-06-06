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
import androidx.activity.result.contract.ActivityResultContracts
import com.wire.android.di.ApplicationScope
import com.wire.kalium.logic.feature.e2ei.EnrolE2EIUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.browser.BrowserAllowList
import net.openid.appauth.browser.VersionedBrowserMatcher
import java.security.MessageDigest
import java.security.SecureRandom
import javax.inject.Inject
import androidx.appcompat.app.AppCompatActivity


class OAuthUseCase @Inject constructor(
    private val enrolE2EIUseCase: EnrolE2EIUseCase,
    private val context: Context,
    @ApplicationScope private val coroutineScope: CoroutineScope
    ) {
    private var authState: AuthState = AuthState()
    private var authorizationService: AuthorizationService
    private var authServiceConfig: AuthorizationServiceConfiguration = AuthorizationServiceConfiguration(
        Uri.parse(URL_AUTHORIZATION), Uri.parse(URL_TOKEN_EXCHANGE), null, Uri.parse(URL_LOGOUT)
    )
    private var appAuthConfiguration: AppAuthConfiguration = AppAuthConfiguration.Builder().setBrowserMatcher(
        BrowserAllowList(
            VersionedBrowserMatcher.CHROME_CUSTOM_TAB, VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
        )
    ).build()

    init {
        authorizationService = AuthorizationService(
            context, appAuthConfiguration
        )
    }

    operator fun invoke(activity: AppCompatActivity) {
        val authIntent = authorizationService.getAuthorizationRequestIntent(getAuthorizationRequest())

        val resultLauncher = activity.activityResultRegistry.register(
            OAUTH_ACTIVITY_RESULT_KEY, ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                handleAuthorizationResponse(result.data!!)
            }
        }
        resultLauncher.launch(authIntent)
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


    fun handleAuthorizationResponse(intent: Intent) {
        val authorizationResponse: AuthorizationResponse? = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        authState = AuthState(authorizationResponse, error)

        val tokenExchangeRequest = authorizationResponse!!.createTokenExchangeRequest()
        authorizationService.performTokenRequest(tokenExchangeRequest) { response, exception ->
            if (exception != null) {
                authState = AuthState()
            } else {
                if (response != null) {
                    authState.update(response, exception)
                    coroutineScope.launch {
                        enrolE2EIUseCase.invoke(response.idToken.toString())
                    }
                    //todo: Mojtaba: we need to handle the error state and show something to the user
                }
            }
        }
    }

    companion object {
        val OAUTH_ACTIVITY_RESULT_KEY = "OAuthActivityResult"

        val SCOPE_PROFILE = "profile"
        val SCOPE_EMAIL = "email"
        val SCOPE_OPENID = "openid"

        val CLIENT_ID = "338888153072-4fep6tn6k16tmcbhg4nt4lr65pv3avgi.apps.googleusercontent.com"//todo mojtaba: configuration!
        val CODE_VERIFIER_CHALLENGE_METHOD = "S256"
        val MESSAGE_DIGEST_ALGORITHM = "SHA-256"
        val MESSAGE_DIGEST = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)
        val ENCODING = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP

        val URL_AUTHORIZATION = "https://accounts.google.com/o/oauth2/auth"//todo: all of these? configurable? maybe
        val URL_TOKEN_EXCHANGE = "https://oauth2.googleapis.com/token"
        val URL_AUTH_PROVIDER = "https://www.googleapis.com/oauth2/v1/cert"
        val URL_AUTH_REDIRECT = "com.wire.android.internal.debug:/oauth2redirect"
        val URL_LOGOUT = "https://accounts.google.com/o/oauth2/revoke?token="
    }
}
