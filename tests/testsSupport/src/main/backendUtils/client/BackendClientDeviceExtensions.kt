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
package backendUtils.client

import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import backendUtils.auth.getAuthToken
import kotlinx.coroutines.runBlocking
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONArray
import org.json.JSONObject
import user.utils.ClientUser
import java.net.HttpURLConnection
import java.net.URI

fun BackendClient.getBackendClientIds(forUser: ClientUser): List<String> {
    val token = runBlocking { getAuthToken(forUser) }
    val url = URI("clients".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token?.type} ${token?.value}")
        put("Accept", BackendClient.applicationJson)
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "GET",
        headers = headers,
        options = RequestOptions(
            accessToken = token,
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )

    val clients = JSONArray(response.body)
    return buildList {
        for (i in 0 until clients.length()) {
            add(clients.getJSONObject(i).getString("id"))
        }
    }
}

fun BackendClient.removeBackendClient(forUser: ClientUser, clientId: String) {
    val token = runBlocking { getAuthToken(forUser) }
    val url = URI("clients/$clientId".composeCompleteUrl()).toURL()

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token?.type} ${token?.value}")
        put("Accept", BackendClient.applicationJson)
    }

    NetworkBackendClient.makeRequest(
        url = url,
        method = "DELETE",
        body = JSONObject().apply {
            put("password", forUser.password)
        }.toString(),
        headers = headers,
        options = RequestOptions(
            accessToken = token,
            expectedResponseCodes = NumberSequence.Array(
                intArrayOf(
                    HttpURLConnection.HTTP_OK,
                    HttpURLConnection.HTTP_NO_CONTENT
                )
            )
        )
    )
}
