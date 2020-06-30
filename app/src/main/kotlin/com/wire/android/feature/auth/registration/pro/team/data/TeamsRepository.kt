package com.wire.android.feature.auth.registration.pro.team.data

import com.wire.android.core.exception.Failure
import com.wire.android.core.functional.Either

interface TeamsRepository {

    suspend fun teamName(): Either<Failure, String>

    suspend fun updateTeamName(teamName: String): Either<Failure, Unit>

}
