/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */

package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateUsersUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaUserDatabase: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {
    suspend operator fun invoke(userId: UserId): Either<CoreFailure, UserId> =
        scalaUserDatabase.userDAO(userId.value).flatMap { scalaUserDAO ->
            val users = scalaUserDAO.allUsers()
            // No need to match the domain since it can be missing from the scala DB
            // firstOrNull is just to be safe in case the self user is not in the local DB
            // any inconsistency in the DB will be fixed by sync
            // and we only add users that are missing form sync so it is safe to assume that team is null in that case
            val selfScalaUser = users.firstOrNull { it.id == userId.value }
            val mappedUsers = users.map { scalaUser ->
                mapper.fromScalaUserToUser(scalaUser, userId.value, userId.domain, selfScalaUser?.teamId, userId)
            }
            val sessionScope = coreLogic.getSessionScope(userId)
            sessionScope.users.persistMigratedUsers(mappedUsers)
            Either.Right(userId)
        }
}
