package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaConversationData
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.foldToEitherWhileRight
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateMessagesUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaUserDatabase: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {

    suspend operator fun invoke(userIdsAndConversationIds: Map<UserId, List<ScalaConversationData>>): Either<CoreFailure, Unit> =
        userIdsAndConversationIds.toList().foldToEitherWhileRight(Unit) { (userId, scalaConversations), _ ->
            val messageDAO = scalaUserDatabase.messageDAO(userId)
            val userDAO = scalaUserDatabase.userDAO(userId)
            val messages = messageDAO?.messages(scalaConversations) ?: listOf()
            if (messages.isNotEmpty()) {
                val mappedMessages = messages.mapNotNull {
                    userDAO?.users(listOf(it.senderId))?.firstOrNull()?.let { user -> mapper.fromScalaMessageToMessage(it, user) }
                }
                val sessionScope = coreLogic.getSessionScope(userId)
                sessionScope.messages.persistMigratedMessage(mappedMessages)
            }
            Either.Right(Unit)
        }
}

