package com.wire.android.shared.user

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface UserRepository {
    suspend fun selfUser(accessToken: String, tokenType: String): Either<Failure, User>

    suspend fun save(user: User): Either<Failure, Unit>
}
