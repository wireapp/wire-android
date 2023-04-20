package com.wire.android.migration

import com.wire.android.migration.feature.MigrateActiveAccountsUseCase
import com.wire.android.migration.userDatabase.ScalaConversationData
import com.wire.kalium.logger.obfuscateId
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.isLeft
import com.wire.kalium.logic.functional.map
import com.wire.kalium.logic.functional.mapLeft

class MigrationReport {
    private val report: MutableMap<String, Either<CoreFailure, Any>> = mutableMapOf()

    fun toFormattedString(): String =
        report.entries.joinToString(separator = "\n") { (key, value) ->
            "$key: ${value.fold({ it.toString() }, { "Success" })}"
        }

    fun addServerConfigReport(result: Either<CoreFailure, ServerConfig>) {
        result.also { addReport("Migrate ServerConfig", it) }
    }

    fun addAccountsReport(result: MigrateActiveAccountsUseCase.Result) {
        result.userIds.values.firstOrNull { it.isLeft() }?.let {
            it.mapLeft { it.cause }.map { }.also { addReport("Migrate Accounts", it) }
        } ?: addReport("Migrate Accounts", Either.Right(Unit))
    }

    fun addClientReport(userId: UserId, result: Either<CoreFailure, Unit>) {
        addReport("Migrate Client ${userId.value.obfuscateId().hashCode()}", result)
    }

    fun addUserReport(userId: UserId, result: Either<CoreFailure, UserId>) {
        result.also {
            addReport("Migrate User ${userId.value.obfuscateId().hashCode()}", it)
        }
    }

    fun addConversationReport(userId: UserId, result: Either<CoreFailure, List<ScalaConversationData>>) {
        result.also {
            addReport("Migrate Conversation ${userId.value.obfuscateId().hashCode()}", it)
        }
    }

    fun addMessagesReport(userId: UserId, result: Either<CoreFailure, Unit>) {
        addReport("Migrate Messages ${userId.value.obfuscateId().hashCode()}", result)
    }

    private fun addReport(key: String, value: Either<CoreFailure, Any>) {
        report[key] = value
    }
}
