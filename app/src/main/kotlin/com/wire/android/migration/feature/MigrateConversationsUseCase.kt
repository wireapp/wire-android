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
class MigrateConversationsUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaUserDatabase: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {

    suspend operator fun invoke(userIds: List<UserId>): Either<CoreFailure, Map<UserId, List<ScalaConversationData>>> =
        userIds.foldToEitherWhileRight(mapOf()) { userId, acc ->
            val conversations = scalaUserDatabase.conversationDAO(userId)?.conversations() ?: listOf()
            if (conversations.isNotEmpty()) {
                val mappedConversations = conversations.mapNotNull { scalaConversation ->
                    mapper.fromScalaConversationToConversation(scalaConversation)
                }
                val sessionScope = coreLogic.getSessionScope(userId)
                sessionScope.conversations.persistMigratedConversation(mappedConversations)
                Either.Right(acc + (userId to conversations))
            } else Either.Right(acc)
        }
}
