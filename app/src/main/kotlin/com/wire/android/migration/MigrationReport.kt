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
