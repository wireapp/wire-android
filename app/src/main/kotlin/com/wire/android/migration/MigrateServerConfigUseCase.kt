package com.wire.android.migration

import com.wire.android.di.KaliumCoreLogic
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.failure.ServerConfigFailure
import com.wire.kalium.logic.feature.server.FetchApiVersionResult
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.StoreServerConfigResult
import com.wire.kalium.logic.functional.Either
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MigrateServerConfigUseCase @Inject constructor(
    @KaliumCoreLogic private val coreLogic: CoreLogic,
    private val scalaServerConfigDAO: ScalaServerConfigDAO,
) {
    suspend operator fun invoke(): Either<CoreFailure, ServerConfig> =
        when (val scalaServerConfig = scalaServerConfigDAO.scalaServerConfig) {
            is ScalaServerConfig.Full -> // TODO what to do when versionInfo.domain is null?
                coreLogic.getGlobalScope().storeServerConfig(scalaServerConfig.links, scalaServerConfig.versionInfo).handleResult()
            is ScalaServerConfig.Links ->
                scalaServerConfig.links.fetchApiVersionAndStore()
            is ScalaServerConfig.ConfigUrl ->
                coreLogic.getGlobalScope().fetchServerConfigFromDeepLink(scalaServerConfig.customConfigUrl).handleResult()
            ScalaServerConfig.NoData ->
                Either.Left(StorageFailure.DataNotFound)
        }

    private suspend fun StoreServerConfigResult.handleResult() = when (this) {
        is StoreServerConfigResult.Success ->
            when (serverConfig.metaData.commonApiVersion) {
                is CommonApiVersionType.Valid -> Either.Right(serverConfig) // if valid just return success
                else -> serverConfig.links.fetchApiVersionAndStore() // else try to fetch and update the info
            }
        is StoreServerConfigResult.Failure.Generic -> Either.Left(genericFailure)
    }

    private suspend fun GetServerConfigResult.handleResult() = when(this) {
        is GetServerConfigResult.Success -> serverConfigLinks.fetchApiVersionAndStore()
        is GetServerConfigResult.Failure.Generic -> Either.Left(genericFailure)
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

}
