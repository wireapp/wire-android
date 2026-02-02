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
@file:OptIn(ExperimentalCoroutinesApi::class)

package com.wire.android.ui.settings.debug

import android.content.Context
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.ScopedArgsTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestUser
import com.wire.android.ui.debug.DebugDataOptionsViewModelImpl
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.ui.UIText
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.debug.ObserveIsConsumableNotificationsEnabledUseCase
import com.wire.kalium.logic.feature.debug.RepairFaultyRemovalKeysUseCase
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsResult
import com.wire.kalium.logic.feature.debug.StartUsingAsyncNotificationsUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenError
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenResult
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(ScopedArgsTestExtension::class)
@ExtendWith(CoroutineTestExtension::class)
class DebugDataOptionsViewModelTest {

    @Test
    fun `given token sending token will succeed, when sending FCM token, then info message should emmit success message`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withSendFCMTokenSuccess()
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.forceSendFCMToken()

            // then
            val result = awaitItem()
            assertEquals(UIText.DynamicString("Token registered"), result)
        }
    }

    @Test
    fun `given there is not client ID, when sending FCM token,info message should emit error message`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withSendFCMTokenClientIdFailure()
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.forceSendFCMToken()

            // then
            val result = awaitItem()
            assertEquals(UIText.DynamicString("Can't get client ID, error: error message"), result)
        }
    }

    @Test
    fun `given there is not notification token, when sending FCM token,info message should emit error message`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withSendFCMTokenNotificationTokenFailure()
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.forceSendFCMToken()

            // then
            val result = awaitItem()
            assertEquals(UIText.DynamicString("Can't get notification token, error: error message"), result)
        }
    }

    @Test
    fun `given that there is API failure, when sending FCM token,info message should emit error message`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withSendFCMTokenClientRepositoryRegisterTokenFailure()
            .arrange()

        viewModel.infoMessage.test {
            // when
            viewModel.forceSendFCMToken()

            // then
            val result = awaitItem()
            assertEquals(UIText.DynamicString("Can't register token, error: error message"), result)
        }
    }

    @Test
    fun `given that Proteus protocol is used, view state should have Proteus protocol name`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withProteusProtocolSetup()
            .arrange()

        assertEquals("Proteus", viewModel.state.defaultProtocol)
    }

    @Test
    fun `given that Mls protocol is used, view state should have proteus Mls name`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withMlsProtocolSetup()
            .arrange()

        assertEquals("MLS", viewModel.state.defaultProtocol)
    }

    @Test
    fun `given that federation is disabled, view state should have federation value of false`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withFederationDisabled()
            .arrange()

        assertEquals(false, viewModel.state.isFederationEnabled)
    }

    @Test
    fun `given that federation is enabled, view state should have federation value of true`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withFederationEnabled()
            .arrange()

        assertEquals(true, viewModel.state.isFederationEnabled)
    }

    @Test
    fun `given that api version is unknown, view state should have api version unknown`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withApiVersionUnknown()
            .arrange()

        assertEquals("Unknown", viewModel.state.currentApiVersion)
    }

    @Test
    fun `given that api version is set, view state should have api version set`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withApiVersionSet(7)
            .arrange()

        assertEquals("7", viewModel.state.currentApiVersion)
    }

    @Test
    fun `given server config failure, view state should have default values`() = runTest {
        // given
        val (_, viewModel) = DebugDataOptionsHiltArrangement()
            .withServerConfigError()
            .arrange()

        assertEquals("null", viewModel.state.currentApiVersion)
        assertEquals(false, viewModel.state.isFederationEnabled)
    }

    @Test
    fun `given async notifications is not enabled, when enabling, then start using async notifications is called`() = runTest {
        // given
        val (arrangement, viewModel) = DebugDataOptionsHiltArrangement()
            .withObserveIsConsumableNotificationsEnabled(false)
            .withStartUsingAsyncNotificationsResult()
            .arrange()

        assertEquals(false, viewModel.state.isAsyncNotificationsEnabled)

        viewModel.enableAsyncNotifications(true)

        assertEquals(true, viewModel.state.isAsyncNotificationsEnabled)
        coVerify(exactly = 1) { arrangement.startUsingAsyncNotifications() }
    }

    @Test
    fun `given async notifications is enabled, then start using async notifications is never called`() = runTest {
        // given
        val (arrangement, viewModel) = DebugDataOptionsHiltArrangement()
            .withObserveIsConsumableNotificationsEnabled(true)
            .withStartUsingAsyncNotificationsResult()
            .arrange()

        assertEquals(true, viewModel.state.isAsyncNotificationsEnabled)

        viewModel.enableAsyncNotifications(false)

        assertEquals(true, viewModel.state.isAsyncNotificationsEnabled)
        coVerify(exactly = 0) { arrangement.startUsingAsyncNotifications() }
    }
}

