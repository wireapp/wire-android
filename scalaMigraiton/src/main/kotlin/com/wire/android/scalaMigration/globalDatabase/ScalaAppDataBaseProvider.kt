package com.wire.android.scalaMigration.globalDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wire.android.scalaMigration.util.ScalaDBNameProvider
import com.wire.android.scalaMigration.util.openDatabaseIfExists

class ScalaAppDataBaseProvider(private val applicationContext: Context) {

    private var _db: ScalaGlobalDatabase? = null
    val db
        @Synchronized
        get() = if (_db == null) {
            _db = applicationContext.openDatabaseIfExists(ScalaDBNameProvider.globalDB())
            _db
        } else {
            _db
        }

    val scalaAccountsDAO: ScalaAccountsDAO get() = ScalaAccountsDAO(db!!)
}

typealias ScalaGlobalDatabase = SQLiteDatabase
