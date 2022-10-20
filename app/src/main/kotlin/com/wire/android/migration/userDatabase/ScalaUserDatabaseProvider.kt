package com.wire.android.migration.userDatabase

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import com.wire.android.appLogger
import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.util.ScalaDBNameProvider
import com.wire.android.migration.util.openDatabaseIfExists
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ScalaUserDatabaseProvider @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @ApplicationContext private val applicationContext: Context,
) {

    val userId: Lazy<UserId> = lazy {
        coreLogic.globalScope {
            when (val result = session.currentSession()) {
                is CurrentSessionResult.Success -> {
                    result.accountInfo.userId
                }
                else -> {
                    appLogger.e("Error getting session")
                    throw Error("Error getting session")
                }
            }
        }
    }

    private var _db: ScalaUserDatabase? = null
    private val db: Lazy<ScalaUserDatabase?> = lazy {
        synchronized(this@ScalaUserDatabaseProvider) {
            if (_db == null) {
                _db = applicationContext.openDatabaseIfExists(ScalaDBNameProvider.userDB(userId))
                _db
            } else {
                _db
            }
        }
    }

    val clientDAO: Lazy<ScalaClientDAO> = lazy { ScalaClientDAO(db.value!!) }
    val conversationDAO: Lazy<ScalaConversationDAO> = lazy { ScalaConversationDAO(db.value!!) }
}

typealias ScalaUserDatabase = SQLiteDatabase
