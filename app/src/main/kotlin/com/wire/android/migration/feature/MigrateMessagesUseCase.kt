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
 *
 *
 */

package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaConversationData
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.onFailure
import kotlinx.coroutines.CoroutineScope
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateMessagesUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaUserDatabase: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {
    suspend operator fun invoke(
        userId: UserId,
        scalaConversations: List<ScalaConversationData>,
        coroutineScope: CoroutineScope
    ): Map<String, CoreFailure> {
        val errorsAcc: MutableMap<String, CoreFailure> = mutableMapOf()
        val messageDAO = scalaUserDatabase.messageDAO(userId)
        val userDAO = scalaUserDatabase.userDAO(userId)
        // iterate over all conversations and migrate messages
        // if any error occurs, add it to the errors accumulator
        // if the accumulator is empty, return Right(Unit)
        // otherwise, return Left(errorsAcc)
        for (scalaConversation in scalaConversations) {
            val messages = messageDAO?.messages(listOf(scalaConversation)) ?: listOf()
            if (messages.isEmpty()) continue

            val users = userDAO?.users(messages.map { it.senderId }.distinct())?.associateBy { it.id } ?: mapOf()
            val mappedMessages = messages.mapNotNull { scalaMessage ->
                users[scalaMessage.senderId]?.let { mapper.fromScalaMessageToMessage(userId, scalaMessage, it) }
            }
            val sessionScope = coreLogic.getSessionScope(userId)
            sessionScope.messages.persistMigratedMessage(mappedMessages, coroutineScope).onFailure {
                errorsAcc[scalaConversation.id] = it
            }
        }

        return errorsAcc
    }
}
