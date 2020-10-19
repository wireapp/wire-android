package com.wire.android.shared.session

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import kotlinx.coroutines.flow.Flow

interface SessionRepository {
    suspend fun save(session: Session, current: Boolean = true): Either<Failure, Unit>

    fun currentSession(): Flow<Session?>

    suspend fun accessToken(refreshToken: String): Either<Failure, Session>

    suspend fun doesCurrentSessionExist(): Either<Failure, Boolean>
}
