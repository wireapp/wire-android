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
import net.openid.appauth.connectivity.ConnectionBuilder
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest
import java.security.SecureRandom
import java.security.cert.X509Certificate
import javax.net.ssl.HostnameVerifier
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

class OAuthUseCase(context: Context, private val authUrl: String) {
    private var authState: AuthState = AuthState()
    private var authorizationService: AuthorizationService
    private lateinit var authServiceConfig: AuthorizationServiceConfiguration

    // todo: this is a temporary code to ignore ssl issues on the test environment, will be removed after the preparation of the environment
    // region Ignore SSL for OAuth
    val naiveTrustManager = object : X509TrustManager {
        override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        override fun checkClientTrusted(certs: Array<X509Certificate>, authType: String) = Unit
        override fun checkServerTrusted(certs: Array<X509Certificate>, authType: String) = Unit
    }
    val insecureSocketFactory = SSLContext.getInstance("SSL").apply {
        val trustAllCerts = arrayOf<TrustManager>(naiveTrustManager)
        init(null, trustAllCerts, SecureRandom())
    }.socketFactory

    private var insecureConnection = ConnectionBuilder() { uri ->
        val url = URL(uri.toString())
        val connection = url.openConnection() as HttpURLConnection
        if (connection is HttpsURLConnection) {
            connection.hostnameVerifier = HostnameVerifier { _, _ -> true }
            connection.sslSocketFactory = insecureSocketFactory
        }
        connection
    }
    // endregion

    private var appAuthConfiguration: AppAuthConfiguration = AppAuthConfiguration.Builder()
        .setBrowserMatcher(
            BrowserAllowList(
                VersionedBrowserMatcher.CHROME_CUSTOM_TAB, VersionedBrowserMatcher.SAMSUNG_CUSTOM_TAB
            )
        )
        .setConnectionBuilder(insecureConnection)
        .setSkipIssuerHttpsCheck(true)
        .build()

    init {
        authorizationService = AuthorizationService(context, appAuthConfiguration)
    }

    private fun getAuthorizationRequestIntent(): Intent = authorizationService.getAuthorizationRequestIntent(getAuthorizationRequest())

    fun launch(activityResultRegistry: ActivityResultRegistry, resultHandler: (OAuthResult) -> Unit) {
        val resultLauncher = activityResultRegistry.register(
            OAUTH_ACTIVITY_RESULT_KEY, ActivityResultContracts.StartActivityForResult()
        ) { result ->
            handleActivityResult(result, resultHandler)
        }
        AuthorizationServiceConfiguration.fetchFromUrl(
            Uri.parse(authUrl.plus(IDP_CONFIGURATION_PATH)),
            { configuration, ex ->
                if (ex == null) {
                    authServiceConfig = configuration!!
                    resultLauncher.launch(getAuthorizationRequestIntent())
                } else {
                    resultHandler(OAuthResult.Failed.InvalidActivityResult("Fetching the configurations failed! $ex"))
                }
            }, insecureConnection
        )
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
        val clientAuth: ClientAuthentication = ClientSecretBasic(CLIENT_SECRET)

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
                        appLogger.i("OAuth idToken: ${response.idToken}")
                        appLogger.i("OAuth refreshToken: ${response.refreshToken}")
                        resultHandler(OAuthResult.Success(response.idToken.toString(), response.refreshToken))
                    } else {
                        resultHandler(OAuthResult.Failed.EmptyResponse)
                    }
                }
            }
        } ?: resultHandler(OAuthResult.Failed.Unknown)
    }

    private fun getAuthorizationRequest() = AuthorizationRequest.Builder(
        authServiceConfig, CLIENT_ID, ResponseTypeValues.CODE, URL_AUTH_REDIRECT
    ).setCodeVerifier().setScopes(
        AuthorizationRequest.Scope.OPENID,
        AuthorizationRequest.Scope.EMAIL,
        AuthorizationRequest.Scope.PROFILE,
        AuthorizationRequest.Scope.OFFLINE_ACCESS
    ).build()

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
        data class Success(val idToken: String, val refreshToken: String?) : OAuthResult()
        open class Failed(val reason: String) : OAuthResult() {
            object Unknown : Failed("Unknown")
            class InvalidActivityResult(reason: String) : Failed(reason)
            object EmptyResponse : Failed("Empty Response")
        }
    }

    companion object {
        const val OAUTH_ACTIVITY_RESULT_KEY = "OAuthActivityResult"

        // todo: clientId and the clientSecret will be replaced with the values from the BE once the BE provides them
        const val CLIENT_ID = "wireapp"
        const val CLIENT_SECRET = "dUpVSGx2dVdFdGQ0dmsxWGhDalQ0SldU"
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
