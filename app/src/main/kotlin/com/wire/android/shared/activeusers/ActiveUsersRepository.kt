package com.wire.android.shared.activeusers

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface ActiveUsersRepository {
    fun hasActiveUser() : Boolean

    suspend fun saveActiveUser(userId: String): Either<Failure, Unit>
}
