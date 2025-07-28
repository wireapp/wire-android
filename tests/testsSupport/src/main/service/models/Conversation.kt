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
package service.models


import org.json.JSONArray
import org.json.JSONObject

data class Conversation(
    var id: String = "",
    var qualifiedID: QualifiedID = QualifiedID(),
    var name: String? = null,
    /*
        RegularConv = 0 (= any group conversation, = any team conversation)
        SelfConv = 1
        One2OneConv = 2
        ConnectConv = 3
     */
    var type: Int? = null,
    var teamId: String? = null,
    var selfId: String = "",
    var otherIds: List<QualifiedID> = emptyList(),
    var creatorId: String = "",
    var receiptMode: Int? = null,
    var protocol: String = "",
    var messageTimerInMilliseconds: Int = 0
) {
    val isReceiptModeEnabled: Boolean
        get() = receiptMode?.let { it > 0 } ?: false

    companion object {
        fun fromJSON(json: JSONObject): Conversation {
            val conversation = Conversation()

            conversation.id = json.getString("id")
            conversation.protocol = json.getString("protocol")
            conversation.qualifiedID = QualifiedID.fromJSON(json.getJSONObject("qualified_id"))

            conversation.name = json.optString("name", null)?.takeIf { it.isNotBlank() }
                ?.replace("\uFFFC", "")?.trim()

            conversation.messageTimerInMilliseconds = json.optInt("message_timer", 0)
            conversation.type = json.optInt("type").takeIf { json.has("type") && !json.isNull("type") }
            conversation.teamId = json.optString("team", null).takeIf { json.has("team") && !json.isNull("team") }

            conversation.creatorId = if (json.isNull("creator")) "null" else json.getString("creator")

            val members = json.getJSONObject("members")
            conversation.selfId = members.getJSONObject("self").getString("id")

            val otherArray: JSONArray = members.getJSONArray("others")
            conversation.otherIds = (0 until otherArray.length()).map { i ->
                val qualified = otherArray.getJSONObject(i).getJSONObject("qualified_id")
                QualifiedID.fromJSON(qualified)
            }

            if (json.has("receipt_mode") && !json.isNull("receipt_mode")) {
                conversation.receiptMode = json.getInt("receipt_mode")
            }

            return conversation
        }
    }
}
