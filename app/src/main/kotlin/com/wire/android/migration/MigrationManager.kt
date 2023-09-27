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

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.text.Spanned
import androidx.core.app.NotificationCompat
import androidx.core.text.toSpanned
import androidx.work.Data
import androidx.work.workDataOf
import com.wire.android.R
import com.wire.android.appLogger
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.MigrationFailure
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.migration.feature.MarkUsersAsNeedToBeMigrated
import com.wire.android.migration.feature.MigrateActiveAccountsUseCase
import com.wire.android.migration.feature.MigrateClientsDataUseCase
import com.wire.android.migration.feature.MigrateConversationsUseCase
import com.wire.android.migration.feature.MigrateMessagesUseCase
import com.wire.android.migration.feature.MigrateServerConfigUseCase
import com.wire.android.migration.feature.MigrateUsersUseCase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.android.notification.NotificationConstants
import com.wire.android.notification.openAppPendingIntent
import com.wire.android.notification.openMigrationLoginPendingIntent
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.stringWithBoldArgs
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.EncryptionFailure
import com.wire.kalium.logic.MLSFailure
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.ProteusFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.isLeft
import com.wire.kalium.logic.functional.isRight
import com.wire.kalium.logic.functional.map
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
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Suppress("LongParameterList", "TooGenericExceptionCaught", "TooManyFunctions")
@Singleton
class MigrationManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore,
    private val migrateServerConfig: MigrateServerConfigUseCase,
    private val migrateActiveAccounts: MigrateActiveAccountsUseCase,
    private val migrateClientsData: MigrateClientsDataUseCase,
    private val migrateUsers: MigrateUsersUseCase,
    private val migrateConversations: MigrateConversationsUseCase,
    private val migrateMessages: MigrateMessagesUseCase,
    private val markUsersAsNeedToBeMigrated: MarkUsersAsNeedToBeMigrated,
    private val notificationManager: NotificationManager
) {
    private suspend fun isScalaDBPresent(): Boolean = withContext(Dispatchers.IO) {
        applicationContext
            .getDatabasePath(ScalaDBNameProvider.globalDB())
            .let { it.isFile && it.exists() }
    }
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

    suspend fun migrateSingleUser(
        userId: UserId,
        coroutineScope: CoroutineScope,
        updateProgress: suspend (MigrationData.Progress) -> Unit,
    ): MigrationData.Result {
        val report: MigrationReport = MigrationReport()
        updateProgress(MigrationData.Progress(MigrationData.Progress.Type.MESSAGES))

        return try {
            appLogger.d("$TAG - Step 1 - Migrating users for ${userId.value.obfuscateId()}")
            migrateUsers(userId)
                .also { report.addUserReport(userId, it) }
                .flatMap {
                    appLogger.d("$TAG - Step 2 - Migrating conversations for ${userId.value.obfuscateId()}")
                    migrateConversations(it).also { report.addConversationReport(userId, it) }
                }
                .flatMap {
                    appLogger.d("$TAG - Step 3 - Migrating messages for ${userId.value.obfuscateId()}")
                    migrateMessages(userId, it, coroutineScope).let { failedConversation ->
                        if (failedConversation.isEmpty()) {
                            Either.Right(Unit)
                        } else {
                            Either.Left(failedConversation.values.first())
                        }
                    }.also { report.addMessagesReport(userId, it) }
                }
                .fold({
                    globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.CompletedWithErrors)
                    when (it) {
                        is NetworkFailure.NoNetworkConnection -> MigrationData.Result.Failure.NoNetwork
                        else -> MigrationData.Result.Failure.Messages(
                            it.getErrorCode().toString(),
                            report.toFormattedString()
                        )
                    }
                }, {
                    globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.Successfully)
                    MigrationData.Result.Success
                })
        } catch (e: Exception) {
            globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.CompletedWithErrors)
            appLogger.e("$TAG - Migration failed for ${userId.value.obfuscateId()}")
            MigrationData.Result.Failure.Messages(
                CoreFailure.Unknown(e).getErrorCode().toString(),
                report.toFormattedString()
            )
        }
    }

    suspend fun migrate(
        coroutineScope: CoroutineScope,
        updateProgress: suspend (MigrationData.Progress) -> Unit,
        migrationDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(2)
    ): MigrationData.Result = try {
        val report: MigrationReport = MigrationReport()

        updateProgress(MigrationData.Progress(MigrationData.Progress.Type.ACCOUNTS))
        markUsersAsNeedToBeMigrated()
        migrateServerConfig()
            .also { report.addServerConfigReport(it) }
            .map {
                appLogger.d("$TAG - Step 1 - Migrating accounts")
                migrateActiveAccounts(it).also { report.addAccountsReport(it) }
            }
            .map { it.userIds.values.toList() to it.isFederationEnabled }
            .fold(
                {
                    when (it) {
                        is NetworkFailure.NoNetworkConnection -> MigrationData.Result.Failure.NoNetwork
                        else -> MigrationData.Result.Failure.Account.Any(report.toFormattedString())
                    }
                }, { (migratedAccounts, isFederated) ->
                    onAccountsMigrated(migratedAccounts, isFederated, coroutineScope, updateProgress, migrationDispatcher)
                }
            ).also { showNotificationsIfNeeded(it) }
    } finally {
        // if migration crashed for any reason, we want to set migration as completed so that we don't try to migrate again
        // and avoid any possible crash loop
        globalDataStore.setMigrationCompleted()
    }

    private fun showNotificationsIfNeeded(result: MigrationData.Result) {
        when (result) {
            is MigrationData.Result.Failure.Account.Specific -> showAccountSpecificNotification(result.userName, result.userHandle)
            is MigrationData.Result.Failure.Account.Any -> showAccountAnyNotification()
            is MigrationData.Result.Failure.Messages -> showMessagesNotification(result.errorCode)
            is MigrationData.Result.Failure.Unknown,
            is MigrationData.Result.Failure.NoNetwork,
            is MigrationData.Result.Success -> { /* no-op */
            }
        }
    }

    @Suppress("MagicNumber")
    private fun CoreFailure.getErrorCode(): Int = when (this) {
        is MigrationFailure.ClientNotRegistered -> 1
        is MigrationFailure.InvalidRefreshToken -> 2
        is StorageFailure -> 3
        is NetworkFailure -> 4
        is EncryptionFailure -> 5
        is ProteusFailure -> 6
        is MLSFailure -> 7
        else -> 0
    }

    @Suppress("NestedBlockDepth")
    private suspend fun onAccountsMigrated(
        migratedAccounts: List<Either<MigrateActiveAccountsUseCase.AccountMigrationFailure, UserId>>,
        isFederated: Boolean,
        coroutineScope: CoroutineScope,
        updateProgress: suspend (MigrationData.Progress) -> Unit,
        migrationDispatcher: CoroutineDispatcher = Dispatchers.Default.limitedParallelism(2)
    ): MigrationData.Result {
        val accountsSucceeded = migratedAccounts.filter { it.isRight() }.map { (it as Either.Right).value }
        val accountsFailed = migratedAccounts.filter { it.isLeft() }.map { (it as Either.Left).value }
        return if (accountsFailed.any { it.cause is NetworkFailure.NoNetworkConnection }) {
            MigrationData.Result.Failure.NoNetwork
        } else {
            updateProgress(MigrationData.Progress(MigrationData.Progress.Type.MESSAGES))
            val (results, report) = migrateAccountsData(accountsSucceeded, isFederated, coroutineScope, migrationDispatcher)
            val dataFailed = results.values.filter { it.isLeft() }.map { (it as Either.Left).value }
            when {
                dataFailed.any { it is NetworkFailure.NoNetworkConnection } -> MigrationData.Result.Failure.NoNetwork
                accountsFailed.size > 1 -> MigrationData.Result.Failure.Account.Any(report.toFormattedString())
                accountsFailed.size == 1 -> accountsFailed.first().let {
                    if (it.userName?.isNotEmpty() == true && it.userHandle?.isNotEmpty() == true) {
                        MigrationData.Result.Failure.Account.Specific(it.userName, it.userHandle, report.toFormattedString())
                    } else {
                        MigrationData.Result.Failure.Account.Any(report.toFormattedString())
                    }
                }

                dataFailed.isNotEmpty() ->
                    MigrationData.Result.Failure.Messages(
                        dataFailed.joinToString { it.getErrorCode().toString() },
                        report.toFormattedString()
                    )

                else -> MigrationData.Result.Success
            }
        }
    }

    private suspend fun migrateAccountsData(
        migratedAccounts: List<UserId>,
        isFederated: Boolean,
        coroutineScope: CoroutineScope,
        migrationDispatcher: CoroutineDispatcher
    ): Pair<Map<String, Either<CoreFailure, Unit>>, MigrationReport> {

        val report: MigrationReport = MigrationReport()
        val resultAcc: ConcurrentMap<String, Either<CoreFailure, Unit>> = ConcurrentHashMap()

        val migrationJobs: List<Job> = migratedAccounts.map { userId ->
            coroutineScope.launch(migrationDispatcher) {
                try {
                    appLogger.d("$TAG - Step 2 - Migrating clients for ${userId.value.obfuscateId()}")
                    migrateClientsData(userId, isFederated)
                        .also { report.addClientReport(userId, it) }
                    appLogger.d("$TAG - Step 3 - Migrating users for ${userId.value.obfuscateId()}")
                    migrateUsers(userId)
                        .also { report.addUserReport(userId, it) }
                        .flatMap {
                            appLogger.d("$TAG - Step 4 - Migrating conversations for ${userId.value.obfuscateId()}")
                            migrateConversations(it)
                                .also { report.addConversationReport(userId, it) }
                        }.flatMap {
                            appLogger.d("$TAG - Step 5 - Migrating messages for ${userId.value.obfuscateId()}")
                            migrateMessages(userId, it, coroutineScope).let { failedConversations ->
                                if (failedConversations.isEmpty()) {
                                    Either.Right(Unit)
                                } else {
                                    Either.Left(failedConversations.values.first())
                                }
                            }.also { report.addMessagesReport(userId, it) }
                        }.also {
                            resultAcc[userId.value] = it
                        }.onFailure {
                            globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.CompletedWithErrors)
                        }.onSuccess {
                            globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.Successfully)
                        }
                } catch (e: Exception) {
                    resultAcc[userId.value] = Either.Left(CoreFailure.Unknown(e))
                    globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.CompletedWithErrors)
                }
            }
        }
        migrationJobs.joinAll()
        return resultAcc.toMap() to report
    }

    private fun showMigrationFailureNotification(message: Spanned, pendingIntent: PendingIntent) {
        val notification = NotificationCompat.Builder(applicationContext, NotificationConstants.OTHER_CHANNEL_ID)
            .setSmallIcon(R.drawable.notification_icon_small)
            .setAutoCancel(true)
            .setSilent(false)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .setContentTitle(applicationContext.getString(R.string.welcome_migration_dialog_title))
            .setContentText(message)
            .setStyle(NotificationCompat.BigTextStyle().bigText(message))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setContentIntent(pendingIntent)
            .build()
        notificationManager.notify(NotificationConstants.MIGRATION_ERROR_NOTIFICATION_ID, notification)
    }

    private fun showAccountAnyNotification() {
        val message = applicationContext.resources.getString(R.string.migration_login_required).toSpanned()
        showMigrationFailureNotification(message, openMigrationLoginPendingIntent(applicationContext, String.EMPTY))
    }

    private fun showAccountSpecificNotification(userName: String, userHandle: String) {
        val message = applicationContext.resources.stringWithBoldArgs(
            R.string.migration_login_required_specific_account,
            applicationContext.resources.getString(R.string.migration_login_required_specific_account_name, userName, userHandle)
        )
        showMigrationFailureNotification(message, openMigrationLoginPendingIntent(applicationContext, userHandle))
    }

    private fun showMessagesNotification(errorCode: String) {
        val message = applicationContext.resources.getString(R.string.migration_messages_failure, errorCode).toSpanned()
        showMigrationFailureNotification(message, openAppPendingIntent(applicationContext))
    }

    fun dismissMigrationFailureNotification() {
        notificationManager.cancel(NotificationConstants.MIGRATION_ERROR_NOTIFICATION_ID)
    }

    companion object {
        private const val TAG = "MigrationManager"
    }
}

