package com.wire.android.shared.session.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.flatMap
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.mapper.SessionMapper
import kotlinx.coroutines.runBlocking

class SessionDataSource(
    private val localDataSource: SessionLocalDataSource,
    private val mapper: SessionMapper
) : SessionRepository {

    override suspend fun save(session: Session, current: Boolean): Either<Failure, Unit> =
        if (current) {
            localDataSource.setCurrentSessionToDormant().flatMap {
                runBlocking { saveLocally(session, true) }
            }
        } else saveLocally(session, current)

    private suspend fun saveLocally(session: Session, current: Boolean) =
        localDataSource.save(mapper.toSessionEntity(session, current))
}
