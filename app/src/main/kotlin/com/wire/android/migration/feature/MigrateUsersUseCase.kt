package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.MigrationMapper
import com.wire.android.migration.userDatabase.ScalaUserDatabaseProvider
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.foldToEitherWhileRight
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
                    mapper.fromScalaUserToUser(scalaUser, selfScalaUser.id, selfScalaUser.domain, selfScalaUser.teamId)
                }
                val sessionScope = coreLogic.getSessionScope(userId)
                sessionScope.users.persistMigratedUsers(mappedUsers)
                Either.Right(acc + userId)
            } else Either.Right(acc)
        }
}
