/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
@file:Suppress("TooGenericExceptionCaught")

package com.wire.android.testSupport.backendConnections

import CredentialsManager
import android.net.Uri
import com.wire.android.testSupport.BuildConfig
import com.wire.android.testSupport.backendConnections.team.defaultheaders
import com.wire.android.testSupport.backendConnections.team.getAuthToken
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import logger.WireTestLogger
import network.HttpRequestException
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONObject
import user.utils.AccessCookie
import user.utils.AccessCredentials
import user.utils.BasicAuth
import user.utils.ClientUser
import java.net.*
import java.util.logging.Logger
import javax.net.ssl.HttpsURLConnection
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager

@Suppress("LongParameterList", "MagicNumber")
class BackendClient(
    val name: String,
    val backendUrl: String,
    val webappUrl: String,
    val backendWebsocket: String,
    val basicAuth: BasicAuth,
    val inbucketAuth: BasicAuth,
    val domain: String,
    val deeplink: String,
    val inbucketUrl: String,
    val inbucketUsername: String,
    val inbucketPassword: String,
    val keycloakUrl: String,
    val acmeDiscoveryUrl: String,
    val k8sNamespace: String,
    val socksProxy: String,
    var proxy: Proxy? = if (socksProxy.isNotEmpty()) {

        Authenticator.setDefault(object : Authenticator() {
            override fun getPasswordAuthentication(): PasswordAuthentication {
                return PasswordAuthentication(
                    "qa",
                    BuildConfig.SOCKS_PROXY_PASSWORD_PASSWORD.toCharArray()
                )
            }
        })
        Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
    } else {
        null
    }
) {

    fun hasInbucketSetup() = inbucketUrl.isNotEmpty()

    companion object {
        private val log: Logger = Logger.getLogger(NetworkBackendClient::class.simpleName)

        const val PROFILE_PICTURE_JSON_ATTRIBUTE = "complete"
        const val PROFILE_PREVIEW_PICTURE_JSON_ATTRIBUTE = "preview"

        data class BackendSecrets(
            val backendUrl: String,
            val backendWebsocket: String,
            val webappUrl: String,
            val basicAuthUsername: String?,
            val basicAuthPassword: String?,
            val basicAuthGeneral: String,
            val inbucketUsername: String?,
            val inbucketPassword: String?,
            val domain: String,
            val deeplink: String,
            val inbucketUrl: String,
            val keycloakUrl: String,
            val acmeDiscoveryUrl: String,
            val k8sNamespace: String,
            val socksProxy: String
        )

        private fun loadSecrets(connectionName: String): BackendSecrets {
            fun field(name: String): String? =
                CredentialsManager.getSecretFieldValue("BACKENDCONNECTION_$connectionName", name.uppercase())

            return BackendSecrets(
                backendUrl = field("backendUrl") ?: "",
                backendWebsocket = field("backendWebsocket") ?: "",
                webappUrl = field("webappUrl") ?: "",
                basicAuthUsername = field("basicAuthUsername"),
                basicAuthPassword = field("basicAuthPassword"),
                basicAuthGeneral = field("basicAuth") ?: "",
                inbucketUsername = field("inbucketUsername"),
                inbucketPassword = field("inbucketPassword"),
                domain = field("domain") ?: "",
                deeplink = field("deeplink") ?: "",
                inbucketUrl = field("inbucketUrl") ?: "",
                keycloakUrl = field("keycloakUrl") ?: "",
                acmeDiscoveryUrl = field("acmeDiscoveryUrl") ?: "",
                k8sNamespace = field("k8sNamespace") ?: "",
                socksProxy = field("socksProxy") ?: ""
            )
        }

        private fun buildBasicAuth(username: String?, password: String?, fallback: String): BasicAuth {
            return if (!username.isNullOrEmpty() && !password.isNullOrEmpty()) {
                BasicAuth(username, password)
            } else {
                BasicAuth(fallback)
            }
        }

        fun getDefault(): BackendClient? {
            return loadBackend("Staging")
        }

        fun loadBackend(connectionName: String): BackendClient {
            val secrets = loadSecrets(connectionName)

            WireTestLogger.getLog("This CLasss").info(
                "Auths are ${secrets.basicAuthUsername} ${secrets.basicAuthPassword} " +
                        "${secrets.basicAuthGeneral} ${secrets.inbucketUsername}"
            )

            return BackendClient(
                name = connectionName,
                backendUrl = secrets.backendUrl,
                webappUrl = secrets.webappUrl,
                backendWebsocket = secrets.backendWebsocket,
                basicAuth = buildBasicAuth(
                    secrets.basicAuthUsername,
                    secrets.basicAuthPassword,
                    secrets.basicAuthGeneral
                ),
                inbucketAuth = buildBasicAuth(
                    secrets.inbucketUsername,
                    secrets.inbucketPassword,
                    secrets.basicAuthGeneral
                ),
                domain = secrets.domain,
                deeplink = secrets.deeplink,
                inbucketUrl = secrets.inbucketUrl,
                inbucketUsername = secrets.inbucketUsername.orEmpty(),
                inbucketPassword = secrets.inbucketPassword.orEmpty(),
                keycloakUrl = secrets.keycloakUrl,
                acmeDiscoveryUrl = secrets.acmeDiscoveryUrl,
                k8sNamespace = secrets.k8sNamespace,
                socksProxy = secrets.socksProxy
            )
        }
    }

    init {
        val trustAllCerts = arrayOf<TrustManager>(
            object : X509TrustManager, TrustManager {
                override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate>? = null
                override fun checkClientTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {
                    true
                }

                override fun checkServerTrusted(certs: Array<java.security.cert.X509Certificate>, authType: String) {
                    true
                }
            }
        )
        if (!socksProxy.isNullOrEmpty()) {
            Authenticator.setDefault(object : Authenticator() {
                override fun getPasswordAuthentication(): PasswordAuthentication {
                    return PasswordAuthentication(
                        "qa",
                        BuildConfig.SOCKS_PROXY_PASSWORD_PASSWORD?.toCharArray()
                    )
                }
            })
            this.proxy = Proxy(Proxy.Type.SOCKS, InetSocketAddress("socks.wire.link", 1080))
        }
        try {
            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())
            HttpsURLConnection.setDefaultSSLSocketFactory(sslContext.socketFactory)
        } catch (e: Exception) {
            log.severe("Could not install all-trusting trust manager: ${e.message}")
        }
    }

    fun createPersonalUserViaBackend(user: ClientUser): ClientUser {
        WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null").info("user is $user")
        val url = URL(this.backendUrl + "register")

        val requestBody = JSONObject().apply {
            put("email", user.email)
            put("name", user.name)
            put("password", user.password)
        }

        val headers = mapOf("Content-Type" to "application/json")

        val response = NetworkBackendClient.sendJsonRequestWithCookies(
            url = url,
            method = "POST",
            body = requestBody.toString(),
            headers = headers
        )

        val json = JSONObject(response.body)
        user.id = json.getString("id")
        val accessCookie = AccessCookie("zuid", response.cookies)
        user.accessCredentials = AccessCredentials(null, accessCookie)

        val activationCode = getActivationCodeForEmail(
            user.email.orEmpty()
        )
        WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null")
            .info("code is $activationCode")
        activateRegisteredEmailByBackendCode(
            user.email.orEmpty(),
            activationCode
        )

        return user
    }

    fun createWirelessUserViaBackend(user: ClientUser): ClientUser {
        val url = URL(this.backendUrl)

        val requestBody = JSONObject().apply {
            put("name", user.name)
        }

        if (user.expiresIn != null) {
            requestBody.put("expires_in", user.expiresIn!!.seconds)
        }

        val headers = mapOf("Content-Type" to "application/json")

        val response = NetworkBackendClient.sendJsonRequest(
            url = url,
            method = "POST",
            body = requestBody.toString(),
            headers = headers
        )

        val connection = url.openConnection() as HttpURLConnection
        val cookiesHeader = connection.getHeaderField("Set-Cookie")
        val cookies = HttpCookie.parse(cookiesHeader)

        val json = JSONObject(response)
        user.id = json.getString("id")
        val accessCookie = AccessCookie("zuid", cookies)
        user.accessCredentials = AccessCredentials(null, accessCookie)

        val activationCode = getActivationCodeForEmail(
            user.email.orEmpty()
        )
        WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null").info("code is $activationCode")
        activateRegisteredEmailByBackendCode(user.email.orEmpty(), activationCode)
        return user
    }

    fun getActivationCodeForEmail(email: String): String {
        val encodedEmail = URLEncoder.encode(email, "UTF-8")
        val url = URL("${backendUrl}i/users/activation-code?email=$encodedEmail")
        val headers = mapOf(
            "Authorization" to basicAuth.getEncoded(),
            "Accept" to "application/json"
        )
        val response = NetworkBackendClient.sendJsonRequest(
            url = url,
            method = "GET",
            body = null,
            headers = headers
        )
        return JSONObject(response).getString("code")
    }

    @Suppress("TooGenericExceptionCaught")
    fun trigger2FA(email: String) {
        val url = URL("${backendUrl}v5/verification-code/send")

        val requestBody = JSONObject().apply {
            put("action", "login")
            put("email", email)
        }

        val headers = mapOf(
            "Authorization" to basicAuth.getEncoded(),
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )

        try {
            NetworkBackendClient.sendJsonRequest(
                url = url,
                method = "POST",
                body = requestBody.toString(),
                headers = headers
            )
        } catch (e: Exception) {
            // Optional: Allow HTTP 429 as acceptable like in Java
        }
    }

    @Suppress("TooGenericExceptionThrown")
    fun getVerificationCode(user: ClientUser): String {
        trigger2FA(user.email.orEmpty())

        val encodedUserId = Uri.encode(user.id)
        val url = URL("${backendUrl}i/users/$encodedUserId/verification-code/login")

        val headers = mapOf(
            "Authorization" to basicAuth.getEncoded(),
            "Accept" to "application/json"
        )

        return try {
            val response = NetworkBackendClient.sendJsonRequest(
                url = url,
                method = "GET",
                headers = headers,
                options = RequestOptions(
                    expectedResponseCodes = NumberSequence.Range(200..299)
                )
            )
            response.replace("\"", "")
        } catch (e: Exception) {
            throw RuntimeException("Failed to get verification code: ${e.message}", e)
        }
    }

    // Used to active a register user email
    fun activateRegisteredEmailByBackendCode(email: String, code: String): String {

        val url = URL("${backendUrl}activate")

        val requestBody = JSONObject().apply {
            put("email", email)
            put("code", code)
            put("dryrun", false)
        }
        WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null").info("JsonBody is $requestBody")

        val headers = mapOf(
            "Content-Type" to "application/json",
            "Accept" to "application/json"
        )

        val response = NetworkBackendClient.sendJsonRequest(
            url = url,
            method = "POST",
            body = requestBody.toString(),
            headers = headers
        )

        WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null")
            .info("JsonBody response is $requestBody")

        return "Email Registered"
    }

    private fun getFeatureConfig(feature: String, user: ClientUser): JSONObject {
        val token = runBlocking {
            getAuthToken(user)
        }
        val url = URL(String.format("feature-configs/%s", feature).composeCompleteUrl())

        val headers = defaultheaders.toMutableMap().apply {
            put("Authorization", "${token?.type} ${token?.value}")
        }

        val response = NetworkBackendClient.sendJsonRequestWithCookies(
            url = url,
            method = "GET",
            headers = headers,
            options = RequestOptions(
                accessToken = token
            )
        )

        val objectResponse = JSONObject(response.body)
        return objectResponse
    }

    suspend fun sendConnectionRequest(fromUser: ClientUser, toUser: ClientUser) {
        val token = getAuthToken(fromUser)
        val url =
            URL("connections/${BackendClient.loadBackend(toUser.backendName.orEmpty()).domain}/${toUser.id}".composeCompleteUrl())

        val headers = defaultheaders.toMutableMap().apply {
            put("Authorization", "${token?.type} ${token?.value}")
        }

        // First try the new endpoint
        val response = try {
            NetworkBackendClient.sendJsonRequestWithCookies(
                url = url,
                method = "POST",
                headers = headers,
                options = RequestOptions(accessToken = token)
            )
        } catch (e: Exception) {
            if (e.message?.contains("404") == true) {
                // Fallback to old endpoint
                val fallbackUrl = URL("connections".composeCompleteUrl())
                val requestBody = JSONObject().apply {
                    put("user", toUser.id)
                    put("name", toUser.name)
                    put("message", "This message is not shown anywhere anymore")
                }

                NetworkBackendClient.sendJsonRequestWithCookies(
                    url = fallbackUrl,
                    method = "POST",
                    headers = headers,
                    body = requestBody.toString(),
                    options = RequestOptions(accessToken = token)
                )
            } else {
                // Retry after delay for other errors
                delay(1500)
                try {
                   NetworkBackendClient.sendJsonRequestWithCookies(
                        url = url,
                        method = "POST",
                        headers = headers,
                        options = RequestOptions(accessToken = token)
                    )
                } catch (e: Exception) {
                    throw RuntimeException("Connection request failed with status code ${e.message}")

                }
            }
        }
network.WireTestLogger.getLog("Backend").info("Response of send connection request is ${response}")
    }

    suspend fun updateUniqueUsername(user: ClientUser, newUniqueUsername: String) {
        val USERNAME_ALREADY_REGISTERED_ERROR = 409
        val tryAvoidDuplicates = newUniqueUsername.equals(user.uniqueUsername, ignoreCase = true)
        var currentUsername = newUniqueUsername
        var attempts = 0

        while (true) {
            try {
                updateSelfHandle(user, currentUsername)
                user.uniqueUsername = currentUsername
                return
            } catch (e: HttpRequestException) {
                if (tryAvoidDuplicates && e.returnCode == USERNAME_ALREADY_REGISTERED_ERROR && attempts < 5) {
                    currentUsername = ClientUser.sanitizedRandomizedHandle(user.firstName)
                } else {
                    throw e
                }
            }
            attempts++
        }
    }

