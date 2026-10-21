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
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.verify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.EnumSource

@OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
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

    @ParameterizedTest
    @EnumSource(TestParams::class)
    fun `captured analytics gate preserves the public configuration matrix`(params: TestParams) = runTest {
        val (arrangement, useCase) = Arrangement().withServerConfig(params.serverConfig)
            .withAnalyticsUsageEnabled(params.isAnalyticsUsageEnabled).arrange(params.analyticsConfiguration)
        assertEquals(params.expected, useCase(TestUser.USER_ID, arrangement.session))
        coVerify(exactly = 1) { arrangement.selfServerConfig() }
        verify(exactly = 1) { arrangement.dataStoreProvider.getOrCreate(TestUser.USER_ID) }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `captured analytics server failure propagates`() = runTest {
        val (arrangement, useCase) = Arrangement().withAnalyticsUsageEnabled(true).arrange(AnalyticsConfiguration.Enabled)
        val failure = IllegalStateException("server config failed")
        coEvery { arrangement.selfServerConfig() } throws failure
        val observed = try {
            useCase(TestUser.USER_ID, arrangement.session)
            error("expected failure")
        } catch (actual: IllegalStateException) {
            actual
        }
        assertEquals(failure, observed)
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `caller cancellation while preferences are pending cannot acquire a resource`() = runTest {
        val preferences = MutableSharedFlow<Boolean>()
        val (arrangement, useCase) = Arrangement().arrange(AnalyticsConfiguration.Enabled)
        coEvery { arrangement.dataStore.isAnonymousUsageDataEnabled() } returns preferences
        val request = async { useCase(TestUser.USER_ID, arrangement.session) }
        runCurrent()
        request.cancel()
        request.join()
        assertTrue(request.isCancelled)
        coVerify(exactly = 0) { arrangement.selfServerConfig() }
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
    }

    @Test
    fun `public analytics gate retains its fresh scope lookup after preferences arrive`() = runTest {
        val preferences = MutableSharedFlow<Boolean>()
        val (arrangement, useCase) = Arrangement().withServerConfig(PRODUCTION_SERVER_CONFIG).arrange(AnalyticsConfiguration.Enabled)
        coEvery { arrangement.dataStore.isAnonymousUsageDataEnabled() } returns preferences
        val request = async { useCase(TestUser.USER_ID) }
        runCurrent()
        verify(exactly = 0) { arrangement.coreLogic.getSessionScope(any()) }
        preferences.emit(true)
        assertTrue(request.await())
        verify(exactly = 1) { arrangement.coreLogic.getSessionScope(TestUser.USER_ID) }
    }

    private class Arrangement {
        val dataStore = mockk<UserDataStore>()
        val dataStoreProvider = mockk<UserDataStoreProvider>()
        val selfServerConfig = mockk<SelfServerConfigUseCase>()
        val coreLogic = mockk<CoreLogic>()
        val session = mockk<UserSessionScope>()

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)

            coEvery { dataStore.setIsAnonymousAnalyticsEnabled(any()) } returns Unit
            coEvery { dataStore.setIsAnalyticsDialogSeen() } returns Unit
            every { dataStoreProvider.getOrCreate(any()) } returns dataStore
            every { coreLogic.getSessionScope(any()) } returns session
            every { session.users.serverLinks } returns selfServerConfig
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
