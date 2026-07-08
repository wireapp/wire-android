@file:Suppress("MagicNumber", "PackageNaming")
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
package backendUtils.auth

import InbucketClient.getInbucketVerificationCode
import backendUtils.BackendClient
import network.NetworkBackendClient
import network.NetworkBackendClient.accessCredentials
import network.NetworkBackendClient.response
import network.NumberSequence
import network.RequestOptions
import org.json.JSONObject
import user.utils.AccessCredentials
import user.utils.AccessToken
import user.utils.ClientUser
import java.io.IOException
import java.net.URL

private fun jsonOf(vararg pairs: Pair<String, Any?>): JSONObject {
    return JSONObject().apply {
        pairs.forEach { (key, value) -> value?.let { put(key, it) } }
    }
}

suspend fun BackendClient.getAuthToken(user: ClientUser): AccessToken? {
    return getAuthCredentials(user).accessToken
}

private suspend fun BackendClient.getAuthCredentials(user: ClientUser): AccessCredentials {
    val credentials = user.accessCredentials
    return when {
        credentials == null -> login(user).also {
            user.accessCredentials = it
        }

        credentials.accessToken == null || credentials.accessToken.isInvalid() || credentials.accessToken.isExpired() ->
            access(credentials).also {
                user.accessCredentials = it
            }

        else -> credentials
    }
}

private suspend fun BackendClient.login(user: ClientUser): AccessCredentials {
    val connection = NetworkBackendClient.makeRequest(
        url = URL("login".composeCompleteUrl()),
        method = "POST",
        body = jsonOf(
            "email" to user.email,
            "password" to user.password,
            "label" to ""
        ).toString(),
        options = RequestOptions(expectedResponseCodes = NumberSequence.Array(intArrayOf(200, 403))),
        headers = defaultheaders,
    )

    return when (connection.responseCode) {
        403 -> {
            if (inbucketUrl.isBlank()) {
                throw IOException("Received 403 for 2FA but no inbucket url present - check your backend settings")
            }
            val verificationCode = getInbucketVerificationCode(
                user.email ?: throw IllegalArgumentException("No email tied to user")
            )
            val connection2fa = NetworkBackendClient.makeRequest(
                url = URL("login".composeCompleteUrl()),
                method = "POST",
                body = jsonOf(
                    "email" to user.email,
                    "password" to user.password,
                    "verification_code" to verificationCode
                ).toString(),
                headers = defaultheaders,
            )
            connection2fa.accessCredentials(connection2fa.response())
        }

        else -> connection.accessCredentials(connection.response())
    }
}

private fun BackendClient.access(credentials: AccessCredentials): AccessCredentials {
    val connection = NetworkBackendClient.makeRequest(
        url = URL("access".composeCompleteUrl()),
        method = "POST",
        body = jsonOf("withCredentials" to true).toString(),
        headers = defaultheaders,
        options = RequestOptions(
            accessToken = credentials.accessToken,
            cookie = credentials.accessCookie
        ),
    )
    return connection.accessCredentials(connection.response())
}

val defaultheaders = mapOf(
    "Accept" to BackendClient.applicationJson,
    BackendClient.contentType to BackendClient.applicationJson
)
