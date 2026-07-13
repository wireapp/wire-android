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
import network.NetworkBackendClient
import network.NumberSequence
import network.RequestOptions
import org.json.JSONObject
import user.utils.ClientUser
import java.net.HttpURLConnection
import java.net.URL

// SCIM provisioning client used to create managed users in Wire after IdP credentials are prepared.
class ScimClient(
    private val backend: BackendClient
) {
    private var scimAuthToken: String? = null

    fun insert(asUser: ClientUser, userToCreate: ClientUser): String {
        val profile = JSONObject().apply {
            put("externalId", userToCreate.email)
            put("userName", userToCreate.uniqueUsername)
            put("displayName", userToCreate.name)
        }

        return createProfile(asUser, profile)
    }

    private fun createProfile(asUser: ClientUser, profile: JSONObject): String {
        val token = scimAuthToken ?: backend.createScimAccessToken(asUser, TOKEN_DESCRIPTION).also {
            scimAuthToken = it
        }

        val response = NetworkBackendClient.sendJsonRequest(
            url = URL("${backend.backendUrl}scim/v2/Users"),
            method = "POST",
            body = profile.toString(),
            headers = mapOf(
                "Authorization" to "Bearer $token",
                "Content-Type" to CONTENT_TYPE_SCIM_JSON,
                "Accept" to BackendClient.applicationJson
            ),
            options = RequestOptions(
                expectedResponseCodes = NumberSequence.Array(
                    intArrayOf(HttpURLConnection.HTTP_OK, HttpURLConnection.HTTP_CREATED)
                ),
                proxy = backend.proxy
            )
        )

        return JSONObject(response).getString("id")
    }

    private companion object {
        const val CONTENT_TYPE_SCIM_JSON = "application/scim+json"
        const val TOKEN_DESCRIPTION = "SCIM"
    }
}
