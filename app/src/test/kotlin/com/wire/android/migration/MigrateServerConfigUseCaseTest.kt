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

package com.wire.android.migration

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.migration.feature.MigrateServerConfigUseCase
import com.wire.android.migration.preference.ScalaServerConfig
import com.wire.android.migration.preference.ScalaServerConfigDAO
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AuthenticationScope
import com.wire.kalium.logic.feature.auth.autoVersioningAuth.AutoVersionAuthScopeUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.StoreServerConfigResult
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.functional.isLeft
import com.wire.kalium.logic.functional.isRight
import io.mockk.Called
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MigrateServerConfigUseCaseTest {

    @Test
    fun givenFullDataAndSuccessfulRequests_whenRetrievingServerConfig_thenSaveWithSuccess() = runTest {
        val expected = Arrangement.serverConfig
        val versionInfo = Arrangement.versionInfo
        val (arrangement, useCase) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.Full(expected.links, versionInfo))
            .withStoreServerConfigResult(StoreServerConfigResult.Success(expected))
            .arrange()
        val result = useCase()
        coVerify(exactly = 1) { arrangement.globalKaliumScope.storeServerConfig(expected.links, versionInfo) }
        assert(result.isRight())
        assertEquals(expected, (result as Either.Right).value)
    }

    @Test
    fun givenLinksDataAndSuccessfulRequests_whenRetrievingServerConfig_thenMakeProperRequestsAndSaveWithSuccess() = runTest {
        val expected = Arrangement.serverConfig
        val (arrangement, useCase) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.Links(expected.links))
            .withCurrentServerConfig(expected)
            .arrange()

        val result = useCase()
        assert(result.isRight())
        assertEquals(expected, (result as Either.Right).value)
    }

    @Test
    fun givenConfigUrlDataAndSuccessfulRequests_whenRetrievingServerConfig_thenMakeProperRequestsAndSaveWithSuccess() = runTest {
        val customConfigUrl = Arrangement.customConfigUrl
        val expected = Arrangement.serverConfig
        val (arrangement, useCase) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.ConfigUrl(customConfigUrl))
            .withFetchServerConfigFromDeepLinkResult(GetServerConfigResult.Success(expected.links))
            .withCurrentServerConfig(expected)
            .arrange()

        val result = useCase()
        coVerify(exactly = 1) { arrangement.globalKaliumScope.fetchServerConfigFromDeepLink(customConfigUrl) }
        assert(result.isRight())
        assertEquals(expected, (result as Either.Right).value)
    }

    @Test
    fun givenNoData_whenRetrievingServerConfig_thenDoNotSaveAndReturnNoData() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.NoData)
            .arrange()
        val result = useCase()
        coVerify { arrangement.globalKaliumScope.storeServerConfig(any(), any()) wasNot Called }
        assert(result.isLeft())
        assertEquals(StorageFailure.DataNotFound, (result as Either.Left).value)
    }

    private class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var scalaServerConfigDAO: ScalaServerConfigDAO

        @MockK
        lateinit var globalKaliumScope: GlobalKaliumScope

        private val useCase: MigrateServerConfigUseCase by lazy {
            MigrateServerConfigUseCase(coreLogic, scalaServerConfigDAO)
        }

        @MockK
        lateinit var autoVersionAuthScopeUseCase: AutoVersionAuthScopeUseCase

        @MockK
        lateinit var authScope: AuthenticationScope

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getGlobalScope() } returns globalKaliumScope
            every { coreLogic.versionedAuthenticationScope(any()) } returns autoVersionAuthScopeUseCase
            coEvery { autoVersionAuthScopeUseCase(any()) } returns AutoVersionAuthScopeUseCase.Result.Success(authScope)
        }

        fun withCurrentServerConfig(serverConfig: ServerConfig) = apply {
            every { authScope.currentServerConfig() } returns serverConfig
        }

        fun withScalaServerConfig(scalaServerConfig: ScalaServerConfig): Arrangement {
            every { scalaServerConfigDAO.scalaServerConfig } returns scalaServerConfig
            return this
        }

        fun withStoreServerConfigResult(result: StoreServerConfigResult): Arrangement {
            coEvery { globalKaliumScope.storeServerConfig(any(), any()) } returns result
            return this
        }

        fun withFetchServerConfigFromDeepLinkResult(result: GetServerConfigResult): Arrangement {
            coEvery { globalKaliumScope.fetchServerConfigFromDeepLink(any()) } returns result
            return this
        }

        fun arrange() = this to useCase

        companion object {
            const val customConfigUrl = "customConfigUrl"
            val serverConfig = newServerConfig(1)
            val versionInfo = ServerConfig.VersionInfo(true, listOf(0, 1, 2), "wire.com", listOf(2))
        }
    }
}
