@file:Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown")
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
package service

import backendUtils.BackendClient
import backendUtils.team.defaultheaders
import backendUtils.team.getAuthToken
import com.wire.android.testSupport.backendConnections.team.Team
import network.NetworkBackendClient
import org.json.JSONArray
import org.json.JSONObject
import user.utils.ClientUser
import java.net.URI

suspend fun BackendClient.createChannelTeamConversation(
    user: ClientUser,
    conversationName: String?,
    team: Team
): String {
    val token = getAuthToken(user)
    val url = URI("conversations".composeCompleteUrl()).toURL()

    val requestBody = JSONObject().apply {
        put("users", JSONArray())
        put("qualified_users", JSONArray())
        put("conversation_role", "wire_member")
        conversationName?.let { put("name", it) }
        put(
            "team",
            JSONObject().apply {
                put("teamid", team.id)
                put("managed", false)
            }
        )
        put(
            "access",
            JSONArray().apply {
                listOf("invite", "code").forEach { put(it) }
            }
        )
        put(
            "access_role_v2",
            JSONArray().apply {
                listOf("team_member", "non_team_member", "guest", "service").forEach { put(it) }
            }
        )
        put("protocol", "mls")
        put("group_conv_type", "channel")
    }

    val response = NetworkBackendClient.sendJsonRequest(
        url = url,
        method = "POST",
        body = requestBody.toString(),
        headers = defaultheaders.toMutableMap().apply {
            put("Authorization", "Bearer ${token?.value}")
        }
    )
    return JSONObject(response).getString("id")
}
