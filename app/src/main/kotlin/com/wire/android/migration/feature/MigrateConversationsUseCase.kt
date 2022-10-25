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
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateConversationsUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaUserDatabase: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {

    private val logger by lazy { appLogger.withFeatureId(KaliumLogger.Companion.ApplicationFlow.CONVERSATIONS) }

    suspend operator fun invoke(userIds: List<UserId>): Either<CoreFailure, Unit> {
        userIds.forEach { userId ->
            val sessionScope = coreLogic.getSessionScope(userId)
            // start sync to later migrate data into created model

            // todo: we need to migrate clients before doing this sync below
            // sessionScope.syncManager.waitUntilLive()

            val conversations = scalaUserDatabase.conversationDAO(userId)?.conversations()
            val mappedConversations = mutableListOf<Conversation>()
            conversations?.forEachIndexed { index, scalaConversation ->
                logger.d("Conversation num: $index / data: $scalaConversation")
                mappedConversations += mapper.fromScalaConversationToConversation(scalaConversation)
                val migrated = sessionScope.conversations.persistMigratedConversation(mappedConversations)

                logger.d("Migrated conversations? $migrated")
            }
        }

        return Either.Right(Unit)
    }

}
