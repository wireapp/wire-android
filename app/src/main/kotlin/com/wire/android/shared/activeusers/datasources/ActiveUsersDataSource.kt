package com.wire.android.shared.activeusers.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.activeusers.ActiveUsersRepository
import com.wire.android.shared.activeusers.datasources.local.ActiveUsersLocalDataSource

class ActiveUsersDataSource(private val localDataSource: ActiveUsersLocalDataSource) : ActiveUsersRepository {

    override fun hasActiveUser(): Boolean = localDataSource.activeUserId() != null

    //TODO update active user preference
    override suspend fun saveActiveUser(userId: String): Either<Failure, Unit> = localDataSource.saveActiveUser(userId)
}
