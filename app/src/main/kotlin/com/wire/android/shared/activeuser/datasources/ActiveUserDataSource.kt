package com.wire.android.shared.activeuser.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.activeuser.ActiveUserRepository
import com.wire.android.shared.activeuser.datasources.local.ActiveUserLocalDataSource

class ActiveUserDataSource(private val localDataSource: ActiveUserLocalDataSource) : ActiveUserRepository {

    override fun hasActiveUser(): Boolean = localDataSource.activeUserId() != null

    //TODO update active user preference
    override suspend fun saveActiveUser(userId: String): Either<Failure, Unit> = localDataSource.saveActiveUser(userId)
}
