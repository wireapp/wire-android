@file:Suppress("MagicNumber", "PackageNaming")
/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package backendUtils.sso

import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import backendUtils.auth.getAuthToken
import kotlinx.coroutines.runBlocking
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONObject
import user.utils.ClientUser
import java.net.HttpURLConnection
import java.net.URI

fun BackendClient.createIdentityProvider(user: ClientUser, metadata: String): String {
    return createIdentityProviderAtPath(
        user = user,
        metadata = metadata,
        path = "identity-providers"
    )
}

// Creates an identity provider through the backend v2 API and returns its id.
fun BackendClient.createIdentityProviderV2(user: ClientUser, metadata: String): String {
    return createIdentityProviderAtPath(
        user = user,
        metadata = metadata,
        path = "v5/identity-providers?api_version=v2"
    )
}

private fun BackendClient.createIdentityProviderAtPath(
    user: ClientUser,
    metadata: String,
    path: String
): String {
    val token = runBlocking { getAuthToken(user) }
    val url = URI(path.composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token?.type} ${token?.value}")
        put("Accept", BackendClient.applicationJson)
        put("Content-Type", APPLICATION_XML)
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        body = metadata,
        headers = headers,
        options = RequestOptions(
            accessToken = token,
            expectedResponseCodes = NumberSequence.Array(
                intArrayOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED)
            )
        )
    )

    val responseBody = JSONObject(response.body)
    return responseBody.getString("id")
}

private const val APPLICATION_XML = "application/xml"
