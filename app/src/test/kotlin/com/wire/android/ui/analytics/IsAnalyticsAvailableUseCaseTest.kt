/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
 */
package com.wire.android.ui.analytics

import com.wire.android.datastore.UserDataStore
import com.wire.android.datastore.UserDataStoreProvider
import com.wire.android.framework.TestUser
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

class IsAnalyticsAvailableUseCaseTest {

    @ParameterizedTest
    @EnumSource(TestParams::class)
    fun `is Analytics Available - handle accordingly`(params: TestParams) = runTest {
        // given
        val (_, useCase) = Arrangement()
            .withServerConfig(params.serverConfig)
            .withAnalyticsUsageEnabled(params.isAnalyticsUsageEnabled)
            .arrange(analyticsConfiguration = params.analyticsConfiguration)

        advanceUntilIdle()

        // when
        val result = useCase(TestUser.USER_ID)

        // then
        assertEquals(params.expected, result)
    }

    private class Arrangement {
        val dataStore = mockk<UserDataStore>()
        val dataStoreProvider = mockk<UserDataStoreProvider>()
        val selfServerConfig = mockk<SelfServerConfigUseCase>()
        val coreLogic = mockk<CoreLogic>()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { dataStore.setIsAnonymousAnalyticsEnabled(any()) } returns Unit
            coEvery { dataStore.setIsAnalyticsDialogSeen() } returns Unit
            every { dataStoreProvider.getOrCreate(any()) } returns dataStore
            coEvery { coreLogic.getSessionScope(any()).users.serverLinks } returns selfServerConfig
        }

        fun withServerConfig(serverConfig: ServerConfig) = apply {
            coEvery { selfServerConfig() } returns SelfServerConfigUseCase.Result.Success(
                serverLinks = serverConfig
            )
        }

        fun withAnalyticsUsageEnabled(enabled: Boolean) = apply {
            coEvery { dataStore.isAnonymousUsageDataEnabled() } returns flowOf(enabled)
        }

        fun arrange(analyticsConfiguration: AnalyticsConfiguration) = this to IsAnalyticsAvailableUseCase(
            analyticsEnabled = analyticsConfiguration,
            coreLogic = coreLogic,
            userDataStoreProvider = dataStoreProvider
        )
    }

    companion object {
        val PRODUCTION_SERVER_CONFIG = newServerConfig(1).copy(links = ServerConfig.PRODUCTION)
        val STAGING_SERVER_CONFIG = newServerConfig(1).copy(links = ServerConfig.STAGING)
        val CUSTOM_SERVER_CONFIG = newServerConfig(1).copy(links = ServerConfig.DUMMY)

        enum class TestParams(
            val serverConfig: ServerConfig,
            val isAnalyticsUsageEnabled: Boolean,
            val analyticsConfiguration: AnalyticsConfiguration,
            val expected: Boolean
        ) {
            SHOULD_SHOW_DIALOG(
                PRODUCTION_SERVER_CONFIG,
                false,
                AnalyticsConfiguration.Enabled,
                false
            ),
            SHOULD_HIDE_DIALOG(
                STAGING_SERVER_CONFIG,
                false,
                AnalyticsConfiguration.Enabled,
                false
            ),
            ANALYTICS_ALREADY_ENABLED(
                STAGING_SERVER_CONFIG,
                true,
                AnalyticsConfiguration.Enabled,
                true
            ),
            CUSTOM_BACKEND(
                CUSTOM_SERVER_CONFIG,
                true,
                AnalyticsConfiguration.Enabled,
                false
            ),
            ANALYTICS_CONFIGURATION_DISABLED(
                PRODUCTION_SERVER_CONFIG,
                false,
                AnalyticsConfiguration.Disabled,
                false
            )
        }
    }
}
