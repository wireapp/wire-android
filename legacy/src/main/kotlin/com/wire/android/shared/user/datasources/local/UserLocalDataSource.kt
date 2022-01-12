package com.wire.android.shared.user.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class UserLocalDataSource(private val userDao: UserDao) : DatabaseService {

    suspend fun userById(userId: String): Either<Failure, UserEntity> = request {
        userDao.userById(userId)
    }

    suspend fun save(userEntity: UserEntity): Either<Failure, Unit> = request {
        userDao.insert(userEntity)
    }

    suspend fun update(userEntity: UserEntity): Either<Failure, Unit> = request {
        userDao.update(userEntity)
    }
}
