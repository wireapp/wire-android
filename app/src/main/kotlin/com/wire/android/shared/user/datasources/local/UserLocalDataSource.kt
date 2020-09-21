package com.wire.android.shared.user.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class UserLocalDataSource(private val userDao: UserDao) : DatabaseService {

    suspend fun saveUser(userId: String): Either<Failure, Unit> = request {
        userDao.insert(UserEntity(userId))
    }
}
