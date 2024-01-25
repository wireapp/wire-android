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

package com.wire.android.migration.feature

import com.wire.android.di.KaliumCoreLogic
import com.wire.android.migration.preference.ScalaServerConfig
import com.wire.android.migration.preference.ScalaServerConfigDAO
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.failure.ServerConfigFailure
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
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
            is ScalaServerConfig.Full ->
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
        // scala did not support proxy mode so we can pass null here
        coreLogic.versionedAuthenticationScope(this)(null).let { // it also already stores the fetched config
            when (it) {
                is AutoVersionAuthScopeUseCase.Result.Failure.Generic -> Either.Left(it.genericFailure)
                AutoVersionAuthScopeUseCase.Result.Failure.TooNewVersion -> Either.Left(ServerConfigFailure.NewServerVersion)
                AutoVersionAuthScopeUseCase.Result.Failure.UnknownServerVersion -> Either.Left(ServerConfigFailure.UnknownServerVersion)
                is AutoVersionAuthScopeUseCase.Result.Success -> Either.Right(TODO())
            }
        }
}
