package com.wire.android.shared.team.usecase

import com.wire.android.core.exception.Failure
import com.wire.android.core.exception.FeatureFailure
import com.wire.android.core.functional.Either
import com.wire.android.core.usecase.UseCase
import com.wire.android.shared.team.Team
import com.wire.android.shared.user.User

class GetUserTeamUseCase : UseCase<Team, GetUserTeamUseCaseParams> {

    override suspend fun run(params: GetUserTeamUseCaseParams): Either<Failure, Team> {
        //TODO: implement
        return Either.Left(NotATeamUser)
    }
}

data class GetUserTeamUseCaseParams(val user: User)

sealed class GetUserTeamFailure: FeatureFailure()
object NotATeamUser : GetUserTeamFailure()