sealed class MigrationData {
    data class Progress(val type: Type) : MigrationData() {
        enum class Type { ACCOUNTS, MESSAGES, UNKNOWN; }
        companion object {
            const val KEY_PROGRESS_TYPE = "progress_type"
            val steps = listOf(Type.ACCOUNTS, Type.MESSAGES) // migration steps in order
        }
    }

    sealed class Result : MigrationData() {
        object Success : Result()
        sealed class Failure(open val migrationReport: String?) : Result() {
            class Unknown(val throwable: Throwable?, override val migrationReport: String?) : Failure(migrationReport)
            object NoNetwork : Failure(null)
            sealed class Account(override val migrationReport: String?) : Failure(migrationReport) {
                class Specific(val userName: String, val userHandle: String, migrationReport: String?) : Account(migrationReport)
                class Any(migrationReport: String?) : Account(migrationReport)
            }

            class Messages(val errorCode: String, migrationReport: String?) : Failure(migrationReport)

            companion object {
                const val KEY_FAILURE_TYPE = "failure_type"
                const val FAILURE_TYPE_NO_NETWORK = "failure_no_network"
                const val FAILURE_TYPE_ACCOUNT_ANY = "failure_account_any"
                const val FAILURE_TYPE_ACCOUNT_SPECIFIC = "failure_account_specific"
                const val FAILURE_TYPE_MESSAGES = "failure_messages"
                const val KEY_FAILURE_USER_NAME = "failure_user_name"
                const val KEY_FAILURE_USER_HANDLE = "failure_user_handle"
                const val KEY_FAILURE_ERROR_CODE = "failure_error_code"
                const val KEY_MIGRATION_EXCEPTION = "failure_error_message"
                const val KEY_MIGRATION_REPORT = "migration_report"
            }
        }
    }
}

