package com.wire.android.migration

import android.content.Context
import androidx.work.Data
import androidx.work.workDataOf
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.NetworkFailure
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.failure.ServerConfigFailure
import com.wire.kalium.logic.feature.server.FetchApiVersionResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.StoreServerConfigResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.flatMap
import com.wire.kalium.logic.functional.fold
import com.wire.kalium.logic.functional.mapLeft
import dagger.hilt.android.qualifiers.ApplicationContext
import io.github.esentsov.PackagePrivate
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

    private suspend fun ServerConfig.Links.fetchApiVersionAndStore(): Either<CoreFailure, ServerConfig> =
        coreLogic.getGlobalScope().fetchApiVersion(this).let { // it also already stores the fetched config
            when (it) {
                is FetchApiVersionResult.Success -> Either.Right(it.serverConfig)
                FetchApiVersionResult.Failure.TooNewVersion -> Either.Left(ServerConfigFailure.NewServerVersion)
                FetchApiVersionResult.Failure.UnknownServerVersion -> Either.Left(ServerConfigFailure.UnknownServerVersion)
                is FetchApiVersionResult.Failure.Generic -> Either.Left(it.genericFailure)
            }
        }

    @PackagePrivate
    internal suspend fun migrateServerConfig(): Either<CoreFailure, ServerConfig> =
        when (val scalaServerConfig = scalaServerConfigDAO.scalaServerConfig) {
            is ScalaServerConfig.Full -> // TODO what to do when versionInfo.domain is null?
                coreLogic.getGlobalScope().storeServerConfig(scalaServerConfig.links, scalaServerConfig.versionInfo).let {
                    when (it) {
                        is StoreServerConfigResult.Success -> Either.Right(it.serverConfig)
                        is StoreServerConfigResult.Failure.Generic -> Either.Left(it.genericFailure)
                    }
                }
            is ScalaServerConfig.Links -> scalaServerConfig.links.fetchApiVersionAndStore()
            is ScalaServerConfig.ConfigUrl ->
                coreLogic.getGlobalScope().fetchServerConfigFromDeepLink(scalaServerConfig.customConfigUrl).let {
                    when (it) {
                        is GetServerConfigResult.Success -> it.serverConfigLinks.fetchApiVersionAndStore()
                        is GetServerConfigResult.Failure.Generic -> Either.Left(it.genericFailure)
                    }
                }
            ScalaServerConfig.NoData -> Either.Left(StorageFailure.DataNotFound)
        }

    suspend fun migrate(): MigrationResult {
        return migrateServerConfig()
            .flatMap {
                // TODO implement next migration steps and setMigrationCompleted()
                Either.Right(Unit)
            }
            .mapLeft {
                when (it) {
                    is NetworkFailure.NoNetworkConnection -> MigrationResult.Failure.Type.NO_NETWORK
                    is StorageFailure.DataNotFound -> MigrationResult.Failure.Type.DATA_NOT_FOUND
                    is ServerConfigFailure.UnknownServerVersion -> MigrationResult.Failure.Type.UNKNOWN_SERVER_VERSION
                    is ServerConfigFailure.NewServerVersion -> MigrationResult.Failure.Type.TOO_NEW_VERSION
                    else -> MigrationResult.Failure.Type.UNKNOWN
                }
            }
            .fold({ MigrationResult.Failure(it) }, { MigrationResult.Success })
    }

    companion object {
        private const val SCALA_DB_NAME = "ZGlobal.db"
    }
}

sealed class MigrationResult {
    object Success : MigrationResult()
    data class Failure(val type: Type) : MigrationResult() {
        enum class Type { UNKNOWN_SERVER_VERSION, TOO_NEW_VERSION, DATA_NOT_FOUND, NO_NETWORK, UNKNOWN; }
        companion object {
            const val KEY_FAILURE_TYPE = "failure_type"
        }
    }
}

fun MigrationResult.Failure.Type.toData(): Data = workDataOf(MigrationResult.Failure.KEY_FAILURE_TYPE to this.name)

fun Data.getMigrationFailure(): MigrationResult.Failure.Type = this.getString(MigrationResult.Failure.KEY_FAILURE_TYPE)
    ?.let {
        try {
            MigrationResult.Failure.Type.valueOf(it)
        } catch (e: IllegalArgumentException) {
            null
        }
    } ?: MigrationResult.Failure.Type.UNKNOWN
