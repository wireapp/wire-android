package com.wire.android.shared.user.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.user.UserRepository
import com.wire.android.shared.user.UserSession
import com.wire.android.shared.user.datasources.local.UserLocalDataSource
import com.wire.android.shared.user.mapper.UserSessionMapper

class UserDataSource(
    private val localDataSource: UserLocalDataSource,
    private val userSessionMapper: UserSessionMapper
) : UserRepository {

    //TODO update active user preference
    override suspend fun saveUser(userId: String): Either<Failure, Unit> = localDataSource.saveUser(userId)

    override suspend fun saveCurrentSession(userSession: UserSession): Either<Failure, Unit> =
        localDataSource.saveSession(userSessionMapper.toSessionEntity(userSession, true))
}