//    fun getSelfDeletingMessagesSettings(teamMember: ClientUser): JSONObject {
//        val teamId = Uri.encode(getTeamId(teamMember))
//        val url = "i/teams/$teamId/features/selfDeletingMessages".composeCompleteUrl()
//
//        val headers = defaultheaders.toMutableMap().apply {
//            put("Authorization", "${basicAuth.getEncoded()}")
//        }
//
//        val response = NetworkBackendClient.sendJsonRequestWithCookies(
//            url = URL(url),
//            method = "GET",
//            headers = headers,
//            options = RequestOptions()
//        )
//
//        return JSONObject(response.body)
//    }

    fun getUserNameByID(domain: String, id: String, user: ClientUser): String {
        val token = runBlocking { getAuthToken(user) }

        val url = "users/$domain/$id/".composeCompleteUrl()
        val headers = defaultheaders.toMutableMap().apply {
            put("Authorization", "${token?.type} ${token?.value}")
        }

        val response = NetworkBackendClient.sendJsonRequestWithCookies(
            url = URL(url),
            method = "GET",
            headers = headers,
            options = RequestOptions(accessToken = token)
        )

        return JSONObject(response.body).getString("name")
    }


    private suspend fun updateSelfHandle(user: ClientUser, handle: String) {
        val token = getAuthToken(user)
        val url = URL("self/handle".composeCompleteUrl())

        val headers = defaultheaders.toMutableMap().apply {
            put("Authorization", "${token?.type} ${token?.value}")
        }

        val requestBody = JSONObject().apply {
            put("handle", handle)
        }

        NetworkBackendClient.sendJsonRequestWithCookies(
            url = url,
            method = "PUT",
            headers = headers,
            body = requestBody.toString(),
            options = RequestOptions(accessToken = token)
        )
    }

    fun isDevelopmentApiEnabled(user: ClientUser): Boolean {

        return getFeatureConfig("mls", user).get("status").equals("enabled")
    }

    fun String.composeCompleteUrl(): String {
        return "${backendUrl}$this"
    }
}
