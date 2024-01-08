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

package com.wire.android.migration.userDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wire.android.migration.failure.MigrationFailure.MissingUserDatabase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.android.migration.util.openDatabaseIfExists
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ScalaUserDatabaseProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {
    private val _dbs: ConcurrentMap<String, Pair<ScalaUserDatabase, CoroutineDispatcher>?> by lazy { ConcurrentHashMap() }

    @Synchronized
    fun db(userId: String): Pair<ScalaUserDatabase, CoroutineDispatcher>? {
        return _dbs.getOrPut(userId) {
            val dbName = ScalaDBNameProvider.userDB(userId)
            applicationContext.openDatabaseIfExists(dbName)?.let {
                val dispatcher = Dispatchers.IO.limitedParallelism(1)
                it to dispatcher
            }
        }
    }

    fun clientDAO(userId: String): Either<MissingUserDatabase, ScalaClientDAO> =
        db(userId)?.let { Either.Right(ScalaClientDAO(it.first, it.second)) }
            ?: Either.Left(MissingUserDatabase)

    fun conversationDAO(userId: String): Either<MissingUserDatabase, ScalaConversationDAO> =
        db(userId)?.let { Either.Right(ScalaConversationDAO(it.first, it.second)) }
            ?: Either.Left(MissingUserDatabase)

    fun messageDAO(userId: String): Either<MissingUserDatabase, ScalaMessageDAO> =
        db(userId)?.let { Either.Right(ScalaMessageDAO(it.first, it.second)) }
            ?: Either.Left(MissingUserDatabase)

    fun userDAO(userId: String): Either<MissingUserDatabase, ScalaUserDAO> =
        db(userId)?.let { Either.Right(ScalaUserDAO(it.first, it.second)) }
            ?: Either.Left(MissingUserDatabase)
}

typealias ScalaUserDatabase = SQLiteDatabase
