package com.wire.android.shared.session.datasources

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.shared.session.Session
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.session.datasources.local.SessionLocalDataSource
import com.wire.android.shared.session.mapper.SessionMapper

class SessionDataSource(
    private val localDataSource: SessionLocalDataSource,
    private val mapper: SessionMapper
) : SessionRepository {

    //TODO: check isCurrent and if true, update older entities
    override suspend fun save(session: Session): Either<Failure, Unit> =
        localDataSource.save(mapper.toSessionEntity(session, true))
}
