@file:Suppress("MagicNumber", "PackageNaming", "TooGenericExceptionCaught", "TooGenericExceptionThrown")
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
package backendUtils.connection

import backendUtils.BackendClient
import backendUtils.auth.defaultheaders
import backendUtils.auth.getAuthToken
import kotlinx.coroutines.delay
import network.NetworkBackendClient
import network.RequestOptions
import org.json.JSONObject
import service.models.Connection
import user.utils.AccessToken
import user.utils.ClientUser
import java.net.URL

suspend fun BackendClient.sendConnectionRequest(fromUser: ClientUser, toUser: ClientUser) {
    val token = getAuthToken(fromUser)
    val url =
        URL("connections/${BackendClient.loadBackend(toUser.backendName.orEmpty()).domain}/${toUser.id}".composeCompleteUrl())

    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, "${token?.type} ${token?.value}")
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
    network.WireTestLogger.getLog("Backend").info("Response of send connection request is $response")
}

suspend fun BackendClient.acceptAllIncomingConnectionRequests(asUser: ClientUser) {
    updateConnections(asUser, ConnectionStatus.Pending, ConnectionStatus.Accepted, null)
}

suspend fun BackendClient.acceptIncomingConnectionRequest(asUser: ClientUser, fromUser: ClientUser) {
    updateConnections(
        asUser,
        ConnectionStatus.Pending,
        ConnectionStatus.Accepted,
        listOf(fromUser.id.orEmpty())
    )
}

private suspend fun BackendClient.updateConnections(
    asUser: ClientUser,
    srcStatus: ConnectionStatus,
    dstStatus: ConnectionStatus,
    forUserIds: List<String>? = null
) {
    getAllConnections(asUser)
        .filter { it.status == srcStatus && (forUserIds == null || forUserIds.contains(it.to)) }
        .forEach { connection ->
            try {
                changeConnectRequestStatus(asUser, connection.to.orEmpty(), connection.domain.orEmpty(), dstStatus)
            } catch (e: Exception) {
                throw RuntimeException("Failed to update connection for ${connection.to}", e)
            }
        }
}

private suspend fun BackendClient.getAllConnections(user: ClientUser): List<Connection> {
    var pagingState: String? = null
    val result = mutableListOf<Connection>()

    do {
        val connectionsInfo = getConnectionsInfo(getAuthToken(user), pagingState)
        val connections = connectionsInfo.getJSONArray("connections")

        for (i in 0 until connections.length()) {
            result.add(Connection.fromJSON(connections.getJSONObject(i)))
        }

        // Backward-compat with older backends
        if (connectionsInfo.has("paging_state")) {
            pagingState = connectionsInfo.getString("paging_state")
        }
    } while (connectionsInfo.getBoolean("has_more"))

    return result
}

private fun BackendClient.getConnectionsInfo(token: AccessToken?, pagingState: String?): JSONObject {
    val url = URL("list-connections".composeCompleteUrl())

    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, "${token?.type} ${token?.value}")
    }

    val requestBody = JSONObject().apply {
        put("paging_state", pagingState)
    }

    return try {
        val output = NetworkBackendClient.sendJsonRequestWithCookies(
            url = url,
            method = "POST",
            headers = headers,
            body = requestBody.toString(),
            options = RequestOptions(accessToken = token)
        )
        JSONObject(output.body)
    } catch (e: Exception) {
        e.printStackTrace()
        if (e.message?.contains("404") == true) {
            // Fallback for old backend
            val fallbackUrl = URL("connections".composeCompleteUrl())
            val output = NetworkBackendClient.sendJsonRequestWithCookies(
                url = fallbackUrl,
                method = "GET",
                headers = headers,
                options = RequestOptions(accessToken = token)
            )
            JSONObject(output.body)
        } else {
            throw RuntimeException("Failed to fetch connections info", e)
        }
    }
}

suspend fun BackendClient.changeConnectRequestStatus(
    asUser: ClientUser,
    connectionId: String,
    domain: String,
    newStatus: ConnectionStatus
) {
    val token = getAuthToken(asUser)
    val url = URL("connections/$domain/$connectionId".composeCompleteUrl())

    val headers = defaultheaders.toMutableMap().apply {
        put(BackendClient.AUTHORIZATION, "${token?.type} ${token?.value}")
    }

    val requestBody = JSONObject().apply {
        put("status", newStatus.toString())
    }

    try {
        NetworkBackendClient.sendJsonRequestWithCookies(
            url = url,
            method = "PUT",
            headers = headers,
            body = requestBody.toString(),
            options = RequestOptions(accessToken = token)
        )
    } catch (e: Exception) {
        if (e.message?.contains("404") == true) {
            // fallback for old backend
            val fallbackUrl = URL("connections/$connectionId".composeCompleteUrl())
            NetworkBackendClient.sendJsonRequestWithCookies(
                url = fallbackUrl,
                method = "PUT",
                headers = headers,
                body = requestBody.toString(),
                options = RequestOptions(accessToken = token)
            )
        } else {
            throw RuntimeException("Failed to change connection status for $connectionId", e)
        }
    }
}

enum class ConnectionStatus {
    Accepted, Blocked, Pending, Ignored, Sent, Cancelled;

    override fun toString(): String = name.lowercase()

    companion object {
        fun fromString(s: String): ConnectionStatus =
            entries.firstOrNull { it.name.equals(s, ignoreCase = true) }
                ?: throw IllegalArgumentException("Connection status '$s' is unknown")
    }
}
