package com.wire.android.shared.user

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface UserRepository {
    suspend fun save(userId: String): Either<Failure, Unit>
}
