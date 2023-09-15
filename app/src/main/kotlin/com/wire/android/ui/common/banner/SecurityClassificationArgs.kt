/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.common.banner

import com.wire.android.di.ScopedArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
sealed interface SecurityClassificationArgs : ScopedArgs {

    val id: QualifiedID

    @Serializable
    data class Conversation(
        @SerialName("conversationId")
        override val id: ConversationId,
    ) : SecurityClassificationArgs {
        override val key = "$ARGS_KEY:$id"

        companion object {
            const val ARGS_KEY = "SecurityClassificationConversationArgsKey"
        }
    }

    @Serializable
    data class User(
        @SerialName("userId")
        override val id: UserId
    ) : SecurityClassificationArgs {
        override val key = "$ARGS_KEY:$id"

        companion object {
            const val ARGS_KEY = "SecurityClassificationUserArgsKey"
        }
    }
}
