package com.wire.android.migration.globalDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.android.migration.util.openDatabaseIfExists
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaAppDataBaseProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context
) {

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