fun MigrationData.Progress.Type.toData(): Data = workDataOf(MigrationData.Progress.KEY_PROGRESS_TYPE to this.name)

fun Data.getMigrationProgress(): MigrationData.Progress.Type = this.getString(MigrationData.Progress.KEY_PROGRESS_TYPE)?.let {
    try {
        MigrationData.Progress.Type.valueOf(it)
    } catch (e: IllegalArgumentException) {
        null
    }
} ?: MigrationData.Progress.Type.UNKNOWN

fun MigrationData.Result.Failure.toData(): Data = when (this) {
    is MigrationData.Result.Failure.Account.Any -> workDataOf(
        MigrationData.Result.Failure.KEY_FAILURE_TYPE to MigrationData.Result.Failure.FAILURE_TYPE_ACCOUNT_ANY,
        MigrationData.Result.Failure.KEY_MIGRATION_REPORT to this.migrationReport!!
    )

    is MigrationData.Result.Failure.Account.Specific -> workDataOf(
        MigrationData.Result.Failure.KEY_FAILURE_TYPE to MigrationData.Result.Failure.FAILURE_TYPE_ACCOUNT_SPECIFIC,
        MigrationData.Result.Failure.KEY_FAILURE_USER_NAME to this.userName,
        MigrationData.Result.Failure.KEY_FAILURE_USER_HANDLE to this.userHandle,
        MigrationData.Result.Failure.KEY_MIGRATION_REPORT to this.migrationReport!!
    )

    is MigrationData.Result.Failure.Messages -> workDataOf(
        MigrationData.Result.Failure.KEY_FAILURE_TYPE to MigrationData.Result.Failure.FAILURE_TYPE_MESSAGES,
        MigrationData.Result.Failure.KEY_FAILURE_ERROR_CODE to this.errorCode,
        MigrationData.Result.Failure.KEY_MIGRATION_REPORT to this.migrationReport
    )

    MigrationData.Result.Failure.NoNetwork -> workDataOf(
        MigrationData.Result.Failure.KEY_FAILURE_TYPE to MigrationData.Result.Failure.FAILURE_TYPE_NO_NETWORK
    )

    is MigrationData.Result.Failure.Unknown -> {
        if (this.throwable != null) {
            workDataOf(
                MigrationData.Result.Failure.KEY_MIGRATION_EXCEPTION to this.throwable,
                MigrationData.Result.Failure.KEY_MIGRATION_REPORT to this.migrationReport!!
            )
        } else {
            workDataOf(
                MigrationData.Result.Failure.KEY_MIGRATION_REPORT to this.migrationReport!!
            )
        }
    }
}

