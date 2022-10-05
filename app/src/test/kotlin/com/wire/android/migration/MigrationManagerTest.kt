package com.wire.android.migration

import android.content.Context
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.GlobalKaliumScope
import com.wire.kalium.logic.StorageFailure
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.server.FetchApiVersionResult
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
class MigrationManagerTest {

    @Test
    fun givenFullDataAndSuccessfulRequests_whenRetrievingServerConfig_thenSaveWithSuccess() = runTest {
        val expected = Arrangement.serverConfig
        val versionInfo = Arrangement.versionInfo
        val (arrangement, manager) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.Full(expected.links, versionInfo))
            .withStoreServerConfigResult(StoreServerConfigResult.Success(expected))
            .arrange()
        val result = manager.migrateServerConfig()
        coVerify(exactly = 1) { arrangement.globalKaliumScope.storeServerConfig(expected.links, versionInfo) }
        coVerify { arrangement.globalKaliumScope.fetchApiVersion(any()) wasNot Called }
        assert(result.isRight())
        assertEquals(expected, (result as Either.Right).value)
    }

    @Test
    fun givenLinksDataAndSuccessfulRequests_whenRetrievingServerConfig_thenMakeProperRequestsAndSaveWithSuccess() = runTest {
        val expected = Arrangement.serverConfig
        val (arrangement, manager) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.Links(expected.links))
            .withFetchApiVersionResult(FetchApiVersionResult.Success(expected))
            .arrange()
        val result = manager.migrateServerConfig()
        coVerify(exactly = 1) { arrangement.globalKaliumScope.fetchApiVersion(expected.links) }
        assert(result.isRight())
        assertEquals(expected, (result as Either.Right).value)
    }

    @Test
    fun givenConfigUrlDataAndSuccessfulRequests_whenRetrievingServerConfig_thenMakeProperRequestsAndSaveWithSuccess() = runTest {
        val customConfigUrl = Arrangement.customConfigUrl
        val expected = Arrangement.serverConfig
        val (arrangement, manager) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.ConfigUrl(customConfigUrl))
            .withFetchServerConfigFromDeepLinkResult(GetServerConfigResult.Success(expected.links))
            .withFetchApiVersionResult(FetchApiVersionResult.Success(expected))
            .arrange()
        val result = manager.migrateServerConfig()
        coVerify(exactly = 1) { arrangement.globalKaliumScope.fetchServerConfigFromDeepLink(customConfigUrl) }
        coVerify(exactly = 1) { arrangement.globalKaliumScope.fetchApiVersion(expected.links) }
        assert(result.isRight())
        assertEquals(expected, (result as Either.Right).value)
    }

    @Test
    fun givenNoData_whenRetrievingServerConfig_thenDoNotSaveAndReturnNoData() = runTest {
        val (arrangement, manager) = Arrangement()
            .withScalaServerConfig(ScalaServerConfig.NoData)
            .arrange()
        val result = manager.migrateServerConfig()
        coVerify { arrangement.globalKaliumScope.storeServerConfig(any(), any()) wasNot Called }
        assert(result.isLeft())
        assertEquals(StorageFailure.DataNotFound, (result as Either.Left).value)
    }

    private class Arrangement() {

        @MockK
        lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var applicationContext: Context

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var scalaServerConfigDAO: ScalaServerConfigDAO

        @MockK
        lateinit var globalKaliumScope: GlobalKaliumScope

        private val manager: MigrationManager by lazy {
            MigrationManager(coreLogic, applicationContext, globalDataStore, scalaServerConfigDAO)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { coreLogic.getGlobalScope() } returns globalKaliumScope
        }

        fun withScalaServerConfig(scalaServerConfig: ScalaServerConfig): Arrangement {
            every { scalaServerConfigDAO.scalaServerConfig } returns scalaServerConfig
            return this
        }
        fun withStoreServerConfigResult(result : StoreServerConfigResult): Arrangement {
            coEvery { globalKaliumScope.storeServerConfig(any(), any()) } returns result
            return this
        }
        fun withFetchServerConfigFromDeepLinkResult(result : GetServerConfigResult): Arrangement {
            coEvery { globalKaliumScope.fetchServerConfigFromDeepLink(any()) } returns result
            return this
        }
        fun withFetchApiVersionResult(result : FetchApiVersionResult): Arrangement {
            coEvery { globalKaliumScope.fetchApiVersion(any()) } returns result
            return this
        }

        fun arrange() = this to manager

        companion object {
            const val customConfigUrl = "customConfigUrl"
            val serverConfig = newServerConfig(1)
            val versionInfo = ServerConfig.VersionInfo(true, listOf(0, 1, 2), "wire.com", listOf(2))
        }
    }
}
