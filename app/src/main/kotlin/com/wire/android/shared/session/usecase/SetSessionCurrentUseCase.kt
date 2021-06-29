package com.wire.android.shared.session.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository

class SetSessionCurrentUseCase(
    private val sessionRepository: SessionRepository
) : UseCase<Unit, SetSessionCurrentUseCaseParams> {

    override suspend fun run(params: SetSessionCurrentUseCaseParams): Either<Failure, Unit> =
        sessionRepository.setSessionCurrent(params.userId)
}

data class SetSessionCurrentUseCaseParams(val userId: String)
