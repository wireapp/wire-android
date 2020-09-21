package com.wire.android.shared.session.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class SessionLocalDataSource(private val sessionDao: SessionDao) : DatabaseService {

    suspend fun saveSession(sessionEntity: SessionEntity): Either<Failure, Unit> = request {
        sessionDao.insert(sessionEntity)
    }
}
