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
package com.wire.android.ui.debugscreen

import android.app.Activity
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_CANCEL_CURRENT
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import com.auth0.android.jwt.JWT
import com.wire.android.appLogger
import com.wire.android.di.ApplicationScope
import com.wire.android.ui.WireActivity
import com.wire.android.util.extension.getActivity
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ResponseTypeValues
import net.openid.appauth.ResponseTypeValues.CODE
import net.openid.appauth.browser.BrowserAllowList
import net.openid.appauth.browser.VersionedBrowserMatcher
import java.security.MessageDigest
import java.security.SecureRandom
import com.wire.kalium.logic.feature.e2ei.EnrolE2EIUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

class OAuth {

    private var authState: AuthState = AuthState()
    private var jwt: JWT? = null

    private lateinit var authorizationService: AuthorizationService
    lateinit var authServiceConfig: AuthorizationServiceConfiguration


    fun init2(context: Context) {
        val authService = AuthorizationService(context)
        authServiceConfig = AuthorizationServiceConfiguration(
            Uri.parse(URL_AUTHORIZATION),  // authorization endpoint
            Uri.parse(URL_TOKEN_EXCHANGE)
        ) // token endpoint
        val authRequest = AuthorizationRequest.Builder(
            // OAuth 2.0 endpoint for Google's authorization server
            authServiceConfig,
            // Client ID registered with Google
            CLIENT_ID,
            // Response type, which should always be "code"
            CODE,
            // Redirect URI registered with Google
            Uri.parse(URL_AUTH_REDIRECT)
        )
            .setScope(SCOPE_OPENID)
            .build()
        authService.performAuthorizationRequest(
            authRequest, PendingIntent.getActivity(
                context,
                0,
                Intent(context.applicationContext, WireActivity::class.java),
                FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
        )
    }

    fun initAuthServiceConfig() {
        authServiceConfig = AuthorizationServiceConfiguration(
            Uri.parse(URL_AUTHORIZATION),
            Uri.parse(URL_TOKEN_EXCHANGE),
            null,
            Uri.parse(URL_LOGOUT)
        )
    }

    fun initAuthService(context: Context) {
        val appAuthConfiguration = AppAuthConfiguration.Builder()
            .setBrowserMatcher(
                BrowserAllowList(
                    VersionedBrowserMatcher.CHROME_CUSTOM_TAB,
                    VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
                )
            ).build()

        authorizationService = AuthorizationService(
            context,
            appAuthConfiguration
        )
    }


    fun attemptAuthorization(context: Context, enrolE2EIUseCase: EnrolE2EIUseCase, videModelScope: CoroutineScope) {
        val secureRandom = SecureRandom()
        val bytes = ByteArray(64)
        secureRandom.nextBytes(bytes)

        val encoding = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val codeVerifier = Base64.encodeToString(bytes, encoding)
        val digest = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)
        val hash = digest.digest(codeVerifier.toByteArray())
        val codeChallenge = Base64.encodeToString(hash, encoding)

        val builder = AuthorizationRequest.Builder(
            authServiceConfig,
            CLIENT_ID,
            CODE,
            Uri.parse(URL_AUTH_REDIRECT)
        )
            .setCodeVerifier(
                codeVerifier,
                codeChallenge,
                CODE_VERIFIER_CHALLENGE_METHOD
            )

        builder.setScopes(
            SCOPE_OPENID,
            SCOPE_EMAIL,
            SCOPE_PROFILE
        )
        val request = builder.build()
        appLogger.e("####### request details:  ${request.jsonSerializeString()}")

        val authIntent = authorizationService.getAuthorizationRequestIntent(request)

        resultLauncher = context.getActivity()!!.activityResultRegistry.register(
            "myActivityResultKey",
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            if (result.resultCode == Activity.RESULT_OK) {
                instance.handleAuthorizationResponse(result.data!!, enrolE2EIUseCase, videModelScope)
            }
        }
// Receiver
        resultLauncher.launch(authIntent)
    }

    private lateinit var resultLauncher: ActivityResultLauncher<Intent>


    fun handleAuthorizationResponse(intent: Intent, enrolE2EIUseCase: EnrolE2EIUseCase, videModelScope: CoroutineScope) {
        val authorizationResponse: AuthorizationResponse? = AuthorizationResponse.fromIntent(intent)
        val error = AuthorizationException.fromIntent(intent)

        authState = AuthState(authorizationResponse, error)
        appLogger.e("####### auth resp details:  ${authorizationResponse!!.jsonSerializeString()}")

        val tokenExchangeRequest = authorizationResponse!!.createTokenExchangeRequest()
        appLogger.e("####### tokenExchangeRequest:  ${tokenExchangeRequest.jsonSerializeString()}")
        authorizationService.performTokenRequest(tokenExchangeRequest) { response, exception ->
            if (exception != null) {
                authState = AuthState()
            } else {
                if (response != null) {
                    authState.update(response, exception)
                    jwt = JWT(response.idToken!!)
                    appLogger.e("####### idToken:  ${response.jsonSerializeString()}")
                    appLogger.e("####### idToken:  ${response.idToken}")
//                    videModelScope.launch {
//                        enrolE2EIUseCase.invoke(response.idToken.toString())
//                    }
                }
            }
            persistState()
        }
    }

    fun persistState() {
        appLogger.e("####### ${authState.jsonSerializeString()}")
    }


    companion object {
        public val instance = OAuth()

        val SHARED_PREFERENCES_NAME = "AUTH_STATE_PREFERENCE"
        val AUTH_STATE = "AUTH_STATE"

        val SCOPE_PROFILE = "profile"
        val SCOPE_EMAIL = "email"
        val SCOPE_OPENID = "openid"

        val DATA_PICTURE = "picture"
        val DATA_FIRST_NAME = "given_name"
        val DATA_LAST_NAME = "family_name"
        val DATA_EMAIL = "email"

        val CLIENT_ID = "338888153072-4fep6tn6k16tmcbhg4nt4lr65pv3avgi.apps.googleusercontent.com"
        val CODE_VERIFIER_CHALLENGE_METHOD = "S256"
        val MESSAGE_DIGEST_ALGORITHM = "SHA-256"

        val URL_AUTHORIZATION = "https://accounts.google.com/o/oauth2/auth"
        val URL_TOKEN_EXCHANGE = "https://oauth2.googleapis.com/token"
        val URL_AUTH_PROVIDER = "https://www.googleapis.com/oauth2/v1/cert"
        val URL_AUTH_REDIRECT = "com.wire.android.internal.debug:/oauth2redirect"
        val URL_API_CALL = "https://www.googleapis.com/drive/v2/files"
        val URL_LOGOUT = "https://accounts.google.com/o/oauth2/revoke?token="

        val URL_LOGOUT_REDIRECT = "com.wire.android.internal.debug:/logout"
    }
}
