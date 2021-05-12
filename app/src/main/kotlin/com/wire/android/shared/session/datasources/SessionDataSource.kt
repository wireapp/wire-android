package com.wire.android.shared.session.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.map
import com.wire.android.core.functional.suspending
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.datasources.remote.SessionRemoteDataSource
import com.wire.android.shared.session.mapper.SessionMapper

class SessionDataSource(
    private val remoteDataSource: SessionRemoteDataSource,
    private val localDataSource: SessionLocalDataSource,
    private val mapper: SessionMapper
) : SessionRepository {

    override suspend fun save(session: Session, current: Boolean): Either<Failure, Unit> = suspending {
        if (current) {
            localDataSource.setCurrentSessionToDormant().flatMap {
                saveLocally(session, true)
            }
        } else saveLocally(session, current)
    }

    override suspend fun currentSession(): Either<Failure, Session> = localDataSource.currentSession()
        .map { mapper.fromSessionEntity(it) }

    private suspend fun saveLocally(session: Session, current: Boolean) =
        localDataSource.save(mapper.toSessionEntity(session, current))

    override suspend fun accessToken(): Either<Failure, String> = currentSession().map { it.accessToken }

    override suspend fun newAccessToken(refreshToken: String): Either<Failure, Session> =
        remoteDataSource.accessToken(refreshToken).map {
            mapper.fromAccessTokenResponse(it, refreshToken)
        }

    override suspend fun doesCurrentSessionExist(): Either<Failure, Boolean> = localDataSource.doesCurrentSessionExist()

    override suspend fun setSessionCurrent(userId: String): Either<Failure, Unit> = suspending {
        localDataSource.setCurrentSessionToDormant().flatMap {
            localDataSource.setSessionCurrent(userId)
        }
    }

    override suspend fun userAuthorizationToken(userId: String): Either<Failure, String> = localDataSource.userAuthorizationToken(userId)
}
