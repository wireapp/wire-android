@file:Suppress("TooGenericExceptionCaught", "TooGenericExceptionThrown", "MagicNumber")
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

import android.graphics.Bitmap
import backendUtils.BackendClient
import backendUtils.team.defaultheaders
import backendUtils.team.getAuthToken
import com.wire.android.testSupport.backendConnections.team.Team
import com.wire.android.testSupport.service.TestService
import kotlinx.coroutines.runBlocking
import network.NetworkBackendClient
import network.RequestOptions
import org.json.JSONArray
import org.json.JSONObject
import service.enums.LegalHoldStatus
import service.models.Conversation
import service.models.QualifiedID
import service.models.SendTextWithLinkParams
import user.utils.AccessToken
import user.utils.ClientUser
import util.generateQRCode
import java.io.File
import java.io.FileOutputStream
import java.io.IOException
import java.net.URL
import java.time.Duration
import java.util.regex.Matcher

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
        put(
            "qualified_users",
            JSONArray().apply {
                qids.forEach {
                    put(
                        QualifiedID(
                            it.id.orEmpty(),
                            BackendClient.loadBackend(it.backendName.orEmpty()).domain
                        ).toJSON()
                    )
                }
            }
        )
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
                listOf("invite", "code").forEach {
                    this.put(it)
                }
            }
        )
        put(
            "access_role_v2",
            JSONArray().apply {
                listOf("team_member", "non_team_member", "guest", "service").forEach {
                    this.put(it)
                }
            }
        )
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

fun BackendClient.getConversationObjects(token: AccessToken, conversationIDs: JSONArray): JSONObject {
    val url = "v10/conversations/list".composeCompleteUrl()
    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token.type} ${token.value}")
    }
    val requestBody = JSONObject().put("qualified_ids", conversationIDs)
    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(url),
        method = "POST",
        headers = headers,
        body = requestBody.toString(),
        options = RequestOptions(accessToken = token)
    )
    return JSONObject(response.body)
}

fun BackendClient.getConversationIDs(user: ClientUser): List<QualifiedID> {
    val result = mutableListOf<QualifiedID>()
    var pagingState: String? = null
    do {
        val token = runBlocking { getAuthToken(user) }
        if (token == null) return emptyList()
        val response = getConversationIDs(token, pagingState)
        pagingState = response.getString("paging_state")
        val qualifiedConversations = response.getJSONArray("qualified_conversations")
        for (i in 0 until qualifiedConversations.length()) {
            result.add(QualifiedID.fromJSON(qualifiedConversations.getJSONObject(i)))
        }
    } while (response.getBoolean("has_more"))
    return result
}

fun BackendClient.getConversations(user: ClientUser): List<Conversation> {
    val result = mutableListOf<Conversation>()
    val ids = getConversationIDs(user)
    for (i in ids.indices step 1000) {
        val batch = ids.subList(i, minOf(i + 1000, ids.size))
        val jsonArray = JSONArray(batch.map { it.toJSON() })
        val token = runBlocking { getAuthToken(user) }
        if (token == null) return emptyList()
        val response = getConversationObjects(token, jsonArray)
        val found = response.getJSONArray("found")
        for (j in 0 until found.length()) {
            result.add(Conversation.fromJSON(found.getJSONObject(j)))
        }
    }
    return result
}

fun BackendClient.getConversationByName(user: ClientUser, name: String): Conversation {
    return getConversations(user).firstOrNull { conv ->
        when {
            conv.name == null && conv.otherIds.size == 1 && conv.protocol == "mls" -> {
                try {
                    getUserNameByID(conv.otherIds[0].domain, conv.otherIds[0].id, user) == name
                } catch (e: IOException) {
                    false
                }
            }

            conv.type == 2 -> {
                try {
                    getUserNameByID(conv.otherIds[0].domain, conv.otherIds[0].id, user) == name
                } catch (e: Exception) {
                    false
                }
            }

            else -> conv.name == name
        }
    } ?: throw NoSuchElementException("Conversation '$name' does not exist for user '${user.name}'")
}

fun BackendClient.getConversationIDs(token: AccessToken, pagingState: String? = null): JSONObject {
    val url = "conversations/list-ids".composeCompleteUrl()

    val requestBody = JSONObject().apply {
        put("paging_state", pagingState)
    }

    val headers = defaultheaders.toMutableMap().apply {
        put("Authorization", "${token.type} ${token.value}")
    }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(url),
        method = "POST",
        body = requestBody.toString(),
        headers = headers,
        options = RequestOptions(accessToken = token)
    )

    return JSONObject(response.body)
}

