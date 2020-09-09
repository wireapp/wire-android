package com.wire.android.shared.auth.activeuser

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.activeusers.ActiveUsersRepository
import com.wire.android.shared.user.User

class GetActiveUserUseCase(private val activeUsersRepository: ActiveUsersRepository) : UseCase<User, Unit> {

    fun hasActiveUser() = activeUsersRepository.hasActiveUser()

    //TODO real implementation & test
    override suspend fun run(params: Unit): Either<Failure, User> = Either.Right(User("Gizem"))
}
