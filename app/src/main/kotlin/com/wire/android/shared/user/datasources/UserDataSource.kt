package com.wire.android.shared.user.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository
import com.wire.android.shared.user.datasources.local.UserLocalDataSource
import com.wire.android.shared.user.datasources.remote.UserRemoteDataSource
import com.wire.android.shared.user.mapper.UserMapper

class UserDataSource(
    private val localDataSource: UserLocalDataSource,
    private val remoteDataSource: UserRemoteDataSource,
    private val mapper: UserMapper
) : UserRepository {

    override suspend fun selfUser(accessToken: String, tokenType: String): Either<Failure, User> =
        remoteDataSource.selfUser(accessToken, tokenType).map {
            mapper.fromSelfUserResponse(it)
        }

    override suspend fun save(userId: String): Either<Failure, Unit> = localDataSource.saveUser(userId)
}
