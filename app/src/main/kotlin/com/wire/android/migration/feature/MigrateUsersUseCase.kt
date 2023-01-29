/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.foldToEitherWhileRight
import com.wire.kalium.logic.functional.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateUsersUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaUserDatabase: ScalaUserDatabaseProvider,
    private val mapper: MigrationMapper
) {

    suspend operator fun invoke(userIds: List<UserId>): Either<CoreFailure, List<UserId>> =
        userIds.foldToEitherWhileRight(listOf()) { userId, acc ->
            val users = scalaUserDatabase.userDAO(userId)?.allUsers() ?: listOf()
            val selfScalaUser = users.first { it.id == userId.value && it.domain == userId.domain }
            if (users.isNotEmpty()) {
                val mappedUsers = users.map { scalaUser ->
                    mapper.fromScalaUserToUser(scalaUser, selfScalaUser.id, selfScalaUser.domain, selfScalaUser.teamId, userId)
                }
                val sessionScope = coreLogic.getSessionScope(userId)
                sessionScope.users.persistMigratedUsers(mappedUsers)
                Either.Right(acc + userId)
            } else Either.Right(acc)
        }

    suspend operator fun invoke(userId: UserId): Either<CoreFailure, UserId> =
        invoke(listOf(userId)).map { userId }
}
