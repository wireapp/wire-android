package com.wire.android.migration.userDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.android.migration.util.openDatabaseIfExists
import com.wire.kalium.logic.data.user.UserId
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaUserDatabaseProvider @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
    private val userId: UserId
) {

    private var _db: ScalaUserDatabase? = null
    val db
        @Synchronized
        get() = if (_db == null) {
            _db = applicationContext.openDatabaseIfExists(ScalaDBNameProvider.userDB(userId))
            _db
        } else {
            _db
        }

    val clientDAO: ScalaClientDAO = ScalaClientDAO(db!!)
    val conversationDAO: ScalaConversationDAO = ScalaConversationDAO(db!!)
}

typealias ScalaUserDatabase = SQLiteDatabase
