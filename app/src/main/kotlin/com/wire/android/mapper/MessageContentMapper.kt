/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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

import com.wire.android.ui.home.conversations.findUser
import com.wire.android.ui.home.conversations.model.UIMessageContent
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.user.User
import javax.inject.Inject

@Suppress("TooManyFunctions")
class MessageContentMapper @Inject constructor(
    private val regularMessageMapper: RegularMessageMapper,
    private val systemMessageMapper: SystemMessageContentMapper
) {
    fun fromMessage(
        message: Message.Standalone,
        userList: List<User>
    ): UIMessageContent? {
        return when (message) {
            is Message.Regular -> {
                when (message.visibility) {
                    Message.Visibility.VISIBLE -> regularMessageMapper.mapMessage(
                        message,
                        userList.findUser(message.senderUserId),
                        userList
                    )

                    Message.Visibility.DELETED -> UIMessageContent.Deleted // for deleted , there is a state label displayed only
                    Message.Visibility.HIDDEN -> null // we don't want to show hidden message content in any way
                }
            }

            is Message.System -> {
                when (message.visibility) {
                    Message.Visibility.VISIBLE -> systemMessageMapper.mapMessage(message, userList)
                    Message.Visibility.DELETED,
                    Message.Visibility.HIDDEN -> null // we don't want to show hidden nor deleted message content in any way
                }
            }
        }
    }
}
