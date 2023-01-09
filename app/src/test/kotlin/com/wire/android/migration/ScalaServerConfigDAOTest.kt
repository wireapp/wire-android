package com.wire.android.migration

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.migration.preference.ScalaBackendPreferences
import com.wire.android.migration.preference.ScalaServerConfig
import com.wire.android.migration.preference.ScalaServerConfigDAO
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import org.amshove.kluent.internal.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ScalaServerConfigDAOTest {

    @Test
    fun givenAllRequiredData_whenRetrievingServerConfig_thenReturnFullResult() {
        val (_, dao) = Arrangement().withLinks().withVersionInfo().arrange()
        assert(dao.scalaServerConfig is ScalaServerConfig.Full)
        assertEquals(Arrangement.links, (dao.scalaServerConfig as ScalaServerConfig.Full).links)
        assertEquals(Arrangement.versionInfo, (dao.scalaServerConfig as ScalaServerConfig.Full).versionInfo)
    }

    @Test
    fun givenOnlyLinksData_whenRetrievingServerConfig_thenReturnLinksResult() {
        val (_, dao) = Arrangement().withLinks().arrange()
        assert(dao.scalaServerConfig is ScalaServerConfig.Links)
        assertEquals(Arrangement.links, (dao.scalaServerConfig as ScalaServerConfig.Links).links)
    }
    
    @Test
    fun givenOnlyConfigUrlData_whenRetrievingServerConfig_thenReturnConfigUrlResult() {
        val (_, dao) = Arrangement().withCustomConfig().arrange()
        assert(dao.scalaServerConfig is ScalaServerConfig.ConfigUrl)
        assertEquals(Arrangement.customConfigUrl, (dao.scalaServerConfig as ScalaServerConfig.ConfigUrl).customConfigUrl)
    }
    
    @Test
    fun givenNoData_whenRetrievingServerConfig_thenReturnNoDataResult() {
        val (_, dao) = Arrangement().arrange()
        assertEquals(ScalaServerConfig.NoData, dao.scalaServerConfig)
    }

    private class Arrangement {

        @MockK
        lateinit var scalaBackendPreferences: ScalaBackendPreferences

        private val dao: ScalaServerConfigDAO by lazy {
            ScalaServerConfigDAO(scalaBackendPreferences)
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            every { scalaBackendPreferences.environment } returns null
            every { scalaBackendPreferences.baseUrl } returns null
            every { scalaBackendPreferences.websocketUrl } returns null
            every { scalaBackendPreferences.blacklistUrl } returns null
            every { scalaBackendPreferences.teamsUrl } returns null
            every { scalaBackendPreferences.accountsUrl } returns null
            every { scalaBackendPreferences.websiteUrl } returns null
            every { scalaBackendPreferences.apiVersion } returns null
            every { scalaBackendPreferences.customConfigUrl } returns null
        }

        fun withLinks(): Arrangement {
            every { scalaBackendPreferences.environment } returns links.title
            every { scalaBackendPreferences.baseUrl } returns links.api
            every { scalaBackendPreferences.websocketUrl } returns links.webSocket
            every { scalaBackendPreferences.blacklistUrl } returns links.blackList
            every { scalaBackendPreferences.teamsUrl } returns links.teams
            every { scalaBackendPreferences.accountsUrl } returns links.accounts
            every { scalaBackendPreferences.websiteUrl } returns links.website
            return this
        }

        fun withVersionInfo(): Arrangement {
            every { scalaBackendPreferences.apiVersion } returns versionInfoJson
            return this
        }

        fun withCustomConfig(): Arrangement {
            every { scalaBackendPreferences.customConfigUrl } returns customConfigUrl
            return this
        }

        fun arrange() = this to dao

        
        companion object {
            const val customConfigUrl = "customConfigUrl"
            val links = newServerConfig(1).links
            val versionInfo = ServerConfig.VersionInfo(true, listOf(0, 1, 2), "wire.com", listOf(2))
            const val versionInfoJson = "{ \"domain\": \"wire.com\", \"federation\": true, \"supported\": [0, 1, 2], \"development\": [2] }"
        }
    }
}

