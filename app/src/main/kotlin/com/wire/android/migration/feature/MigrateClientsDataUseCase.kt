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

import androidx.annotation.VisibleForTesting
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.failure.MigrationFailure
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.android.migration.util.ScalaCryptoBoxDirectoryProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.client.RegisterClientResult
import com.wire.kalium.logic.feature.client.RegisterClientUseCase.RegisterClientParam
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.fold
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
    @Suppress("ReturnCount", "ComplexMethod")
    suspend operator fun invoke(userId: UserId, isFederated: Boolean): Either<CoreFailure, Unit> =
        scalaUserDBProvider.clientDAO(userId.value).flatMap { clientDAO ->
            val clientId = clientDAO.clientInfo()?.clientId?.let { ClientId(it) }
                ?: return@flatMap Either.Right(Unit)
            // move crypto box files
            val scalaDir = scalaCryptoBoxDirectoryProvider.userDir(userId)
            val currentDir = File(coreLogic.rootPathsProvider.rootProteusPath(userId))
            if (currentDir.exists()) {
                return@flatMap Either.Right(Unit)
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
                                    userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
                                }
                        }

                    is RegisterClientResult.E2EICertificateRequired ->
                        withTimeoutOrNull(SYNC_START_TIMEOUT) {
                            syncManager.waitUntilStartedOrFailure()
                        }.let {
                            it ?: Either.Left(NetworkFailure.NoNetworkConnection(null))
                        }.flatMap {
                            syncManager.waitUntilLiveOrFailure()
                                .onSuccess {
                                    userDataStoreProvider.getOrCreate(userId).setInitialSyncCompleted()
                                    TODO() // TODO: ask question about this!
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
    suspend fun fixSessionFileNames(
        userId: UserId,
        proteusDir: File,
        isFederated: Boolean,
        scalaUserDBProvider: ScalaUserDatabaseProvider
    ) {
        val sessionsDir = File(proteusDir, "sessions")
        if (isFederated) {
            val filesWithoutDomain = getSessionFileNamesWithoutDomain(sessionsDir)
                .map { file -> file.name.substringBefore("_") to file }
            val sessionUserIds = filesWithoutDomain.map { (userId, _) -> userId }.distinct()
            val userDAO = scalaUserDBProvider.userDAO(userId.value).fold(
                { return },
                { it }
            )
            val sessionUsers = sessionUserIds.chunked(SESSION_USER_IDS_CHUNK_SIZE)
                .map { userDAO.users(it) }
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
        const val SESSION_USER_IDS_CHUNK_SIZE = 500
    }
}
