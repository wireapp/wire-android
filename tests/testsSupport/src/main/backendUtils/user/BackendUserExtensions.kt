@file:Suppress("MagicNumber", "PackageNaming", "TooGenericExceptionCaught", "TooGenericExceptionThrown")
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
package backendUtils.user

import android.net.Uri
import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import backendUtils.auth.getAuthToken
import kotlinx.coroutines.runBlocking
import logger.WireTestLogger
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONObject
import user.utils.AccessCookie
import user.utils.AccessCredentials
import user.utils.ClientUser
import java.net.HttpCookie
import java.net.HttpURLConnection
import java.net.URI
import java.net.URL
import java.net.URLEncoder
import kotlin.time.Duration.Companion.seconds

fun BackendClient.createPersonalUserViaBackend(user: ClientUser): ClientUser {
    WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null").info("user is $user")
    val url = URL(backendUrl + "register")

    val requestBody = JSONObject().apply {
        put("email", user.email)
        put("name", user.name)
        put("password", user.password)
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = mapOf(BackendClient.contentType to BackendClient.applicationJson)
    )

    val json = JSONObject(response.body)
    user.id = json.getString("id")
    val accessCookie = AccessCookie("zuid", response.cookies)
    user.accessCredentials = AccessCredentials(null, accessCookie)

    val activationCode = getActivationCodeForEmail(user.email.orEmpty())
    WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null")
        .info("code is $activationCode")
    activateRegisteredEmailByBackendCode(user.email.orEmpty(), activationCode)

    return user
}

fun BackendClient.createWirelessUserViaBackend(user: ClientUser): ClientUser {
    val url = URL(backendUrl)

    val requestBody = JSONObject().apply {
        put("name", user.name)
    }

    if (user.expiresIn != null) {
        requestBody.put("expires_in", user.expiresIn!!.seconds)
    }

    val response = NetworkBackendClient.sendJsonRequest(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = mapOf(BackendClient.contentType to BackendClient.applicationJson)
    )

    val connection = url.openConnection() as HttpURLConnection
    val cookiesHeader = connection.getHeaderField("Set-Cookie")
    val cookies = HttpCookie.parse(cookiesHeader).toList()

    val json = JSONObject(response)
    user.id = json.getString("id")
    val accessCookie = AccessCookie("zuid", cookies)
    user.accessCredentials = AccessCredentials(null, accessCookie)

    val activationCode = getActivationCodeForEmail(user.email.orEmpty())
    WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null").info("code is $activationCode")
    activateRegisteredEmailByBackendCode(user.email.orEmpty(), activationCode)
    return user
}

fun BackendClient.getActivationCodeForEmail(email: String): String {
    val encodedEmail = URLEncoder.encode(email, "UTF-8")
    val url = URL("${backendUrl}i/users/activation-code?email=$encodedEmail")
    val headers = mapOf(
        BackendClient.AUTHORIZATION to basicAuth.getEncoded(),
        "Accept" to BackendClient.applicationJson
    )
    val response = NetworkBackendClient.sendJsonRequest(
        url = url,
        method = "GET",
        body = null,
        headers = headers
    )
    return JSONObject(response).getString("code")
}

fun BackendClient.trigger2FA(email: String) {
    val url = URL("${backendUrl}v5/verification-code/send")

    val requestBody = JSONObject().apply {
        put("action", "login")
        put("email", email)
    }

    val headers = mapOf(
        BackendClient.AUTHORIZATION to basicAuth.getEncoded(),
        BackendClient.applicationJson to BackendClient.applicationJson,
        "Accept" to BackendClient.applicationJson
    )

    try {
        NetworkBackendClient.sendJsonRequest(
            url = url,
            method = "POST",
            body = requestBody.toString(),
            headers = headers
        )
    } catch (e: Exception) {
        // Keep the previous behavior: HTTP 429 while requesting 2FA is tolerated here.
    }
}

fun BackendClient.getVerificationCode(user: ClientUser): String {
    trigger2FA(user.email.orEmpty())

    val encodedUserId = Uri.encode(user.id)
    val url = URL("${backendUrl}i/users/$encodedUserId/verification-code/login")

    val headers = mapOf(
        BackendClient.AUTHORIZATION to basicAuth.getEncoded(),
        "Accept" to BackendClient.applicationJson
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

fun BackendClient.activateRegisteredEmailByBackendCode(email: String, code: String): String {
    val url = URL("${backendUrl}activate")

    val requestBody = JSONObject().apply {
        put("email", email)
        put("code", code)
        put("dryrun", false)
    }
    WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null").info("JsonBody is $requestBody")

    NetworkBackendClient.sendJsonRequest(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = mapOf(
            BackendClient.contentType to BackendClient.applicationJson,
            "Accept" to BackendClient.applicationJson
        )
    )

    WireTestLogger.getLog(NetworkBackendClient::class.simpleName ?: "Null")
        .info("JsonBody response is $requestBody")

    return "Email Registered"
}

private fun BackendClient.getFeatureConfig(feature: String, user: ClientUser): JSONObject {
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

    return JSONObject(response.body)
}

fun BackendClient.getUserNameByID(domain: String, id: String, user: ClientUser): String {
    val token = runBlocking { getAuthToken(user) }

    val url = "users/$domain/$id/".composeCompleteUrl()
    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, "${token?.type} ${token?.value}")
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(url),
        method = "GET",
        headers = headers,
        options = RequestOptions(accessToken = token)
    )
    return JSONObject(response.body).getString("name")
}

fun BackendClient.isDevelopmentApiEnabled(user: ClientUser): Boolean {
    return getFeatureConfig("mls", user).get("status").equals("enabled")
}

suspend fun BackendClient.getPropertyValues(user: ClientUser): JSONObject {
    val token = getAuthToken(user)
    val url = URI("properties-values".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token?.type} ${token?.value}")
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "GET",
        headers = headers,
        body = null,
        options = RequestOptions(
            accessToken = token,
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_NOT_FOUND))
        )
    )

    return if (response.body.isNotEmpty()) {
        JSONObject(response.body)
    } else {
        JSONObject()
    }
}

suspend fun BackendClient.setPropertyValue(user: ClientUser, propertyKey: String, properties: String) {
    val url = "properties/$propertyKey".composeCompleteUrl()
    val token = getAuthToken(user)
    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token?.type} ${token?.value}")
    }
    NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(url),
        method = "PUT",
        headers = headers,
        body = properties,
        options = RequestOptions(
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )
}