fun Exception.toData(): Data = workDataOf(MigrationData.Result.Failure.KEY_MIGRATION_EXCEPTION to this)

fun Data.getMigrationFailure(): MigrationData.Result.Failure = when (this.getString(MigrationData.Result.Failure.KEY_FAILURE_TYPE)) {
    MigrationData.Result.Failure.FAILURE_TYPE_ACCOUNT_SPECIFIC -> MigrationData.Result.Failure.Account.Specific(
        this.getString(MigrationData.Result.Failure.KEY_FAILURE_USER_NAME) ?: "",
        this.getString(MigrationData.Result.Failure.KEY_FAILURE_USER_HANDLE) ?: "",
        this.getString(MigrationData.Result.Failure.KEY_MIGRATION_REPORT)
    )

    MigrationData.Result.Failure.FAILURE_TYPE_ACCOUNT_ANY -> MigrationData.Result.Failure.Account.Any(
        this.getString(MigrationData.Result.Failure.KEY_MIGRATION_REPORT)
    )

    MigrationData.Result.Failure.FAILURE_TYPE_MESSAGES -> MigrationData.Result.Failure.Messages(
        this.getString(MigrationData.Result.Failure.KEY_FAILURE_ERROR_CODE) ?: "",
        this.getString(MigrationData.Result.Failure.KEY_MIGRATION_REPORT)
    )

    MigrationData.Result.Failure.FAILURE_TYPE_NO_NETWORK -> MigrationData.Result.Failure.NoNetwork

    else -> MigrationData.Result.Failure.Unknown(
        this.keyValueMap[MigrationData.Result.Failure.KEY_MIGRATION_EXCEPTION]?.let { it as? Throwable },
        this.getString(MigrationData.Result.Failure.KEY_MIGRATION_REPORT)
    )
}