fun TestServiceHelper.userSendsGenericMessageToConversation(
    senderAlias: String,
    convoName: String,
    deviceName: String? = null,
    message: String
) {
    matchUrl(message)?.let { matcher ->
            val title = matcher.group(0)

            // Generate QR Code bitmap
            val bitmap = generateQRCode(title.orEmpty(), 512)

            // Save bitmap to temporary file
            val tempFile = File.createTempFile("link_preview_", ".png")
            FileOutputStream(tempFile).use { fos ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)
            }

           testServiceClient.userSendsLinkPreview(
                this,
                senderAlias,
                convoName,
                deviceName,
                false,
                message,
                title,
                tempFile.absolutePath
            )

            tempFile.deleteOnExit() // Optional: cleanup later

    } ?: run {
        userSendMessageToConversation(senderAlias,message, deviceName.orEmpty(),convoName,false)
    }
}

fun TestService.userSendsLinkPreview(
    helper: TestServiceHelper,
    senderAlias: String,
    convoName: String,
    deviceName: String? = null,
    isSelfDeleting: Boolean=true,
    msg: String,
    title: String,
    imagePath: String
) {
    val matcher = matchUrl(msg)
        ?: throw IllegalArgumentException("Text does not contain any URL: $msg")

    val urlOffset = matcher.regionStart()
    val url = matcher.group(0)

    val user = helper.toClientUser(senderAlias)
    val conversation = runBlocking {
        helper.toConvoObj(user, convoName)
    }
    val convoId = conversation.qualifiedID.id
    val convoDomain = conversation.qualifiedID.domain

    sendLinkPreview(
        SendTextWithLinkParams(
            user,
            deviceName,
            convoId,
            convoDomain,
            expectsReadConfirmation=false,
            text =msg,
            legalHoldStatus = LegalHoldStatus.ENABLED.code,
            summary =title,
            title =title,
            url =url,
            permUrl =url,
            urlOffset = urlOffset.toString(),
            filePath = imagePath,
            imagePath = imagePath,
            imageFile = File(imagePath),
            timeout = Duration.ofSeconds(1000),
            messageTimer = if (isSelfDeleting) Duration.ofSeconds(1000) else Duration.ofSeconds(0),
        )
    )
}


private fun matchUrl(message: String): Matcher? {
    val pattern = Regex("""([a-z]+://)?[a-z0-9\-]+\.[a-z]+[^\s\n]*""", RegexOption.IGNORE_CASE)
    val matcher = pattern.toPattern().matcher(message)
    return if (matcher.find()) matcher else null
}


fun BackendClient.addUsersToGroupConversation(
    asUser: ClientUser,
    contacts: List<ClientUser>,
    conversation: Conversation
): JSONObject {
    val url = "conversations/${conversation.id}/members/v2".composeCompleteUrl()

    val requestBody = JSONObject().apply {
        val userIds = JSONArray().apply {
            contacts.forEach { contact ->
                val backendDomain = BackendClient.loadBackend(contact.backendName.orEmpty()).domain
                put(QualifiedID(contact.id.orEmpty(), backendDomain).toJSON())
            }
        }
        put("qualified_users", userIds)
        put("conversation_role", "wire_member")
    }
    val token = runBlocking { getAuthToken(asUser) }
    val headers =
        defaultheaders.toMutableMap().apply {
            put("Authorization", "${token?.type} ${token?.value}")
        }

    val response = NetworkBackendClient.sendJsonRequestWithCookies(
        url = URL(url),
        method = "POST",
        body = requestBody.toString(),
        headers = headers,
        options = RequestOptions(accessToken = token)
    )

    return JSONObject(response.body)
}


fun BackendClient.getPersonalConversationByName(user: ClientUser, name: String): Conversation =
    getConversations(user).firstOrNull { conv ->
        conv.protocol == "mls" &&
                when (conv.type) {
                    2 -> try {
                        getUserNameByID(conv.otherIds[0].domain, conv.otherIds[0].id, user) == name
                    } catch (e: Exception) {
                        false
                    }

                    else -> conv.name == name
                }
    } ?: throw NoSuchElementException("Conversation '$name' does not exist for user '${user.name}'")

fun BackendClient.getConversationByName(ownerUser: ClientUser, otherUser: ClientUser): Conversation =
    getConversations(ownerUser).firstOrNull { conv ->
        conv.type == 2 && try {
            getUserNameByID(conv.otherIds[0].domain, conv.otherIds[0].id, ownerUser) == otherUser.name
        } catch (e: Exception) {
            false
        }
    } ?: throw NoSuchElementException("1:1 conversation with '${otherUser.name}' does not exist for user '${ownerUser.name}'")

fun BackendClient.getConversationsByName(user: ClientUser, name: String): List<Conversation> =
    getConversations(user).filter { conv ->
        conv.name == name || (
                conv.otherIds.size == 1 && try {
                    getUserNameByID(
                        conv.qualifiedID.domain,
                        conv.otherIds[0].id, user
                    ) == name
                } catch (e: Exception) {
                    false
                }
                )
    }
