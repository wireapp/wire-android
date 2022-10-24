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

}

typealias ScalaUserDatabase = SQLiteDatabase
