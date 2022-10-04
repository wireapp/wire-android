package com.wire.android.migration

import android.content.Context
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.server.FetchApiVersionResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.functional.Either
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrationManager @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    @ApplicationContext private val applicationContext: Context,
    private val globalDataStore: GlobalDataStore,
    private val scalaServerConfigDAO: ScalaServerConfigDAO,
) {

    private fun isScalaDBPresent(): Boolean = applicationContext.getDatabasePath(SCALA_DB_NAME).let { it.isFile && it.exists() }

    suspend fun shouldMigrate(): Boolean = when {
        // already migrated
        globalDataStore.isMigrationCompleted() -> false
        // not yet migrated and old DB is present
        isScalaDBPresent() -> true
        // not yet migrated and no DB to migrate from - skip and set as migrated because it's not an update of the old app version
        else -> globalDataStore.setMigrationCompleted().let { false }
    }

    private suspend fun ServerConfig.Links.fetchApiVersionAndStore(): Either<CoreFailure, Unit> =
        coreLogic.globalScope { fetchApiVersion(this@fetchApiVersionAndStore) }.let {
            when (it) {
                is FetchApiVersionResult.Failure.Generic -> Either.Left(it.genericFailure)
                FetchApiVersionResult.Failure.TooNewVersion -> { TODO() }
                FetchApiVersionResult.Failure.UnknownServerVersion -> { TODO() }
                is FetchApiVersionResult.Success -> Either.Right(Unit) // config already stored in `fetchApiVersion`
            }
        }

    private suspend fun migrateServerConfig(): Either<CoreFailure, Unit> {
        return when (val result = scalaServerConfigDAO.scalaServerConfig) {
            is ScalaServerConfig.Full -> {
                // TODO store
                Either.Right(Unit)
            }
            is ScalaServerConfig.Links -> result.links.fetchApiVersionAndStore()
            is ScalaServerConfig.ConfigUrl ->
                coreLogic.globalScope { fetchServerConfigFromDeepLink(result.customConfigUrl) }.let {
                    when (it) {
                        is GetServerConfigResult.Success -> it.serverConfigLinks.fetchApiVersionAndStore()
                        is GetServerConfigResult.Failure.Generic -> Either.Left(it.genericFailure)
                    }
                }
            ScalaServerConfig.NoData -> Either.Left(StorageFailure.DataNotFound)
        }
    }

    suspend fun migrate(): Either<CoreFailure, Unit> {
        return migrateServerConfig()
        // TODO
    }

    companion object {
        private const val SCALA_DB_NAME = "ZGlobal.db"
    }
}
