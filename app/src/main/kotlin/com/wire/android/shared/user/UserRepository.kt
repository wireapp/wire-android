package com.wire.android.shared.user

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface UserRepository {
    suspend fun saveUser(userId: String): Either<Failure, Unit>
    suspend fun saveCurrentSession(userSession: UserSession): Either<Failure, Unit>
}
