package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.failure.MigrationFailure
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.android.migration.util.ScalaCryptoBoxDirectoryProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.PersistRegisteredClientIdResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.foldToEitherWhileRight
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateClientsDataUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaCryptoBoxDirectoryProvider: ScalaCryptoBoxDirectoryProvider,
    private val scalaUserDBProvider: ScalaUserDatabaseProvider
) {
    suspend operator fun invoke(userIds: List<UserId>): Either<CoreFailure, Unit> =
        userIds.foldToEitherWhileRight(Unit) { userId, _ ->

            val clientId = scalaUserDBProvider.clientDAO(userId)?.clientInfo()?.clientId?.let { ClientId(it) }
                ?: return Either.Left(StorageFailure.DataNotFound)

            // move crypto box files
            val scalaDir = scalaCryptoBoxDirectoryProvider.userDir(userId)
            val currentDir = File(coreLogic.rootPathsProvider.rootProteusPath(userId))
            scalaDir.copyRecursively(target = currentDir, overwrite = false)
            scalaDir.deleteRecursively()

            // add registered client id, sync will start when the registered id is persisted
            coreLogic.sessionScope(userId) {
                when (val result = this.client.persistRegisteredClientIdUseCase(clientId)) {
                    is PersistRegisteredClientIdResult.Failure.Generic ->
                        Either.Left(result.genericFailure)
                    is PersistRegisteredClientIdResult.Failure.ClientNotRegistered -> {
                        Either.Left(MigrationFailure.ClientNotRegistered)
                    }
                    is PersistRegisteredClientIdResult.Success ->
                        Either.Right(Unit)
                }
            }
        }
}
