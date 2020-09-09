package com.wire.android.shared.activeusers.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.storage.db.DatabaseService
import com.wire.android.core.storage.preferences.GlobalPreferences

class ActiveUsersLocalDataSource(
    private val activeUsersDao: ActiveUsersDao,
    private val globalPreferences: GlobalPreferences
) : DatabaseService {

    fun activeUserId(): String? = globalPreferences.activeUserId

    suspend fun saveActiveUser(userId: String): Either<Failure, Unit> = request {
        activeUsersDao.insert(ActiveUserEntity(userId))
    }.map {
        globalPreferences.activeUserId = userId
    }
}
