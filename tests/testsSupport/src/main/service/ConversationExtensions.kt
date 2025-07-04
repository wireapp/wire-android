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
package service

import com.wire.android.testSupport.backendConnections.BackendClient
import com.wire.android.testSupport.backendConnections.team.Team
import com.wire.android.testSupport.backendConnections.team.defaultheaders
import com.wire.android.testSupport.backendConnections.team.getAuthToken
import network.NetworkBackendClient
import org.json.JSONArray
import org.json.JSONObject
import service.models.QualifiedID
import user.utils.ClientUser
import java.net.URL

suspend fun BackendClient.createTeamConversation(
    user: ClientUser,
    contacts: List<ClientUser>?,
    conversationName: String?,
    team: Team
): String {
    val token = getAuthToken(user)
    val url = URL("conversations".composeCompleteUrl())

    val (ids, qids) = contacts?.partition { it.backendName == user.backendName }
        ?: (emptyList<ClientUser>() to emptyList<ClientUser>())

    val requestBody = JSONObject().apply {
        put("users", JSONArray().apply { ids.forEach { put(it.id) } })
        put("qualified_users", JSONArray().apply {
            qids.forEach {
                put(QualifiedID(it.id.orEmpty(), BackendClient.loadBackend(it.backendName.orEmpty()).domain).toJSON())
            }
        })
        put("conversation_role", "wire_member")
        conversationName?.let { put("name", it) }
        put("team", JSONObject().apply {
            put("teamid", team.id)
            put("managed", false)
        })
        put("access", JSONArray().apply {
            listOf("invite", "code").forEach {
                this.put(it)
            }

        })
        put("access_role_v2", JSONArray().apply {
            listOf("team_member", "non_team_member", "guest", "service").forEach {
                this.put(it)
            }
        })
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