internal class DebugDataOptionsHiltArrangement {

    @MockK(relaxed = true)
    lateinit var context: Context

    private val currentAccount: UserId = TestUser.SELF_USER_ID

    @MockK
    lateinit var updateApiVersions: UpdateApiVersionsScheduler

    @MockK
    lateinit var mlsKeyPackageCount: MLSKeyPackageCountUseCase

    @MockK
    lateinit var restartSlowSyncProcessForRecovery: RestartSlowSyncProcessForRecoveryUseCase

    @MockK
    lateinit var checkCrlRevocationList: CheckCrlRevocationListUseCase

    @MockK
    lateinit var getCurrentAnalyticsTrackingIdentifier: GetCurrentAnalyticsTrackingIdentifierUseCase

    @MockK
    lateinit var selfServerConfigUseCase: SelfServerConfigUseCase

    @MockK
    lateinit var getDefaultProtocolUseCase: GetDefaultProtocolUseCase

    @MockK
    lateinit var sendFCMToken: SendFCMTokenUseCase

    @MockK
    lateinit var observeIsConsumableNotificationsEnabled: ObserveIsConsumableNotificationsEnabledUseCase

    @MockK
    lateinit var startUsingAsyncNotifications: StartUsingAsyncNotificationsUseCase

    @MockK
    lateinit var repairFaultyRemovalKeysUseCase: RepairFaultyRemovalKeysUseCase

