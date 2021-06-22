@file:Suppress("MagicNumber")
package com.wire.android.feature.conversation.data

import com.wire.android.feature.conversation.ConversationType
import com.wire.android.feature.conversation.Group
import com.wire.android.feature.conversation.IncomingConnection
import com.wire.android.feature.conversation.OneToOne
import com.wire.android.feature.conversation.Self
import com.wire.android.feature.conversation.Unknown
import com.wire.android.feature.conversation.WaitingForConnection

class ConversationTypeMapper {
    fun fromIntValue(type: Int): ConversationType =
        when (type) {
            0 -> Group
            1 -> Self
            2 -> OneToOne
            3 -> WaitingForConnection
            4 -> IncomingConnection
            else -> Unknown
        }

    fun toIntValue(type: ConversationType): Int =
        when (type) {
            Group -> 0
            Self -> 1
            OneToOne -> 2
            WaitingForConnection -> 3
            IncomingConnection -> 4
            Unknown -> -1
        }
}
