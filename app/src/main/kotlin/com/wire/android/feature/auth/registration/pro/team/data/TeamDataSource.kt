package com.wire.android.feature.auth.registration.pro.team.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.extension.EMPTY
import com.wire.android.core.functional.Either

class TeamDataSource : TeamsRepository {

    private val teamDataCache = mutableMapOf<String, String>()

    override suspend fun teamName(): Either<Failure, String> =
        Either.Right(teamDataCache[TEAM_NAME_KEY] ?: String.EMPTY)

    override suspend fun updateTeamName(teamName: String): Either<Failure, Unit> {
        teamDataCache[TEAM_NAME_KEY] = teamName
        return Either.Right(Unit)
    }

    companion object {
        private const val TEAM_NAME_KEY = "teamNameKey"
    }
}
