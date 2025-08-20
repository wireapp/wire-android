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

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.datastore.UserDataStore
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@ExtendWith(CoroutineTestExtension::class)
class AnalyticsUsageViewModelTest {

    @ParameterizedTest
    @EnumSource(TestParams::class)
    fun `should hide or show Analytics Usage dialog - handle accordingly`(params: TestParams) = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withServerConfig(params.serverConfig)
            .withAnalyticsUsageEnabled(params.isAnalyticsUsageEnabled)
            .withIsDialogSeen(params.isDialogSeen)
            .arrange(analyticsConfiguration = params.analyticsConfiguration)

        advanceUntilIdle()

        // when
        // then
        assertEquals(params.expected, viewModel.state.shouldDisplayDialog)
    }

    @Test
    fun `given dialog is shown, when user agrees to analytics usage, then setting analytics to enabled and dialog to seen`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withServerConfig(STAGING_SERVER_CONFIG)
            .withAnalyticsUsageEnabled(false)
            .withIsDialogSeen(false)
            .arrange(analyticsConfiguration = AnalyticsConfiguration.Enabled)

        advanceUntilIdle()
        assertEquals(true, viewModel.state.shouldDisplayDialog)

        // when

        viewModel.agreeAnalyticsUsage()

        // then
        coVerify { arrangement.dataStore.setIsAnonymousAnalyticsEnabled(enabled = true) }
        coVerify { arrangement.dataStore.setIsAnalyticsDialogSeen() }
        assertEquals(false, viewModel.state.shouldDisplayDialog)
    }

    @Test
    fun `given dialog is shown, when user declines analytics usage, then setting analytics to disabled and dialog to seen`() = runTest {
        // given
        val (arrangement, viewModel) = Arrangement()
            .withServerConfig(PRODUCTION_SERVER_CONFIG)
            .withAnalyticsUsageEnabled(false)
            .withIsDialogSeen(false)
            .arrange(analyticsConfiguration = AnalyticsConfiguration.Enabled)

        advanceUntilIdle()
        assertEquals(true, viewModel.state.shouldDisplayDialog)

        // when

        viewModel.declineAnalyticsUsage()

        // then
        coVerify { arrangement.dataStore.setIsAnonymousAnalyticsEnabled(enabled = false) }
        coVerify { arrangement.dataStore.setIsAnalyticsDialogSeen() }
        assertEquals(false, viewModel.state.shouldDisplayDialog)
    }

    private class Arrangement {
        val dataStore = mockk<UserDataStore>()
        val selfServerConfig = mockk<SelfServerConfigUseCase>()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { dataStore.setIsAnonymousAnalyticsEnabled(any()) } returns Unit
            coEvery { dataStore.setIsAnalyticsDialogSeen() } returns Unit
        }

        fun withServerConfig(serverConfig: ServerConfig) = apply {
            coEvery { selfServerConfig() } returns SelfServerConfigUseCase.Result.Success(
                serverLinks = serverConfig
            )
        }
        fun withAnalyticsUsageEnabled(enabled: Boolean) = apply {
            coEvery { dataStore.isAnonymousUsageDataEnabled() } returns flowOf(enabled)
        }
        fun withIsDialogSeen(enabled: Boolean) = apply {
            coEvery { dataStore.isAnalyticsDialogSeen() } returns flowOf(enabled)
        }

        fun arrange(analyticsConfiguration: AnalyticsConfiguration) = this to AnalyticsUsageViewModel(
            analyticsEnabled = analyticsConfiguration,
            dataStore = { dataStore },
            selfServerConfig = { selfServerConfig },
        )
    }

    companion object {
        val PRODUCTION_SERVER_CONFIG = newServerConfig(1).copy(links = ServerConfig.PRODUCTION)
        val STAGING_SERVER_CONFIG = newServerConfig(1).copy(links = ServerConfig.STAGING)
        val CUSTOM_SERVER_CONFIG = newServerConfig(1).copy(links = ServerConfig.DUMMY)

        enum class TestParams(
            val serverConfig: ServerConfig,
            val isAnalyticsUsageEnabled: Boolean,
            val isDialogSeen: Boolean,
            val analyticsConfiguration: AnalyticsConfiguration,
            val expected: Boolean
        ) {
            SHOULD_SHOW_DIALOG(
                PRODUCTION_SERVER_CONFIG,
                false,
                false,
                AnalyticsConfiguration.Enabled,
                true
            ),
            SHOULD_HIDE_DIALOG(
                STAGING_SERVER_CONFIG,
                false,
                true,
                AnalyticsConfiguration.Enabled,
                false
            ),
            ANALYTICS_ALREADY_ENABLED(
                STAGING_SERVER_CONFIG,
                true,
                true,
                AnalyticsConfiguration.Enabled,
                false
            ),
            CUSTOM_BACKEND(
                CUSTOM_SERVER_CONFIG,
                false,
                false,
                AnalyticsConfiguration.Enabled,
                false
            ),
            ANALYTICS_CONFIGURATION_DISABLED(
                PRODUCTION_SERVER_CONFIG,
                false,
                false,
                AnalyticsConfiguration.Disabled,
                false
            )
        }
    }
}
