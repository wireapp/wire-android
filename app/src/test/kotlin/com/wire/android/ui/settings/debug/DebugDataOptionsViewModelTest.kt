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
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.framework.TestUser
import com.wire.android.migration.failure.UserMigrationStatus
import com.wire.android.ui.debug.DebugDataOptionsViewModelImpl
import com.wire.android.util.getDeviceIdString
import com.wire.android.util.getGitBuildId
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.configuration.server.CommonApiVersionType
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.data.conversation.ClientId
import com.wire.kalium.logic.data.user.SupportedProtocol
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.analytics.GetCurrentAnalyticsTrackingIdentifierUseCase
import com.wire.kalium.logic.feature.e2ei.CheckCrlRevocationListUseCase
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountResult
import com.wire.kalium.logic.feature.keypackage.MLSKeyPackageCountUseCase
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenError
import com.wire.kalium.logic.feature.notificationToken.SendFCMTokenUseCase
import com.wire.kalium.logic.feature.user.GetDefaultProtocolUseCase
import com.wire.kalium.logic.feature.user.SelfServerConfigUseCase
import com.wire.kalium.logic.functional.Either
import com.wire.kalium.logic.sync.periodic.UpdateApiVersionsScheduler
import com.wire.kalium.logic.sync.slow.RestartSlowSyncProcessForRecoveryUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.amshove.kluent.internal.assertEquals
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
}

internal class DebugDataOptionsHiltArrangement {

    @MockK(relaxed = true)
    lateinit var context: Context

    private val currentAccount: UserId = TestUser.SELF_USER_ID

    @MockK
    lateinit var globalDataStore: GlobalDataStore

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

    private val viewModel by lazy {
        DebugDataOptionsViewModelImpl(
            context = context,
            currentAccount = currentAccount,
            globalDataStore = globalDataStore,
            updateApiVersions = updateApiVersions,
            mlsKeyPackageCount = mlsKeyPackageCount,
            restartSlowSyncProcessForRecovery = restartSlowSyncProcessForRecovery,
            checkCrlRevocationList = checkCrlRevocationList,
            getCurrentAnalyticsTrackingIdentifier = getCurrentAnalyticsTrackingIdentifier,
            sendFCMToken = sendFCMToken,
            dispatcherProvider = TestDispatcherProvider(),
            selfServerConfigUseCase = selfServerConfigUseCase,
            getDefaultProtocolUseCase = getDefaultProtocolUseCase,
        )
    }

    init {
        MockKAnnotations.init(this, relaxUnitFun = true)
        Dispatchers.setMain(UnconfinedTestDispatcher())

        every {
            globalDataStore.isEncryptedProteusStorageEnabled()
        } returns flowOf(true)
        coEvery {
            mlsKeyPackageCount()
        } returns MLSKeyPackageCountResult.Success(ClientId("clientId"), 1, false)
        coEvery {
            getCurrentAnalyticsTrackingIdentifier()
        } returns "trackingId"
        mockkStatic("com.wire.android.util.FileUtilKt")
        every {
            context.getDeviceIdString()
        } returns "deviceId"
        every {
            context.getGitBuildId()
        } returns "gitBuildId"
        coEvery {
            globalDataStore.getUserMigrationStatus(TestUser.SELF_USER_ID.value)
        } returns flowOf(UserMigrationStatus.NoNeed)
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
    }

    fun arrange() = this to viewModel

    fun withSendFCMTokenSuccess() = apply {
        coEvery {
            sendFCMToken()
        } returns Either.Right(Unit)
    }

    suspend fun withSendFCMTokenClientIdFailure() = apply {
        coEvery {
            sendFCMToken()
        } returns Either.Left(SendFCMTokenError(SendFCMTokenError.Reason.CANT_GET_CLIENT_ID, "error message"))
    }

    suspend fun withSendFCMTokenNotificationTokenFailure() = apply {
        coEvery {
            sendFCMToken()
        } returns Either.Left(SendFCMTokenError(SendFCMTokenError.Reason.CANT_GET_NOTIFICATION_TOKEN, "error message"))
    }

    suspend fun withSendFCMTokenClientRepositoryRegisterTokenFailure() = apply {
        coEvery {
            sendFCMToken()
        } returns Either.Left(SendFCMTokenError(SendFCMTokenError.Reason.CANT_REGISTER_TOKEN, "error message"))
    }
}
