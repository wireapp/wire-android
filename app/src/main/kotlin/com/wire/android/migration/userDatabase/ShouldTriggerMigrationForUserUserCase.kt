package com.wire.android.migration.userDatabase

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.first
import javax.inject.Inject

@ViewModelScoped
class ShouldTriggerMigrationForUserUserCase @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore
) {
    suspend operator fun invoke(userId: UserId) = globalDataStore.getUserMigrationStatus(userId.value)
        .first().let { migrationStatus ->
            when (migrationStatus) {
                // if the user has already been migrated, we don't need to do it again
                UserMigrationStatus.Completed,
                UserMigrationStatus.NoNeed -> return@let false

                // if the user has not been migrated yet, we check if the database exists
                // also check when null since it can mean the migration is done on 4.0.1
                UserMigrationStatus.NotStarted,
                null -> checkForScalaDB(userId)
            }
        }

    private suspend fun checkForScalaDB(userId: UserId) = applicationContext.getDatabasePath(ScalaDBNameProvider.userDB(userId.value))
        .let { it.isFile && it.exists() }
        .also {
            // if the user database does not exists, we can't migrate it
            // so we mark it as NoNeed
            if (!it) globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.NoNeed)
        }
}
