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
package com.wire.android.ui.home.settings.privacy

import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.datastore.UserDataStore
import com.wire.android.ui.analytics.AnalyticsConfiguration
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.feature.user.readReceipts.ObserveReadReceiptsEnabledUseCase
import com.wire.kalium.logic.feature.user.readReceipts.PersistReadReceiptsStatusConfigUseCase
import com.wire.kalium.logic.feature.user.readReceipts.ReadReceiptStatusConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.PersistScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.ObserveTypingIndicatorEnabledUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.PersistTypingIndicatorStatusConfigUseCase
import com.wire.kalium.logic.feature.user.typingIndicator.TypingIndicatorConfigResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class PrivacySettingsViewModelTest {

    @Test
    fun `given user opens privacy settings screen, when viewing anonymous usage data, then its value is enabled`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withEnabledAnonymousUsageData()
            .arrange()

        // when
        // then
        assertEquals(
            true,
            viewModel.state.isAnalyticsUsageEnabled
        )
    }

    @Test
    fun `given user opens privacy settings screen, when viewing anonymous usage data, then its value is not enabled`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withDisabledAnonymousUsageData()
            .arrange()

        // when
        // then
        assertEquals(
            false,
            viewModel.state.isAnalyticsUsageEnabled
        )
    }

    @Test
    fun `given anonymous usage data is set to true, when setting to false, then correct value is propagated`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withEnabledAnonymousUsageData()
            .arrange()

        // when
        viewModel.setAnonymousUsageDataEnabled(enabled = false)

        // then
        assertEquals(
            false,
            viewModel.state.isAnalyticsUsageEnabled
        )
    }

    @Test
    fun `given anonymous usage data is set to false, when setting to true, then correct value is propagated`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withDisabledAnonymousUsageData()
            .arrange()

        // when
        viewModel.setAnonymousUsageDataEnabled(enabled = true)

        // then
        assertEquals(
            true,
            viewModel.state.isAnalyticsUsageEnabled
        )
    }

    private class Arrangement {
        val persistReadReceiptsStatusConfig = mockk<PersistReadReceiptsStatusConfigUseCase>()
        val observeReadReceiptsEnabled = mockk<ObserveReadReceiptsEnabledUseCase>()
        val persistScreenshotCensoringConfig = mockk<PersistScreenshotCensoringConfigUseCase>()
        val observeScreenshotCensoringConfig = mockk<ObserveScreenshotCensoringConfigUseCase>()
        val persistTypingIndicatorStatusConfig = mockk<PersistTypingIndicatorStatusConfigUseCase>()
        val observeTypingIndicatorEnabled = mockk<ObserveTypingIndicatorEnabledUseCase>()
        val selfServerConfig = mockk<SelfServerConfigUseCase>()
        val dataStore = mockk<UserDataStore>()

        val viewModel by lazy {
            PrivacySettingsViewModel(
                dispatchers = TestDispatcherProvider(),
                persistReadReceiptsStatusConfig = persistReadReceiptsStatusConfig,
                observeReadReceiptsEnabled = observeReadReceiptsEnabled,
                persistScreenshotCensoringConfig = persistScreenshotCensoringConfig,
                observeScreenshotCensoringConfig = observeScreenshotCensoringConfig,
                persistTypingIndicatorStatusConfig = persistTypingIndicatorStatusConfig,
                observeTypingIndicatorEnabled = observeTypingIndicatorEnabled,
                analyticsEnabled = AnalyticsConfiguration.Enabled,
                selfServerConfig = selfServerConfig,
                dataStore = dataStore
            )
        }

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { persistReadReceiptsStatusConfig.invoke(true) } returns ReadReceiptStatusConfigResult.Success
            coEvery { observeReadReceiptsEnabled() } returns flowOf(true)
            coEvery { persistScreenshotCensoringConfig.invoke(true) } returns PersistScreenshotCensoringConfigResult.Success
            coEvery { observeScreenshotCensoringConfig() } returns
                    flowOf(ObserveScreenshotCensoringConfigResult.Enabled.ChosenByUser)
            coEvery { persistTypingIndicatorStatusConfig.invoke(true) } returns TypingIndicatorConfigResult.Success
            coEvery { observeTypingIndicatorEnabled() } returns flowOf(true)
            coEvery { dataStore.setIsAnonymousAnalyticsEnabled(any()) } returns Unit
            coEvery { selfServerConfig.invoke() } returns SelfServerConfigUseCase.Result.Success(
                serverLinks = newServerConfig(1).copy(links = ServerConfig.STAGING)
            )
        }

        fun withEnabledAnonymousUsageData() = apply {
            every { dataStore.isAnonymousUsageDataEnabled() } returns flowOf(true)
        }

        fun withDisabledAnonymousUsageData() = apply {
            every { dataStore.isAnonymousUsageDataEnabled() } returns flowOf(false)
        }

        fun arrange() = this to viewModel
    }
}
