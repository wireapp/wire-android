package com.wire.android.migration.feature

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.conversation.Conversation
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

    private val logger by lazy { appLogger.withFeatureId(KaliumLogger.Companion.ApplicationFlow.CONVERSATIONS) }

    suspend operator fun invoke(userIds: List<UserId>): Either<CoreFailure, Unit> = userIds.foldToEitherWhileRight(Unit) { userId, _ ->
        val sessionScope = coreLogic.getSessionScope(userId)
        val conversations = scalaUserDatabase.conversationDAO(userId)?.conversations()
        val mappedConversations = mutableListOf<Conversation>()
        conversations?.forEach { scalaConversation ->
            mappedConversations += mapper.fromScalaConversationToConversation(scalaConversation)
            val migrated = sessionScope.conversations.persistMigratedConversation(mappedConversations)
            logger.d("Migrated conversations? $migrated")
        }
        Either.Right(Unit)
    }
}
