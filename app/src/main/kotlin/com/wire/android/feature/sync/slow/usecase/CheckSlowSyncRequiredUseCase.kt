package com.wire.android.feature.sync.slow.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.sync.SyncRepository

class CheckSlowSyncRequiredUseCase(private val syncRepository: SyncRepository) : UseCase<Boolean, Unit> {

    override suspend fun run(params: Unit): Either<Failure, Boolean> =
        Either.Right(syncRepository.isSlowSyncRequired())
}
