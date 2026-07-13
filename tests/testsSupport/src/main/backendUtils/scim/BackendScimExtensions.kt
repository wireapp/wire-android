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
package backendUtils.scim

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
import java.net.URL

/**
 * Creates a SCIM auth token for the given backend user so tests can call SCIM endpoints on that team.
 */
fun BackendClient.createScimAccessToken(asUser: ClientUser, description: String): String {
    val token = runBlocking { getAuthToken(asUser) }
    val url = URL("scim/auth-tokens".composeCompleteUrl())

    val requestBody = JSONObject().apply {
        put("description", description)
        put("password", asUser.password)
        asUser.verificationCode?.let { put("verification_code", it) }
    }

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token?.type} ${token?.value}")
        put("Accept", BackendClient.applicationJson)
        put("Content-Type", BackendClient.applicationJson)
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = headers,
        options = RequestOptions(
            accessToken = token,
            expectedResponseCodes = NumberSequence.Array(intArrayOf(HttpURLConnection.HTTP_OK))
        )
    )

    return JSONObject(response.body).getString("token")
}
