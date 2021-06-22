package com.wire.android.feature.sync.slow.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.sync.SyncRepository

class SetSlowSyncCompletedUseCase(private val syncRepository: SyncRepository) : UseCase<Unit, Unit> {

    override suspend fun run(params: Unit): Either<Failure, Unit> =
        Either.Right(syncRepository.setSlowSyncCompleted())
}
