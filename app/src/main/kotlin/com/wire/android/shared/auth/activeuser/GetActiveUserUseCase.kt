package com.wire.android.shared.auth.activeuser

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.user.User

class GetActiveUserUseCase : UseCase<User, Unit> {

    @Suppress("FunctionOnlyReturningConstant")
    fun hasActiveUser(): Boolean = false //TODO: real implementation

    //TODO real implementation & test
    override suspend fun run(params: Unit): Either<Failure, User> = Either.Right(User("123", "Gizem", "gizem@wire.com"))
}
