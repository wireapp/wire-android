package com.wire.android.util

import com.wire.kalium.logic.data.conversation.ConversationId
import kotlin.math.absoluteValue

// TODO: remove once the solution is implemented
@Suppress("MagicNumber")
private val conversationColors = listOf(0xFFFF0000, 0xFF00FF00, 0xFF0000FF)

fun getConversationColor(id: ConversationId): Long {
    return conversationColors[indexedColor(id)]
}

//tmp solution, this color or it's index should come from Kalium
fun indexedColor(id: ConversationId): Int = (id.hashCode() % conversationColors.size).absoluteValue
