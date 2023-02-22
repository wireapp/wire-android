package com.wire.android.migration.userDatabase

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@ViewModelScoped
class ShouldTriggerMigrationForUserUserCase @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore
) {
    suspend operator fun invoke(userId: UserId) = globalDataStore.getUserMigrationStatus(userId.value)
        .first().let { migrationStatus ->
            if (migrationStatus != UserMigrationStatus.NoNeed) return@let false

            applicationContext.getDatabasePath(ScalaDBNameProvider.userDB(userId))
                .let { it.isFile && it.exists() }
                .also {
                    // if the user database does not exists, we can't migrate it
                    // so we mark it as NoNeed
                    if (!it) globalDataStore.setUserMigrationStatus(userId.value, UserMigrationStatus.NoNeed)
                }
        }
}
