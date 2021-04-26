package com.wire.android.shared.session.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.session.SessionRepository

class SetCurrentSessionToDormantUseCase(private val sessionRepository: SessionRepository) : UseCase<Unit, Unit> {

    override suspend fun run(params: Unit): Either<Failure, Unit> = sessionRepository.setCurrentSessionToDormant()
}
