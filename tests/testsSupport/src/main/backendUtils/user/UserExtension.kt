@file:Suppress("PackageNaming")
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

import backendUtils.BackendClient
import backendUtils.team.defaultheaders
import backendUtils.team.getAuthToken
import kotlinx.coroutines.runBlocking
import logger.WireTestLogger
import network.NetworkBackendClient
import network.RequestOptions
import org.json.JSONObject
import user.utils.ClientUser
import java.net.URL

fun ClientUser.deleteUser(backend: BackendClient) {
    val token = runBlocking {
        backend.getAuthToken(this@deleteUser)
    }
    NetworkBackendClient.sendJsonRequest(
        url = with(backend) { URL("self".composeCompleteUrl()) },
        method = "DELETE",
        body = JSONObject().apply {
            put("password", password)
        }.toString(),
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie
        ),

        headers = defaultheaders.toMutableMap().apply {
            this.put("Authorization", "Bearer $token")
        },
    )
}

fun ClientUser.triggerDeleteEmail(backend: BackendClient) {
    val connection = NetworkBackendClient.makeRequest(
        url = with(backend) { URL("self".composeCompleteUrl()) },
        method = "DELETE",
        body = JSONObject(),
        options = RequestOptions(
            accessToken = accessCredentials?.accessToken,
            cookie = accessCredentials?.accessCookie
        ),

        headers = defaultheaders,
    )
    WireTestLogger.getLog("UserClient").info(connection.responseMessage)
}
