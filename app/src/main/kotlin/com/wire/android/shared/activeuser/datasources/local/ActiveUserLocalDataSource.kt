package com.wire.android.shared.activeuser.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.storage.db.DatabaseService
import com.wire.android.core.storage.preferences.GlobalPreferences

class ActiveUserLocalDataSource(
    private val activeUserDao: ActiveUserDao,
    private val globalPreferences: GlobalPreferences
) : DatabaseService {

    fun activeUserId(): String? = globalPreferences.activeUserId

    suspend fun saveActiveUser(userId: String): Either<Failure, Unit> = request {
        activeUserDao.insert(ActiveUserEntity(userId))
    }.map {
        globalPreferences.activeUserId = userId
    }
}
