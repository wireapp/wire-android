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
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateConversationsUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaUserDatabase: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {

    // Note: 1:1 conversations with team members are marked as 0 (GROUP) in scala database
    // atm we insert the conversation that does not exist remotely anymore aka deleted
    // and in that case leaving it as group will not be an issue
    suspend operator fun invoke(userId: UserId): Either<CoreFailure, List<ScalaConversationData>> =
        scalaUserDatabase.conversationDAO(userId.value).flatMap { scalaConvDAO ->
            val conversations = scalaConvDAO.conversations()
            if (conversations.isEmpty()) {
                return@flatMap Either.Right(conversations)
            }

            val mappedConversations = conversations.mapNotNull { scalaConversation ->
                mapper.fromScalaConversationToConversation(scalaConversation, userId)
            }
            coreLogic.sessionScope(userId) {
                migration.persistMigratedConversation(mappedConversations)
            }.map { conversations }
        }
}
