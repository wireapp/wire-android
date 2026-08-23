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

package com.wire.android.ui.home.conversations.usecase

import com.wire.kalium.logic.data.message.DeliveryStatus
import com.wire.kalium.logic.data.message.Message
import com.wire.kalium.logic.data.message.MessageContent
import com.wire.kalium.logic.data.user.User
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.conversation.ObserveUserListByIdUseCase
import dev.zacsweers.metro.Inject
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapLatest

class GetUsersForMessageUseCase @Inject constructor(
    private val observeMemberDetailsByIds: ObserveUserListByIdUseCase,
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    suspend operator fun invoke(message: Message): List<User> {
        val listWithSender: List<User> = message.sender?.let { listOf(it) } ?: listOf()
        val otherUserIdList = message.relatedUserIds()

        return if (otherUserIdList.isNotEmpty()) {
            observeMemberDetailsByIds(otherUserIdList)
                .mapLatest { usersList ->
                    listWithSender.plus(usersList)
                }.first()
        } else {
            listWithSender
        }
    }
}

private fun Message.relatedUserIds(): List<UserId> = when (this) {
    is Message.Regular -> when (val deliveryStatus = deliveryStatus) {
        is DeliveryStatus.CompleteDelivery -> emptyList()
        is DeliveryStatus.PartialDelivery ->
            deliveryStatus.recipientsFailedDelivery + deliveryStatus.recipientsFailedWithNoClients
    }

    is Message.System -> when (val content = content) {
        is MessageContent.MemberChange -> content.members
        is MessageContent.LegalHold.ForMembers -> content.members
        else -> emptyList()
    }

    is Message.Signaling -> emptyList()
}.distinct()
