package com.wire.android.feature.auth.registration.pro.team.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.registration.pro.team.data.TeamsRepository

class GetTeamNameUseCase(private val teamsRepository: TeamsRepository) : UseCase<String, Unit> {
    override suspend fun run(params: Unit): Either<Failure, String> =
        teamsRepository.teamName()
}
