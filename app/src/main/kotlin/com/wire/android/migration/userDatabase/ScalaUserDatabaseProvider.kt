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

package com.wire.android.migration.userDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.android.migration.util.openDatabaseIfExists
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaUserDatabaseProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {
    private val _dbs: ConcurrentMap<UserId, ScalaUserDatabase> by lazy { ConcurrentHashMap() }

    @Synchronized
    fun db(userId: UserId): ScalaUserDatabase? =
        if (_dbs[userId] != null) _dbs[userId]
        else applicationContext.openDatabaseIfExists(ScalaDBNameProvider.userDB(userId)).also { _dbs[userId] = it }

    fun clientDAO(userId: UserId): ScalaClientDAO? = db(userId)?.let { ScalaClientDAO(it) }
    fun conversationDAO(userId: UserId): ScalaConversationDAO? = db(userId)?.let { ScalaConversationDAO(it) }
    fun messageDAO(userId: UserId): ScalaMessageDAO? = db(userId)?.let { ScalaMessageDAO(it) }
    fun userDAO(userId: UserId): ScalaUserDAO? = db(userId)?.let { ScalaUserDAO(it) }
}

typealias ScalaUserDatabase = SQLiteDatabase
