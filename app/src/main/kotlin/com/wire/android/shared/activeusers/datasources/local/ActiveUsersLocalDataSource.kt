package com.wire.android.shared.activeusers.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class ActiveUsersLocalDataSource(private val activeUsersDao: ActiveUsersDao) : DatabaseService {

    suspend fun saveActiveUser(userId: String): Either<Failure, Unit> = request {
        activeUsersDao.insert(ActiveUserEntity(userId))
    }
}
