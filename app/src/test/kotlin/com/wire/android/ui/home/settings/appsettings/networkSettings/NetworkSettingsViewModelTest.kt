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
package com.wire.android.ui.home.settings.appsettings.networkSettings

import android.content.Context
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.emm.ManagedConfigurationsManager
import com.wire.android.util.isWebsocketEnabledByDefault
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.auth.PersistentWebSocketStatus
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.CurrentSessionUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.PersistPersistentWebSocketConnectionStatusUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import io.mockk.mockkStatic
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class NetworkSettingsViewModelTest {

    private lateinit var viewModel: NetworkSettingsViewModel
    private lateinit var context: Context
    private lateinit var persistPersistentWebSocketConnectionStatus: PersistPersistentWebSocketConnectionStatusUseCase
    private lateinit var observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase
    private lateinit var currentSession: CurrentSessionUseCase
    private lateinit var managedConfigurationsManager: ManagedConfigurationsManager

    @BeforeEach
    fun setup() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        mockkStatic(::isWebsocketEnabledByDefault)
        
        context = mockk(relaxed = true)
        persistPersistentWebSocketConnectionStatus = mockk()
        observePersistentWebSocketConnectionStatus = mockk()
        currentSession = mockk()
        managedConfigurationsManager = mockk()
    }

    @AfterEach
    fun tearDown() {
        io.mockk.unmockkStatic(::isWebsocketEnabledByDefault)
    }

    @Test
    fun `given websocket is enabled by default, when ViewModel initializes, then state reflects it`() = runTest {
        // given
        every { isWebsocketEnabledByDefault(context) } returns true
        coEvery { currentSession() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(userId = QualifiedID("user", "domain"))
        )
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(emptyList())
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(false).stateIn(this)

        // when
        viewModel = createViewModel()

        // then
        assertEquals(true, viewModel.networkSettingsState.isWebSocketEnforcedByDefault)
    }

    @Test
    fun `given websocket is not enabled by default, when ViewModel initializes, then state reflects it`() = runTest {
        // given
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(userId = QualifiedID("user", "domain"))
        )
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(emptyList())
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(false).stateIn(this)

        // when
        viewModel = createViewModel()

        // then
        assertEquals(false, viewModel.networkSettingsState.isWebSocketEnforcedByDefault)
    }

    @Test
    fun `given websocket is enabled, when ViewModel observes status, then state reflects it`() = runTest {
        // given
        val userId = QualifiedID("user", "domain")
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Success(AccountInfo.Valid(userId = userId))
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(listOf(PersistentWebSocketStatus(userId, true)))
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(false).stateIn(this)

        // when
        viewModel = createViewModel()

        // then
        assertEquals(true, viewModel.networkSettingsState.isPersistentWebSocketConnectionEnabled)
    }

    @Test
    fun `given websocket is disabled, when ViewModel observes status, then state reflects it`() = runTest {
        // given
        val userId = QualifiedID("user", "domain")
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Success(AccountInfo.Valid(userId = userId))
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(listOf(PersistentWebSocketStatus(userId, false)))
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(false).stateIn(this)

        // when
        viewModel = createViewModel()

        // then
        assertEquals(false, viewModel.networkSettingsState.isPersistentWebSocketConnectionEnabled)
    }

    @Test
    fun `given MDM enforces websocket, when ViewModel observes MDM state, then state reflects it`() = runTest {
        // given
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(userId = QualifiedID("user", "domain"))
        )
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(emptyList())
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(true).stateIn(this)

        // when
        viewModel = createViewModel()

        // then
        assertEquals(true, viewModel.networkSettingsState.isEnforcedByMDM)
        assertEquals(true, viewModel.networkSettingsState.isPersistentWebSocketConnectionEnabled)
    }

    @Test
    fun `given MDM does not enforce websocket, when ViewModel observes MDM state, then state reflects it`() = runTest {
        // given
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(userId = QualifiedID("user", "domain"))
        )
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(emptyList())
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(false).stateIn(this)

        // when
        viewModel = createViewModel()

        // then
        assertEquals(false, viewModel.networkSettingsState.isEnforcedByMDM)
    }

    @Test
    fun `given websocket is not enforced by MDM, when setting websocket state to enabled, then persist is called`() = runTest {
        // given
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(userId = QualifiedID("user", "domain"))
        )
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(emptyList())
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(false).stateIn(this)
        coEvery { persistPersistentWebSocketConnectionStatus(true) } returns Unit

        viewModel = createViewModel()

        // when
        viewModel.setWebSocketState(true)

        // then
        coEvery { persistPersistentWebSocketConnectionStatus(true) }
    }

    @Test
    fun `given websocket is enforced by MDM, when attempting to set websocket state, then persist is not called`() = runTest {
        // given
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Success(
            AccountInfo.Valid(userId = QualifiedID("user", "domain"))
        )
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(emptyList())
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(true).stateIn(this)

        viewModel = createViewModel()

        // when
        viewModel.setWebSocketState(false)

        // then - persist should not be called
        coEvery { persistPersistentWebSocketConnectionStatus(any()) } returns Unit
    }

    @Test
    fun `given no current session, when ViewModel initializes, then state has default values`() = runTest {
        // given
        every { isWebsocketEnabledByDefault(context) } returns false
        coEvery { currentSession() } returns CurrentSessionResult.Failure.SessionNotFound
        coEvery { observePersistentWebSocketConnectionStatus() } returns
            ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                flowOf(emptyList())
            )
        every { managedConfigurationsManager.persistentWebSocketEnforcedByMDM } returns flowOf(false).stateIn(this)

        // when
        viewModel = createViewModel()

        // then
        assertEquals(false, viewModel.networkSettingsState.isPersistentWebSocketConnectionEnabled)
    }

    private fun createViewModel() = NetworkSettingsViewModel(
        persistPersistentWebSocketConnectionStatus = persistPersistentWebSocketConnectionStatus,
        observePersistentWebSocketConnectionStatus = observePersistentWebSocketConnectionStatus,
        currentSession = currentSession,
        managedConfigurationsManager = managedConfigurationsManager,
        context = context
    )
}
