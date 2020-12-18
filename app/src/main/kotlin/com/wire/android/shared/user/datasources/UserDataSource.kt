package com.wire.android.shared.user.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
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

    override suspend fun selfUser(accessToken: String, tokenType: String): Either<Failure, User> = suspending {
        remoteDataSource.selfUser(accessToken, tokenType).map {
            mapper.fromSelfUserResponse(it)
        }.flatMap { user ->
            save(user).map { user }
        }
    }

    override suspend fun save(user: User): Either<Failure, Unit> =
        localDataSource.save(mapper.toUserEntity(user))

    override suspend fun doesUsernameExist(username: String): Either<Failure, Unit> =
        remoteDataSource.doesUsernameExist(username)

    override suspend fun updateUsername(userId: String, username: String): Either<Failure, Unit> = suspending {
        updateUsernameRemotely(username).flatMap {
            updateUsernameLocally(userId, username)
        }
    }

    private suspend fun updateUsernameRemotely(username: String): Either<Failure, Unit> =
        remoteDataSource.updateUsername(username)

    private suspend fun updateUsernameLocally(userId: String, username: String): Either<Failure, Unit> = suspending {
        localDataSource.userById(userId).flatMap {
            val updatedEntity = it.copy(username = username)
            localDataSource.update(updatedEntity)
        }
    }
}
