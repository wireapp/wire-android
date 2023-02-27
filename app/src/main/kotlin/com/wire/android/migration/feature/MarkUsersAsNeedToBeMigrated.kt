package com.wire.android.migration.feature

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.migration.failure.UserMigrationStatus
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MarkUsersAsNeedToBeMigrated @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore
) {
    /**
     * get all the local scala DB where the DB file name is a UUID
     * and mark them as NotStarted
     */
    suspend operator fun invoke() {
        applicationContext.databaseList()
            .filter { it.matches(UUID_REGEX) }
            .forEach {
                globalDataStore.setUserMigrationStatus(it, UserMigrationStatus.NotStarted)
            }
    }

    private companion object {
        val UUID_REGEX = Regex("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")
    }
}
