package com.wire.android.shared.user.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.user.UserRepository
import com.wire.android.shared.user.datasources.local.UserLocalDataSource

class UserDataSource(private val localDataSource: UserLocalDataSource) : UserRepository {

    //TODO update active user preference
    override suspend fun saveUser(userId: String): Either<Failure, Unit> = localDataSource.saveUser(userId)
}
