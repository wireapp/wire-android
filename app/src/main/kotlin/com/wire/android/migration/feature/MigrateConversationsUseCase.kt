package com.wire.android.migration.feature

import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logger.KaliumLogger
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
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

    suspend operator fun invoke(): Either<CoreFailure, Unit> {
        // fetch from scala db the current list of convos
        val conversations = scalaUserDatabase.conversationDAO.value.conversations()

        logger.d("> Num. of conversations to migrate: ${conversations.size}")
        conversations.forEachIndexed { index, data ->
            logger.d("Conversation num: $index / data: $data")
        }

        // run sync and wait or just sync/fetch convos ?
        // filter not in db and persist the rest upsert (ignoring present)
        // -- at the kalium level, should we perform inserts/selects with partial id (not qualified)?
//        with(coreLogic.getSessionScope(userId)) {
//            syncConversations().fold({ logger.e("Error while migrating conversations $it") }, {
//
//            })
//        }
        return Either.Right(Unit)
    }

    private fun filterConversations() {}

}
