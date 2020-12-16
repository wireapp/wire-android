package com.wire.android.shared.user.username

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.UserRepository

class UpdateUsernameUseCase(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository
) : UseCase<Any, UpdateUsernameParams> {

    override suspend fun run(params: UpdateUsernameParams): Either<Failure, Any> = suspending {
        sessionRepository.currentSession().flatMap {
            userRepository.updateUsername(it.userId, params.username)
        }
    }
}

data class UpdateUsernameParams(val username: String)
