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
package com.wire.android.feature.e2ei

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Base64
import androidx.activity.result.ActivityResult
import androidx.activity.result.ActivityResultRegistry
import androidx.activity.result.contract.ActivityResultContracts
import com.wire.android.appLogger
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.findParameterValue
import com.wire.android.util.removeQueryParams
import kotlinx.serialization.json.JsonObject
import net.openid.appauth.AppAuthConfiguration
import net.openid.appauth.AuthState
import net.openid.appauth.AuthorizationException
import net.openid.appauth.AuthorizationRequest
import net.openid.appauth.AuthorizationResponse
import net.openid.appauth.AuthorizationService
import net.openid.appauth.AuthorizationServiceConfiguration
import net.openid.appauth.ClientAuthentication
import net.openid.appauth.ResponseTypeValues
import org.json.JSONObject
import java.net.URI
import java.security.MessageDigest
import java.security.SecureRandom

class OAuthUseCase(
    context: Context,
    private val authUrl: String,
    private val claims: JsonObject,
    authState: AuthState = AuthState()
) {
    private var _authState: AuthState = authState

    private var authorizationService: AuthorizationService
    private lateinit var authServiceConfig: AuthorizationServiceConfiguration

    private var appAuthConfiguration: AppAuthConfiguration = AppAuthConfiguration.Builder()
        .build()

    init {
        authorizationService = AuthorizationService(context, appAuthConfiguration)
    }

    private fun getAuthorizationRequestIntent(clientId: String): Intent =
        authorizationService.getAuthorizationRequestIntent(getAuthorizationRequest(clientId))

    fun launch(
        activityResultRegistry: ActivityResultRegistry,
        forceLoginFlow: Boolean,
        resultHandler: (OAuthResult) -> Unit,
    ) {
        if (forceLoginFlow) {
            launchLoginFlow(activityResultRegistry, resultHandler)
        } else {
            _authState.performActionWithFreshTokens(authorizationService) { _, idToken, exception ->
                if (exception != null) {
                    appLogger.e(
                        message = "OAuthTokenRefreshManager: Error refreshing tokens, continue with login!",
                        throwable = exception
                    )
                    launchLoginFlow(activityResultRegistry, resultHandler)
                } else {
                    resultHandler(
                        OAuthResult.Success(
                            idToken.toString(),
                            _authState.jsonSerializeString()
                        )
                    )
                }
            }
        }
    }

    private fun launchLoginFlow(
        activityResultRegistry: ActivityResultRegistry,
        resultHandler: (OAuthResult) -> Unit
    ) {
        val resultLauncher = activityResultRegistry.register(
            OAUTH_ACTIVITY_RESULT_KEY,
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleActivityResult(result, resultHandler)
        }
        val clientId = URI(authUrl).findParameterValue(CLIENT_ID_QUERY_PARAM)

        AuthorizationServiceConfiguration.fetchFromUrl(
            Uri.parse(URI(authUrl).removeQueryParams().toString().plus(IDP_CONFIGURATION_PATH))
        ) { configuration, ex ->
            if (ex == null) {
                authServiceConfig = configuration!!
                clientId?.let {
                    resultLauncher.launch(getAuthorizationRequestIntent(it))
                }
            } else {
                resultHandler(OAuthResult.Failed.InvalidActivityResult("Fetching the configurations failed! $ex"))
            }
        }
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
        val clientAuth: ClientAuthentication = AuthState().clientAuthentication

        val error = AuthorizationException.fromIntent(intent)

        _authState = AuthState(authorizationResponse, error)

        val tokenExchangeRequest = authorizationResponse?.createTokenExchangeRequest()

        tokenExchangeRequest?.let { request ->
            authorizationService.performTokenRequest(request, clientAuth) { response, exception ->
                if (exception != null) {
                    _authState = AuthState()
                    resultHandler(OAuthResult.Failed(exception.toString()))
                } else {
                    if (response != null) {
                        _authState.update(response, exception)
                        appLogger.i("OAuth idToken: ${response.idToken}")
                        resultHandler(
                            OAuthResult.Success(
                                response.idToken.toString(),
                                _authState.jsonSerializeString()
                            )
                        )
                    } else {
                        resultHandler(OAuthResult.Failed.EmptyResponse)
                    }
                }
            }
        } ?: resultHandler(OAuthResult.Failed.Unknown)
    }

    private fun getAuthorizationRequest(clientId: String) = AuthorizationRequest.Builder(
        authServiceConfig,
        clientId,
        ResponseTypeValues.CODE,
        URL_AUTH_REDIRECT
    ).setCodeVerifier().setScopes(
        AuthorizationRequest.Scope.OPENID,
        AuthorizationRequest.Scope.EMAIL,
        AuthorizationRequest.Scope.PROFILE,
        AuthorizationRequest.Scope.OFFLINE_ACCESS
    ).setClaims(JSONObject(claims.toString()))
        .setPrompt(AuthorizationRequest.Prompt.LOGIN)
        .build()

    private fun AuthorizationRequest.Builder.setCodeVerifier(): AuthorizationRequest.Builder {
        val codeVerifier = getCodeVerifier()
        setCodeVerifier(
            codeVerifier,
            getCodeChallenge(codeVerifier),
            CODE_VERIFIER_CHALLENGE_METHOD
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
        data class Success(val idToken: String, val authState: String) : OAuthResult()
        open class Failed(val reason: String) : OAuthResult() {
            object Unknown : Failed("Unknown")
            class InvalidActivityResult(reason: String) : Failed(reason)
            object EmptyResponse : Failed("Empty Response")
        }
    }

    companion object {
        const val OAUTH_ACTIVITY_RESULT_KEY = "OAuthActivityResult"
        const val CLIENT_ID_QUERY_PARAM = "client_id"
        const val CODE_VERIFIER_CHALLENGE_METHOD = "S256"
        const val MESSAGE_DIGEST_ALGORITHM = "SHA-256"
        val MESSAGE_DIGEST = MessageDigest.getInstance(MESSAGE_DIGEST_ALGORITHM)
        const val ENCODING = Base64.URL_SAFE or Base64.NO_PADDING or Base64.NO_WRAP
        val URL_AUTH_REDIRECT: Uri = Uri.Builder().scheme(DeepLinkProcessor.DEEP_LINK_SCHEME)
            .authority(DeepLinkProcessor.E2EI_DEEPLINK_HOST)
            .appendPath(DeepLinkProcessor.E2EI_DEEPLINK_OAUTH_REDIRECT_PATH).build()

        const val IDP_CONFIGURATION_PATH = "/.well-known/openid-configuration"
    }
}
