package com.wire.android.shared.session.datasources.local

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.storage.db.DatabaseService

class SessionLocalDataSource(private val sessionDao: SessionDao) : DatabaseService {

    suspend fun save(sessionEntity: SessionEntity): Either<Failure, Unit> = request {
        sessionDao.insert(sessionEntity)
    }

    suspend fun currentSession(): Either<Failure, SessionEntity> = request {
        sessionDao.currentSession()
    }

    suspend fun setClientIdToUser(userId: String, clientId: String) = request {
        sessionDao.setClientIdToUserSession(userId, clientId)
    }

    suspend fun userSession(userId: String): Either<Failure, SessionEntity> = request {
        sessionDao.userSession(userId)
    }

    suspend fun setCurrentSessionToDormant(): Either<Failure, Unit> = request {
        sessionDao.setCurrentSessionToDormant()
    }

    suspend fun doesCurrentSessionExist(): Either<Failure, Boolean> = request {
        sessionDao.doesCurrentSessionExist()
    }

    suspend fun setSessionCurrent(userId: String): Either<Failure, Unit> = request {
        sessionDao.setSessionCurrent(userId)
    }
}
