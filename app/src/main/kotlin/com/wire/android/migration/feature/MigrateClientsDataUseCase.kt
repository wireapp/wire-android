package com.wire.android.migration.feature

import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.failure.MigrationFailure
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.android.migration.util.ScalaCryptoBoxDirectoryProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.RegisterClientUseCase.RegisterClientParam
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.foldToEitherWhileRight
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.onSuccess
import kotlinx.coroutines.withTimeoutOrNull
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateClientsDataUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaCryptoBoxDirectoryProvider: ScalaCryptoBoxDirectoryProvider,
    private val scalaUserDBProvider: ScalaUserDatabaseProvider,
    private val userDataStoreProvider: UserDataStoreProvider
) {
    suspend operator fun invoke(userIds: List<UserId>): Either<CoreFailure, List<UserId>> =
        userIds.foldToEitherWhileRight(emptyList()) { userId, acc ->

            val clientId = scalaUserDBProvider.clientDAO(userId)?.clientInfo()?.clientId?.let { ClientId(it) }
                ?: return Either.Left(StorageFailure.DataNotFound)

            // move crypto box files
            val scalaDir = scalaCryptoBoxDirectoryProvider.userDir(userId)
            val currentDir = File(coreLogic.rootPathsProvider.rootProteusPath(userId))
            scalaDir.copyRecursively(target = currentDir, overwrite = false)
            scalaDir.deleteRecursively()

            // add registered client id, sync will start when the registered id is persisted
            coreLogic.sessionScope(userId) {
                // NOTE we are passing in an RegisterClientParam will null values
                // because we don't support deleting any existing clients when migrating
                // from the old scala app.
                when (val result = this.client.importClient(
                    clientId, RegisterClientParam(
                        password = null,
                        capabilities = null
                    )
                )) {
                    is RegisterClientResult.Failure.Generic ->
                        Either.Left(result.genericFailure)

                    is RegisterClientResult.Failure.TooManyClients ->
                        Either.Left(MigrationFailure.ClientNotRegistered)

                    is RegisterClientResult.Failure.InvalidCredentials ->
                        Either.Left(MigrationFailure.ClientNotRegistered)

                    is RegisterClientResult.Failure.PasswordAuthRequired -> {
                        Either.Left(MigrationFailure.ClientNotRegistered)
                    }

                    is RegisterClientResult.Success ->
                        withTimeoutOrNull(SYNC_START_TIMEOUT) {
                            syncManager.waitUntilStartedOrFailure()
                        }.let {
                            it ?: Either.Left(NetworkFailure.NoNetworkConnection(null))
                        }.flatMap {
                            syncManager.waitUntilLiveOrFailure()
                                .map { acc + userId }
                                .onSuccess { userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted() }
                        }
                    else -> Either.Left(MigrationFailure.ClientNotRegistered)
                }
            }
        }

    companion object {
        const val SYNC_START_TIMEOUT = 20_000L
    }
}
