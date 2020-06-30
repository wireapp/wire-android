package com.wire.android.feature.auth.registration.pro.team.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either

class TeamDataSource : TeamsRepository {

    private var teamName = String.EMPTY

    //TODO Get from local cache
    override suspend fun teamName(): Either<Failure, String> = Either.Right(teamName)

    //TODO Update local cache
    override suspend fun updateTeamName(teamName: String): Either<Failure, Unit> {
        this.teamName = teamName
        return Either.Right(Unit)
    }
}
