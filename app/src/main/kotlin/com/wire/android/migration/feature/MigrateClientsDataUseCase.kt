package com.wire.android.migration.feature

import androidx.annotation.VisibleForTesting
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
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase.RegisterClientParam
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
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
    @Suppress("LoopWithTooManyJumpStatements", "ComplexMethod")
    suspend operator fun invoke(userIds: List<UserId>, isFederated: Boolean): Map<UserId, Either<CoreFailure, Unit>> {

        val acc: MutableMap<UserId, Either<CoreFailure, Unit>> = mutableMapOf()

        for (userId in userIds) {
            val clientId = scalaUserDBProvider.clientDAO(userId)?.clientInfo()?.clientId?.let { ClientId(it) }
            if (clientId == null) {
                acc[userId] = Either.Left(StorageFailure.DataNotFound)
                continue
            }

            // move crypto box files
            val scalaDir = scalaCryptoBoxDirectoryProvider.userDir(userId)
            val currentDir = File(coreLogic.rootPathsProvider.rootProteusPath(userId))
            if (currentDir.exists()) {
                acc[userId] = Either.Right(Unit)
                continue
            }

            try {
                scalaDir.copyRecursively(target = currentDir, overwrite = false)
                // Session file names from the scala app contain user ids without a domain, AR uses session file names having user ids
                // with a domain, so migrated session file names have to be fixed by adding a domain to them.
                fixSessionFileNames(userId, currentDir, isFederated, scalaUserDBProvider)
            } catch (_: Exception) {
                currentDir.deleteRecursively()
            }

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
                                .onSuccess {
                                    acc[userId] = Either.Right(Unit)
                                    userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
                                }
                        }
                }
            }
        }

    @VisibleForTesting
    fun getSessionFileNamesWithoutDomain(sessionsDir: File): List<File> =
        if (sessionsDir.exists() && sessionsDir.isDirectory) {
            sessionsDir.listFiles { file ->
                !file.isDirectory && !file.name.contains("@")
            }?.asList() ?: listOf()
        } else listOf()

    @VisibleForTesting
    fun fixSessionFileNames(userId: UserId, proteusDir: File, isFederated: Boolean, scalaUserDBProvider: ScalaUserDatabaseProvider) {
        val sessionsDir = File(proteusDir, "sessions")
        if (isFederated) {
            val filesWithoutDomain = getSessionFileNamesWithoutDomain(sessionsDir)
                .map { file -> file.name.substringBefore("_") to file }
            val sessionUserIds = filesWithoutDomain.map { (userId, _) -> userId }.distinct()
            val sessionUsers = sessionUserIds.chunked(500)
                .map { scalaUserDBProvider.userDAO(userId)?.users(it) ?: listOf() }
                .flatten()
                .associateBy { it.id }
            filesWithoutDomain.forEach { (sessionUserId, file) ->
                renameSessionFileIfNeeded(sessionsDir, file, sessionUsers[sessionUserId]?.domain ?: userId.domain)
            }
        } else {
            getSessionFileNamesWithoutDomain(sessionsDir).forEach { file ->
                renameSessionFileIfNeeded(sessionsDir, file, userId.domain)
            }
        }
    }

    @VisibleForTesting
    fun renameSessionFileIfNeeded(sessionsDir: File, file: File, domain: String): File {
        val fixedSessionFileName = fixSessionFileName(file.name, domain)
        if (fixedSessionFileName != file.name) {
            val newFile = File(sessionsDir, fixedSessionFileName)
            return if (file.renameTo(newFile)) newFile else file
        }
        return file
    }

    @VisibleForTesting
    fun fixSessionFileName(sessionFileName: String, domain: String): String {
        val sessionNameParams = sessionFileName.split("_")
        return if (!sessionNameParams.first().contains("@")) {
            // this session file name does not contain a domain and needs to be updated
            listOf(sessionNameParams.first() + "@" + domain)
                .plus(sessionNameParams.drop(1))
                .joinToString("_")
        } else sessionFileName
    }

    companion object {
        const val SYNC_START_TIMEOUT = 20_000L
    }
}
