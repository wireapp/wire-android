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

package com.wire.android.migration.globalDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.android.migration.util.openDatabaseIfExists
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import javax.inject.Inject
import javax.inject.Singleton

@OptIn(ExperimentalCoroutinesApi::class)
@Singleton
class ScalaAppDataBaseProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) {

    private val coroutineDispatcher: CoroutineDispatcher = Dispatchers.IO.limitedParallelism(1)

    private var _db: ScalaGlobalDatabase? = null
    val db
        @Synchronized
        get() = if (_db == null) {
            _db = applicationContext.openDatabaseIfExists(ScalaDBNameProvider.globalDB())
            _db
        } else {
            _db
        }
    val scalaAccountsDAO: ScalaAccountsDAO get() = ScalaAccountsDAO(db!!, coroutineDispatcher)
}

typealias ScalaGlobalDatabase = SQLiteDatabase
