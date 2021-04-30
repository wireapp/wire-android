package com.wire.android.shared.session.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository

class SetDormantSessionToCurrentUseCase(
    private val sessionRepository: SessionRepository
) : UseCase<Unit, SetDormantSessionToCurrentUseCaseParams> {

    override suspend fun run(params: SetDormantSessionToCurrentUseCaseParams): Either<Failure, Unit> =
        sessionRepository.setDormantSessionToCurrent(params.userId)
}

data class SetDormantSessionToCurrentUseCaseParams(val userId: String)
