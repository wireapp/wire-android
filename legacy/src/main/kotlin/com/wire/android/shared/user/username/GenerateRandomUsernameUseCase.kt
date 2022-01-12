package com.wire.android.shared.user.username

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.functional.flatMap
import com.wire.android.core.functional.suspending
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository
import com.wire.android.shared.user.UserRepository

class GenerateRandomUsernameUseCase(
    private val sessionRepository: SessionRepository,
    private val userRepository: UserRepository,
    private val usernameGenerator: UsernameAttemptsGenerator
) : UseCase<String, Unit> {

    override suspend fun run(params: Unit): Either<Failure, String> = suspending {
        sessionRepository.currentSession().flatMap { session ->
            userRepository.userById(session.userId).flatMap {
                generateAndValidateUsernameAttempts(it.name)
            }
        }
    }

    private suspend fun generateAndValidateUsernameAttempts(name: String): Either<Failure, String> =
        userRepository.checkUsernamesExist(usernameGenerator.generateUsernameAttempts(name))
            .flatMap {
                if (it.isEmpty()) {
                    Either.Left(NoAvailableUsernames)
                } else {
                    Either.Right(it.first())
                }
            }
}

object NoAvailableUsernames : FeatureFailure()
