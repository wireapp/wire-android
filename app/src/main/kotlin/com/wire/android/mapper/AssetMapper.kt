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
package com.wire.android.mapper

import com.wire.android.R
import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.MessageTime
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.android.ui.home.conversations.model.messagetypes.asset.UIAsset
import com.wire.android.util.time.ISOFormatter
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.User
import javax.inject.Inject

class AssetMapper @Inject constructor(
    private val messageContentMapper: MessageContentMapper,
    private val isoFormatter: ISOFormatter,
) {

    @Suppress("LongMethod")
    fun toUIAsset(userList: List<User>, message: Message.Standalone): UIAsset? {
        val sender = userList.findUser(message.senderUserId)
        val content = messageContentMapper.fromMessage(
            message = message,
            userList = userList
        )
        return if (content is UIMessageContent.ImageMessage) {
            UIAsset(
                imageMessage = content,
                time = MessageTime(message.date),
                username = sender?.name?.let { UIText.DynamicString(it) }
                    ?: UIText.StringResource(R.string.username_unavailable_label)
            )
        } else {
            null
        }
    }
}