    private val viewModel by lazy {
        DebugDataOptionsViewModelImpl(
            context = context,
            currentAccount = currentAccount,
            updateApiVersions = updateApiVersions,
            mlsKeyPackageCount = mlsKeyPackageCount,
            restartSlowSyncProcessForRecovery = restartSlowSyncProcessForRecovery,
            checkCrlRevocationList = checkCrlRevocationList,
            getCurrentAnalyticsTrackingIdentifier = getCurrentAnalyticsTrackingIdentifier,
            sendFCMToken = sendFCMToken,
            dispatcherProvider = TestDispatcherProvider(),
            selfServerConfigUseCase = selfServerConfigUseCase,
            getDefaultProtocolUseCase = getDefaultProtocolUseCase,
            startUsingAsyncNotifications = startUsingAsyncNotifications,
            observeAsyncNotificationsEnabled = observeIsConsumableNotificationsEnabled,
            repairFaultyRemovalKeys = repairFaultyRemovalKeysUseCase
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(UnconfinedTestDispatcher())
        mockkStatic("com.wire.android.util.FileUtilKt")
        runBlocking {
            coEvery {
                mlsKeyPackageCount()
            } returns MLSKeyPackageCountResult.Success(ClientId("clientId"), 1, false)
            coEvery {
                getCurrentAnalyticsTrackingIdentifier()
            } returns "trackingId"
            every {
                context.getDeviceIdString()
            } returns "deviceId"
            every {
                context.getGitBuildId()
            } returns "gitBuildId"
            coEvery {
                selfServerConfigUseCase()
            } returns SelfServerConfigUseCase.Result.Success(
                ServerConfig(
                    id = "id",
                    links = mockk(),
                    metaData = ServerConfig.MetaData(
                        federation = true,
                        commonApiVersion = CommonApiVersionType.Unknown,
                        domain = null,
                    )
                )
            )
            every {
                getDefaultProtocolUseCase()
            } returns SupportedProtocol.PROTEUS

            withObserveIsConsumableNotificationsEnabled(false)
        }
    }

    suspend fun withObserveIsConsumableNotificationsEnabled(isEnabled: Boolean = false) = apply {
        coEvery {
            observeIsConsumableNotificationsEnabled()
        } returns flowOf(isEnabled)
    }

    suspend fun withStartUsingAsyncNotificationsResult(
        result: StartUsingAsyncNotificationsResult = StartUsingAsyncNotificationsResult.Success
    ) = apply {
        coEvery { startUsingAsyncNotifications() } returns result
    }

    fun withSendFCMTokenSuccess() = apply {
        coEvery {
            sendFCMToken()
        } returns SendFCMTokenResult.Success
    }

    suspend fun withSendFCMTokenClientIdFailure() = apply {
        coEvery {
            sendFCMToken()
        } returns SendFCMTokenResult.Failure(SendFCMTokenError(SendFCMTokenError.Reason.CANT_GET_CLIENT_ID, "error message"))
    }

    suspend fun withSendFCMTokenNotificationTokenFailure() = apply {
        coEvery {
            sendFCMToken()
        } returns SendFCMTokenResult.Failure(SendFCMTokenError(SendFCMTokenError.Reason.CANT_GET_NOTIFICATION_TOKEN, "error message"))
    }

    suspend fun withSendFCMTokenClientRepositoryRegisterTokenFailure() = apply {
        coEvery {
            sendFCMToken()
        } returns SendFCMTokenResult.Failure(SendFCMTokenError(SendFCMTokenError.Reason.CANT_REGISTER_TOKEN, "error message"))
    }

    fun withProteusProtocolSetup() = apply {
        every {
            getDefaultProtocolUseCase()
        } returns SupportedProtocol.PROTEUS
    }

    fun withMlsProtocolSetup() = apply {
        every {
            getDefaultProtocolUseCase()
        } returns SupportedProtocol.MLS
    }

    fun withFederationEnabled() = apply {
        coEvery {
            selfServerConfigUseCase()
        } returns SelfServerConfigUseCase.Result.Success(
            ServerConfig(
                id = "id",
                links = mockk(),
                metaData = ServerConfig.MetaData(
                    federation = true,
                    commonApiVersion = CommonApiVersionType.Unknown,
                    domain = null,
                )
            )
        )
    }

    fun withFederationDisabled() = apply {
        coEvery {
            selfServerConfigUseCase()
        } returns SelfServerConfigUseCase.Result.Success(
            ServerConfig(
                id = "id",
                links = mockk(),
                metaData = ServerConfig.MetaData(
                    federation = false,
                    commonApiVersion = CommonApiVersionType.Unknown,
                    domain = null,
                )
            )
        )
    }

    fun withApiVersionUnknown() = apply {
        coEvery {
            selfServerConfigUseCase()
        } returns SelfServerConfigUseCase.Result.Success(
            ServerConfig(
                id = "id",
                links = mockk(),
                metaData = ServerConfig.MetaData(
                    federation = true,
                    commonApiVersion = CommonApiVersionType.Unknown,
                    domain = null,
                )
            )
        )
    }

    fun withApiVersionSet(version: Int) = apply {
        coEvery {
            selfServerConfigUseCase()
        } returns SelfServerConfigUseCase.Result.Success(
            ServerConfig(
                id = "id",
                links = mockk(),
                metaData = ServerConfig.MetaData(
                    federation = true,
                    commonApiVersion = CommonApiVersionType.Valid(version),
                    domain = null,
                )
            )
        )
    }

    fun withServerConfigError() = apply {
        coEvery {
            selfServerConfigUseCase()
        } returns SelfServerConfigUseCase.Result.Failure(
            CoreFailure.Unknown(IllegalStateException())
        )
    }

    fun arrange() = this to viewModel
}
