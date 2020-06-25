package com.wire.android.feature.auth.registration.pro.team.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.feature.auth.registration.pro.team.data.TeamsRepository

class UpdateTeamNameUseCase(
        private val teamsRepository: TeamsRepository
) : UseCase<Unit, UpdateTeamNameParams> {

    override suspend fun run(params: UpdateTeamNameParams): Either<Failure, Unit> =
        teamsRepository.updateTeamName(params.teamName)
}

data class UpdateTeamNameParams(val teamName: String)