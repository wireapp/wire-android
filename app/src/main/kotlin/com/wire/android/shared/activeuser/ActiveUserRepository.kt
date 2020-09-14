package com.wire.android.shared.activeuser

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface ActiveUserRepository {
    fun hasActiveUser() : Boolean

    suspend fun saveActiveUser(userId: String): Either<Failure, Unit>
}
