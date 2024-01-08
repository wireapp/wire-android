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

package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaConversationData
import com.wire.android.migration.userDatabase.ScalaMessageDAO
import com.wire.android.migration.userDatabase.ScalaUserDAO
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.getOrNull
import com.wire.kalium.logic.functional.map
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
    /**
     * Migrates messages from the Scala database to the Kalium database.
     * @param userId the user id
     * @param scalaConversations the conversations to migrate
     * @param coroutineScope the coroutine scope
     * @return a map of conversation ids and errors if any
     */
    suspend operator fun invoke(
        userId: UserId,
        scalaConversations: List<ScalaConversationData>,
        coroutineScope: CoroutineScope
    ): Map<String, CoreFailure> {
        val (messageDAO: ScalaMessageDAO, userDAO: ScalaUserDAO) = scalaUserDatabase.messageDAO(userId.value).flatMap { messageDAO ->
            scalaUserDatabase.userDAO(userId.value).map { messageDAO to it }
        }.getOrNull() ?: return emptyMap()

        val errorsAcc: MutableMap<String, CoreFailure> = mutableMapOf()
        // iterate over all conversations and migrate messages
        // if any error occurs, add it to the errors accumulator
        // if the accumulator is empty, return Right(Unit)
        // otherwise, return Left(errorsAcc)
        for (scalaConversation in scalaConversations) {
            val scalaMessageList = messageDAO.messages(listOf(scalaConversation))
            if (scalaMessageList.isEmpty()) continue

            val users = userDAO.users(scalaMessageList.map { it.senderId }.distinct()).associateBy { it.id }
            val mappedMessages = scalaMessageList.mapNotNull { scalaMessage ->
                users[scalaMessage.senderId]?.let { mapper.fromScalaMessageToMessage(userId, scalaMessage, it) }
            }
            coreLogic.sessionScope(userId) {
                persistMigratedMessage(mappedMessages, coroutineScope).onFailure {
                    errorsAcc[scalaConversation.id] = it
                }
            }
        }
        return errorsAcc
    }
}
