package com.wire.android.shared.user.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.User
import com.wire.android.shared.user.UserRepository

class GetCurrentUserUseCase(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) : UseCase<User, Unit> {

    override suspend fun run(params: Unit): Either<Failure, User> = suspending {
        sessionRepository.currentSession().flatMap {
            userRepository.userById(it.userId)
        }
    }
}
