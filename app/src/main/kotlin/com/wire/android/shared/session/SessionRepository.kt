package com.wire.android.shared.session

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface SessionRepository {
    suspend fun save(session: Session): Either<Failure, Unit>
}
