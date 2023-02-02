/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.migration

import android.content.Context
import androidx.work.Data
import androidx.work.workDataOf
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.MigrationFailure
import com.wire.android.migration.feature.MigrateActiveAccountsUseCase
import com.wire.android.migration.feature.MigrateClientsDataUseCase
import com.wire.android.migration.feature.MigrateConversationsUseCase
import com.wire.android.migration.feature.MigrateMessagesUseCase
import com.wire.android.migration.feature.MigrateServerConfigUseCase
import com.wire.android.migration.feature.MigrateUsersUseCase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.failure.ServerConfigFailure
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.isRight
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.mapLeft
import com.wire.kalium.logic.functional.onFailure
import com.wire.kalium.logic.functional.onSuccess
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList")
@Singleton
class MigrationManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore,
    private val migrateServerConfig: MigrateServerConfigUseCase,
    private val migrateActiveAccounts: MigrateActiveAccountsUseCase,
    private val migrateClientsData: MigrateClientsDataUseCase,
    private val migrateUsers: MigrateUsersUseCase,
    private val migrateConversations: MigrateConversationsUseCase,
    private val migrateMessages: MigrateMessagesUseCase
) {
    private fun isScalaDBPresent(): Boolean =
        applicationContext.getDatabasePath(ScalaDBNameProvider.globalDB()).let { it.isFile && it.exists() }

    suspend fun shouldMigrate(): Boolean = when {
        // already migrated
        globalDataStore.isMigrationCompleted() -> false
        // not yet migrated and old DB is present and mark that we should present the welcome to new android dialog
        isScalaDBPresent() -> {
            globalDataStore.setWelcomeScreenNotPresented()
            true
        }
        // not yet migrated and no DB to migrate from - skip and set as migrated because it's not an update of the old app version
        else -> {
            globalDataStore.setWelcomeScreenPresented()
            globalDataStore.setMigrationCompleted().let { false }
        }
    }

    fun isMigrationCompletedFlow(): Flow<Boolean> = globalDataStore.isMigrationCompletedFlow()

    suspend fun migrate(
        coroutineScope: CoroutineScope,
        updateProgress: suspend (MigrationData.Progress) -> Unit,
        migrationDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(2)
    ): MigrationData.Result {
        migrateServerConfig().map {
            appLogger.d("$TAG - Step 1 - Migrating accounts")
            migrateActiveAccounts(it)
        }.mapLeft(::migrationFailure)
            .onSuccess { (migratedAccounts, isFederated) ->
                updateProgress(MigrationData.Progress(MigrationData.Progress.Type.ACCOUNTS))
                onAccountsMigrated(migratedAccounts, isFederated, coroutineScope, migrationDispatcher).also {
                    appLogger.d("User migration done Result $it")
                }
            }
            .onFailure {
                if (it != MigrationData.Result.Failure.Type.NO_NETWORK) {
                    globalDataStore.setMigrationCompleted() // only for network errors we show the info and "retry" button to the user
                }
                return MigrationData.Result.Failure(it)
            }
        return MigrationData.Result.Success
    }

    private suspend fun onAccountsMigrated(
        migratedAccounts: Map<String, Either<CoreFailure, UserId>>,
        isFederated: Boolean,
        coroutineScope: CoroutineScope,
        migrationDispatcher: CoroutineDispatcher,
    ): Map<String, Either<CoreFailure, Unit>> {
        val resultAcc: ConcurrentMap<String, Either<CoreFailure, Unit>> = ConcurrentHashMap()

        val migrationJobs: List<Job> = migratedAccounts.map { it ->
            coroutineScope.launch(migrationDispatcher) {
                if (it.value.isRight()) {
                    val userid = (it.value as Either.Right).value
                    appLogger.d("$TAG - Step 2 - Migrating clients for $userid")
                    migrateClientsData(userid, isFederated).onFailure { failure ->
                        appLogger.e("$TAG - Step 2 - Migrating clients for $userid failed reason $failure")
                    }
                    appLogger.d("$TAG - Step 3 - Migrating users for $userid")
                    migrateUsers(userid).flatMap {
                        appLogger.d("$TAG - Step 4 - Migrating conversations for $userid")
                        migrateConversations(it)
                    }.flatMap {
                        appLogger.d("$TAG - Step 5 - Migrating messages for $userid")
                        migrateMessages(userid, it, coroutineScope)
                    }.also {
                        resultAcc[userid.value] = it
                    }
                } else {
                    resultAcc[it.key] = it.value as Either.Left
                }
            }
        }
        migrationJobs.joinAll()
        globalDataStore.setMigrationCompleted()

        return resultAcc.toMap()
    }

    private fun migrationFailure(failure: CoreFailure): MigrationData.Result.Failure.Type = when (failure) {
        is NetworkFailure.NoNetworkConnection -> MigrationData.Result.Failure.Type.NO_NETWORK
        is StorageFailure.DataNotFound -> MigrationData.Result.Failure.Type.DATA_NOT_FOUND
        is ServerConfigFailure.UnknownServerVersion -> MigrationData.Result.Failure.Type.UNKNOWN_SERVER_VERSION
        is ServerConfigFailure.NewServerVersion -> MigrationData.Result.Failure.Type.TOO_NEW_VERSION
        is MigrationFailure.InvalidRefreshToken -> MigrationData.Result.Failure.Type.UNKNOWN
        else -> MigrationData.Result.Failure.Type.UNKNOWN
    }

    companion object {
        private const val TAG = "MigrationManager"
    }
}

sealed class MigrationData {
    data class Progress(val type: Type) : MigrationData() {
        enum class Type { SERVER_CONFIGS, ACCOUNTS, CLIENTS, USERS, CONVERSATIONS, MESSAGES, UNKNOWN; }
        companion object {
            const val KEY_PROGRESS_TYPE = "progress_type"
        }
    }

    sealed class Result : MigrationData() {
        object Success : Result()
        data class Failure(val type: Type) : Result() {
            enum class Type { UNKNOWN_SERVER_VERSION, TOO_NEW_VERSION, DATA_NOT_FOUND, NO_NETWORK, UNKNOWN; }
            companion object {
                const val KEY_FAILURE_TYPE = "failure_type"
            }
        }
    }
}

fun MigrationData.Result.Failure.Type.toData(): Data = workDataOf(MigrationData.Result.Failure.KEY_FAILURE_TYPE to this.name)

fun Data.getMigrationFailure(): MigrationData.Result.Failure.Type = this.getString(MigrationData.Result.Failure.KEY_FAILURE_TYPE)
    ?.let {
        try {
            MigrationData.Result.Failure.Type.valueOf(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    } ?: MigrationData.Result.Failure.Type.UNKNOWN

fun MigrationData.Progress.Type.toData(): Data = workDataOf(MigrationData.Progress.KEY_PROGRESS_TYPE to this.name)

fun Data.getMigrationProgress(): MigrationData.Progress.Type = this.getString(MigrationData.Progress.KEY_PROGRESS_TYPE)
    ?.let {
        try {
            MigrationData.Progress.Type.valueOf(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    } ?: MigrationData.Progress.Type.UNKNOWN
