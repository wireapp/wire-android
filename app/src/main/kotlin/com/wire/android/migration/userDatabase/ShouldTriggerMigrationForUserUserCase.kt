package com.wire.android.migration.userDatabase

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.scopes.ViewModelScoped
import kotlinx.coroutines.flow.firstOrNull
import javax.inject.Inject

@ViewModelScoped
class ShouldTriggerMigrationForUserUserCase @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore
) {
    suspend operator fun invoke(userId: UserId) = ScalaDBNameProvider.userDB(userId).let { dbName ->
        applicationContext.getDatabasePath(dbName).let { it.isFile && it.exists() }
    }.let { doesScalaDBExist ->
        if (doesScalaDBExist) {
            !(globalDataStore.isUserMigrated(userId).firstOrNull() ?: false)
        } else {
            false
        }
    }
}
