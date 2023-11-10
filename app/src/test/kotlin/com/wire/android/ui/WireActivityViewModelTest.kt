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

@file:Suppress("MaximumLineLength")

package com.wire.android.ui

import android.content.Intent
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.migration.MigrationManager
import com.wire.android.services.ServicesManager
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.auth.PersistentWebSocketStatus
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.NewClientResult
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigResult
import com.wire.kalium.logic.feature.user.screenshotCensoring.ObserveScreenshotCensoringConfigUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.`should be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@Suppress("MaxLineLength")
@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class WireActivityViewModelTest {

    @Test
    fun `given Intent is null, when currentSession is present, then initialAppState is LOGGED_IN`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .arrange()

        viewModel.handleDeepLink(null, {}, {}, {})

        assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState)
    }

    @Test
    fun `given Intent is null, when currentSession is absent, then initialAppState is NOT_LOGGED_IN`() = runTest {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .arrange()

        viewModel.handleDeepLink(null, {}, {}, {})

        assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState)
    }

    @Test
    fun `given Intent with SSOLogin, when currentSession is present, then return SSOLogin result`() = runTest {
        val result = DeepLinkResult.SSOLogin.Success("cookie", "config")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(result)
            .arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)
        coVerify(exactly = 1) { arrangement.deepLinkProcessor.invoke(any()) }
        verify(exactly = 1) { arrangement.onDeepLinkResult(result) }
    }

    @Test
    fun `given Intent with ServerConfig, when currentSession is present, then initialAppState is LOGGED_IN and customBackEnd dialog is shown`() =
        runTest {
            val result = DeepLinkResult.CustomServerConfig("url")
            val (arrangement, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState)
            verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
            assertEquals(newServerConfig(1).links, viewModel.globalAppState.customBackendDialog!!.serverLinks)
        }

    @Test
    fun `given Intent with ServerConfig, when currentSession is absent, then initialAppState is NOT_LOGGED_IN and customBackEnd dialog is shown`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withNoCurrentSession()
                .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState)
            verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
            assertEquals(newServerConfig(1).links, viewModel.globalAppState.customBackendDialog!!.serverLinks)
        }

    @Test
    fun `given Intent with ServerConfig, when currentSession is absent and migration is required, then initialAppState is NOT_MIGRATED`() =
        runTest {
            val (arrangement, viewModel) = Arrangement()
                .withNoCurrentSession()
                .withMigrationRequired()
                .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
                .withCurrentScreen(MutableStateFlow<CurrentScreen>(CurrentScreen.Home))
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.NOT_MIGRATED, viewModel.initialAppState)
            verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
            assertEquals(null, viewModel.globalAppState.customBackendDialog)
        }

    @Test
    fun `given Intent with SSOLogin, when currentSession is present, then initialAppState is LOGGED_IN and result SSOLogin`() = runTest {
        val ssoLogin = DeepLinkResult.SSOLogin.Success("cookie", "serverConfig")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(ssoLogin)
            .arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

        assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState)
        verify(exactly = 1) { arrangement.onDeepLinkResult(ssoLogin) }
    }

    @Test
    fun `given Intent with SSOLogin, when currentSession is absent, then initialAppState is NOT_LOGGED_IN and result SSOLogin`() = runTest {
        val ssoLogin = DeepLinkResult.SSOLogin.Success("cookie", "serverConfig")
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(ssoLogin)
            .arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

        assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState)
        verify(exactly = 1) { arrangement.onDeepLinkResult(ssoLogin) }
    }

    @Test
    fun `given Intent with MigrationLogin, when currentSession is present, then initialAppState is LOGGED_IN and result MigrationLogin`() =
        runTest {
            val result = DeepLinkResult.MigrationLogin("handle")
            val (arrangement, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState)
            verify(exactly = 1) { arrangement.onDeepLinkResult(result) }
        }

    @Test
    fun `given Intent with MigrationLogin, when currentSession is absent, then initialAppState is NOT_LOGGED_IN and result MigrationLogin`() =
        runTest {
            val result = DeepLinkResult.MigrationLogin("handle")
            val (arrangement, viewModel) = Arrangement()
                .withNoCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState)
            verify(exactly = 1) { arrangement.onDeepLinkResult(result) }
        }

    @Test
    fun `given Intent with IncomingCall, when currentSession is present, then initialAppState is LOGGED_IN and result IncomingCall`() =
        runTest {
            val result = DeepLinkResult.IncomingCall(ConversationId("val", "dom"))
            val (arrangement, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState)
            verify(exactly = 1) { arrangement.onDeepLinkResult(result) }
        }

    @Test
    fun `given Intent with IncomingCall, when currentSession is absent, then initialAppState is NOT_LOGGED_IN`() = runTest {
        val result = DeepLinkResult.IncomingCall(ConversationId("val", "dom"))
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(result)
            .arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

        assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState)
        verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
    }

    @Test
    fun `given Intent with OpenConversation, when currentSession is present, then initialAppState is LOGGED_IN and result OpenConversation`() =
        runTest {
            val result = DeepLinkResult.OpenConversation(ConversationId("val", "dom"))
            val (arrangement, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState)
            verify(exactly = 1) { arrangement.onDeepLinkResult(result) }
        }

    @Test
    fun `given Intent with OpenConversation, when currentSession is absent, then initialAppState is NOT_LOGGED_IN`() = runTest {
        val result = DeepLinkResult.OpenConversation(ConversationId("val", "dom"))
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(result)
            .arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

        assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState)
        verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
    }

    @Test
    fun `given Intent with OpenOtherUser, when currentSession is present, then then initialAppState is LOGGED_IN and result OpenOtherUserProfile`() =
        runTest {
            val userId = QualifiedID("val", "dom")
            val result = DeepLinkResult.OpenOtherUserProfile(userId)
            val (arrangement, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

            assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState)
            verify(exactly = 1) { arrangement.onDeepLinkResult(result) }
        }

    @Test
    fun `given Intent with OpenOtherUser, when currentSession is absent, then initialAppState is NOT_LOGGED_IN`() = runTest {
        val result = DeepLinkResult.OpenOtherUserProfile(QualifiedID("val", "dom"))
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(result)
            .arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

        assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState)
        verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
    }

    @Test
    fun `given Intent is null, when currentSession is present, then should not return any deeplink result`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .arrange()

        viewModel.handleDeepLink(null, {}, {}, arrangement.onDeepLinkResult)

        verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
    }

    @Test
    fun `given appUpdate is required, then should show the appUpdate dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withAppUpdateRequired(true)
            .arrange()

        assertEquals(true, viewModel.globalAppState.updateAppDialog)
    }

    @Test
    fun `given newIntent with Join Conversation Deep link, when user is not a member, then start join conversation flow`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val isPasswordRequired = false
        val (conversationName, conversationId, isSelfMember) = Triple("conversation_name", ConversationId("id", "domain"), false)
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.JoinConversation(code, key, domain))
            .withCheckConversationCode(
                code,
                key,
                domain,
                CheckConversationInviteCodeUseCase.Result.Success(
                    conversationName,
                    conversationId,
                    isSelfMember,
                    isPasswordProtected = isPasswordRequired
                )
            )
            .arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, {}, arrangement.onDeepLinkResult)

        viewModel.globalAppState.conversationJoinedDialog `should be equal to` JoinConversationViaCodeState.Show(
            conversationName,
            code,
            key,
            domain,
            isPasswordRequired
        )
        coVerify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
    }

    @Test
    fun `given newIntent with Join Conversation Deep link, when user is a member, then result JoinConversation deeplink`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val isPasswordRequired = false
        val (conversationName, conversationId, isSelfMember) = Triple("conversation_name", ConversationId("id", "domain"), true)
        val result = DeepLinkResult.JoinConversation(code, key, domain)
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(result)
            .withCheckConversationCode(
                code,
                key,
                domain,
                CheckConversationInviteCodeUseCase.Result.Success(
                    conversationName,
                    conversationId,
                    isSelfMember,
                    isPasswordProtected = isPasswordRequired
                )
            ).arrange()

        viewModel.handleDeepLink(mockedIntent(), {}, arrangement.onSuccess, arrangement.onDeepLinkResult)

        viewModel.globalAppState.conversationJoinedDialog `should be equal to` null
        verify(exactly = 0) { arrangement.onDeepLinkResult(any()) }
        verify(exactly = 1) { arrangement.onSuccess(conversationId) }
    }

    @Test
    fun `given valid accounts, all with persistent socket disabled, then stop socket service`() = runTest {
        val statuses = listOf(
            PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
            PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), false)
        )
        val (arrangement, manager) = Arrangement()
            .withPersistentWebSocketConnectionStatuses(statuses)
            .arrange()

        manager.observePersistentConnectionStatus()

        coVerify(exactly = 0) { arrangement.servicesManager.startPersistentWebSocketService() }
        coVerify(exactly = 1) { arrangement.servicesManager.stopPersistentWebSocketService() }
    }

    @Test
    fun `given valid accounts, at least one with persistent socket enabled, and socket service not running, then start service`() =
        runTest {
            val statuses = listOf(
                PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
                PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), true)
            )
            val (arrangement, manager) = Arrangement()
                .withPersistentWebSocketConnectionStatuses(statuses)
                .withIsPersistentWebSocketServiceRunning(false)
                .arrange()

            manager.observePersistentConnectionStatus()

            coVerify(exactly = 1) { arrangement.servicesManager.startPersistentWebSocketService() }
            coVerify(exactly = 0) { arrangement.servicesManager.stopPersistentWebSocketService() }
        }

    @Test
    fun `given valid accounts, at least one with persistent socket enabled, and socket service running, then do not start service again`() =
        runTest {
            val statuses = listOf(
                PersistentWebSocketStatus(TestUser.SELF_USER.id, false),
                PersistentWebSocketStatus(TestUser.USER_ID.copy(value = "something else"), true)
            )
            val (arrangement, manager) = Arrangement()
                .withPersistentWebSocketConnectionStatuses(statuses)
                .withIsPersistentWebSocketServiceRunning(true)
                .arrange()

            manager.observePersistentConnectionStatus()

            coVerify(exactly = 0) { arrangement.servicesManager.startPersistentWebSocketService() }
            coVerify(exactly = 0) { arrangement.servicesManager.stopPersistentWebSocketService() }
        }

    @Test
    fun `given newClient is registered for the current user, then should show the NewClient dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withNewClient(NewClientResult.InCurrentAccount(listOf(TestClient.CLIENT), USER_ID))
            .withCurrentScreen(MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther))
            .arrange()

        assertEquals(
            NewClientsData.CurrentUser(listOf(NewClientInfo.fromClient(TestClient.CLIENT)), USER_ID),
            viewModel.globalAppState.newClientDialog
        )
    }

    @Test
    fun `given newClient is registered for the other user, then should show the NewClient dialog`() = runTest {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withNewClient(NewClientResult.InOtherAccount(listOf(TestClient.CLIENT), USER_ID, "name", "handle"))
            .withCurrentScreen(MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther))
            .arrange()

        assertEquals(
            NewClientsData.OtherUser(
                listOf(NewClientInfo.fromClient(TestClient.CLIENT)),
                USER_ID,
                "name",
                "handle"
            ),
            viewModel.globalAppState.newClientDialog
        )
    }

    @Test
    fun `given newClient is registered when current screen does not allow dialog, then remember NewClient dialog state`() = runTest {
        val currentScreenFlow = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther)
        val newClientFlow = MutableSharedFlow<NewClientResult>()
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withNewClient(newClientFlow)
            .withCurrentScreen(currentScreenFlow)
            .arrange()

        currentScreenFlow.value = CurrentScreen.ImportMedia
        newClientFlow.emit(NewClientResult.InCurrentAccount(listOf(TestClient.CLIENT), USER_ID))

        advanceUntilIdle()

        assertEquals(null, viewModel.globalAppState.newClientDialog)
    }

    @Test
    fun `given newClient is registered when current screen changed to ImportMedea, then remember NewClient dialog state`() = runTest {
        val currentScreenFlow = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther)
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withNewClient(NewClientResult.InCurrentAccount(listOf(TestClient.CLIENT), USER_ID))
            .withCurrentScreen(currentScreenFlow)
            .arrange()

        currentScreenFlow.value = CurrentScreen.ImportMedia

        assertEquals(null, viewModel.globalAppState.newClientDialog)
    }

    @Test
    fun `when dismissNewClientsDialog is called, then cleared NewClients for user`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .arrange()

        viewModel.dismissNewClientsDialog(USER_ID)

        coVerify(exactly = 1) { arrangement.clearNewClientsForUser(USER_ID) }
    }

    @Test
    fun `given session and screenshot censoring disabled, when observing it, then set state to false`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withScreenshotCensoringConfig(ObserveScreenshotCensoringConfigResult.Disabled)
            .arrange()
        advanceUntilIdle()
        assertEquals(false, viewModel.globalAppState.screenshotCensoringEnabled)
    }

    @Test
    fun `given session and screenshot censoring enabled by user, when observing it, then set state to true`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withScreenshotCensoringConfig(ObserveScreenshotCensoringConfigResult.Enabled.ChosenByUser)
            .arrange()
        advanceUntilIdle()
        assertEquals(true, viewModel.globalAppState.screenshotCensoringEnabled)
    }

    @Test
    fun `given session and screenshot censoring enforced by team, when observing it, then set state to true`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withScreenshotCensoringConfig(ObserveScreenshotCensoringConfigResult.Enabled.EnforcedByTeamSelfDeletingSettings)
            .arrange()
        advanceUntilIdle()
        assertEquals(true, viewModel.globalAppState.screenshotCensoringEnabled)
    }

    @Test
    fun `given no session, when observing screenshot censoring, then set state to false`() = runTest {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withScreenshotCensoringConfig(ObserveScreenshotCensoringConfigResult.Enabled.EnforcedByTeamSelfDeletingSettings)
            .arrange()
        advanceUntilIdle()
        assertEquals(false, viewModel.globalAppState.screenshotCensoringEnabled)
    }

    @Test
    fun `given app theme change, when observing it, then update state with theme option`() = runTest {
        val (_, viewModel) = Arrangement()
            .withThemeOption(ThemeOption.DARK)
            .arrange()
        advanceUntilIdle()
        assertEquals(ThemeOption.DARK, viewModel.globalAppState.themeOption)
    }

    private class Arrangement {
        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            mockUri()
            coEvery { currentSessionFlow() } returns flowOf()
            coEvery { getServerConfigUseCase(any()) } returns GetServerConfigResult.Success(newServerConfig(1).links)
            coEvery { deepLinkProcessor(any()) } returns DeepLinkResult.Unknown
            coEvery { getSessionsUseCase.invoke() }
            coEvery { migrationManager.shouldMigrate() } returns false
            every { observeSyncStateUseCaseProviderFactory.create(any()).observeSyncState } returns observeSyncStateUseCase
            every { observeSyncStateUseCase() } returns emptyFlow()
            coEvery { observeIfAppUpdateRequired(any()) } returns flowOf(false)
            coEvery { observeNewClients() } returns flowOf()
            every { observeScreenshotCensoringConfigUseCaseProviderFactory.create(any()).observeScreenshotCensoringConfig } returns
                    observeScreenshotCensoringConfigUseCase
            coEvery { observeScreenshotCensoringConfigUseCase() } returns flowOf(ObserveScreenshotCensoringConfigResult.Disabled)
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(CurrentScreen.SomeOther)
            coEvery { globalDataStore.selectedThemeOptionFlow() } returns flowOf(ThemeOption.LIGHT)
        }

        @MockK
        lateinit var currentSessionFlow: CurrentSessionFlowUseCase

        @MockK
        lateinit var getServerConfigUseCase: GetServerConfigUseCase

        @MockK
        lateinit var deepLinkProcessor: DeepLinkProcessor

        @MockK
        lateinit var getSessionsUseCase: GetSessionsUseCase

        var authServerConfigProvider: AuthServerConfigProvider = AuthServerConfigProvider()

        @MockK
        private lateinit var switchAccount: AccountSwitchUseCase

        @MockK
        private lateinit var migrationManager: MigrationManager

        @MockK
        private lateinit var observeSyncStateUseCase: ObserveSyncStateUseCase

        @MockK
        private lateinit var observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory

        @MockK
        private lateinit var coreLogic: CoreLogic

        @MockK
        lateinit var servicesManager: ServicesManager

        @MockK
        lateinit var observeIfAppUpdateRequired: ObserveIfAppUpdateRequiredUseCase

        @MockK
        lateinit var observeNewClients: ObserveNewClientsUseCase

        @MockK
        lateinit var clearNewClientsForUser: ClearNewClientsForUserUseCase

        @MockK
        lateinit var currentScreenManager: CurrentScreenManager

        @MockK
        private lateinit var observeScreenshotCensoringConfigUseCase: ObserveScreenshotCensoringConfigUseCase

        @MockK
        private lateinit var observeScreenshotCensoringConfigUseCaseProviderFactory: ObserveScreenshotCensoringConfigUseCaseProvider.Factory

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK(relaxed = true)
        lateinit var onDeepLinkResult: (DeepLinkResult) -> Unit

        @MockK(relaxed = true)
        lateinit var onSuccess: (ConversationId) -> Unit

        private val viewModel by lazy {
            WireActivityViewModel(
                coreLogic = coreLogic,
                dispatchers = TestDispatcherProvider(),
                currentSessionFlow = currentSessionFlow,
                getServerConfigUseCase = getServerConfigUseCase,
                deepLinkProcessor = deepLinkProcessor,
                authServerConfigProvider = authServerConfigProvider,
                getSessions = getSessionsUseCase,
                accountSwitch = switchAccount,
                migrationManager = migrationManager,
                servicesManager = servicesManager,
                observeSyncStateUseCaseProviderFactory = observeSyncStateUseCaseProviderFactory,
                observeIfAppUpdateRequired = observeIfAppUpdateRequired,
                observeNewClients = observeNewClients,
                clearNewClientsForUser = clearNewClientsForUser,
                currentScreenManager = currentScreenManager,
                observeScreenshotCensoringConfigUseCaseProviderFactory = observeScreenshotCensoringConfigUseCaseProviderFactory,
                globalDataStore = globalDataStore
            )
        }

        fun withSomeCurrentSession(): Arrangement = apply {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Success(TEST_ACCOUNT_INFO))
            coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Success(TEST_ACCOUNT_INFO)
        }

        fun withNoCurrentSession(): Arrangement {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Failure.SessionNotFound)
            coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Failure.SessionNotFound
            return this
        }

        fun withDeepLinkResult(result: DeepLinkResult): Arrangement {
            coEvery { deepLinkProcessor(any()) } returns result
            return this
        }

        fun withAppUpdateRequired(result: Boolean): Arrangement = apply {
            coEvery { observeIfAppUpdateRequired(any()) } returns flowOf(result)
        }

        fun withCheckConversationCode(
            code: String,
            key: String,
            domain: String,
            result: CheckConversationInviteCodeUseCase.Result
        ): Arrangement = apply {
            coEvery {
                coreLogic.getSessionScope(TEST_ACCOUNT_INFO.userId).conversations.checkIConversationInviteCode(
                    code,
                    key,
                    domain
                )
            } returns result
        }

        fun withPersistentWebSocketConnectionStatuses(list: List<PersistentWebSocketStatus>): Arrangement = apply {
            coEvery { coreLogic.getGlobalScope().observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(flowOf(list))
        }

        fun withIsPersistentWebSocketServiceRunning(isRunning: Boolean): Arrangement = apply {
            every { servicesManager.isPersistentWebSocketServiceRunning() } returns isRunning
        }

        fun withMigrationRequired(): Arrangement = apply {
            coEvery { migrationManager.shouldMigrate() } returns true
        }

        fun withNewClient(result: NewClientResult) = apply {
            coEvery { observeNewClients() } returns flowOf(result)
        }

        fun withNewClient(resultFlow: Flow<NewClientResult>) = apply {
            coEvery { observeNewClients() } returns resultFlow
        }

        fun withCurrentScreen(currentScreenFlow: StateFlow<CurrentScreen>) = apply {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns currentScreenFlow
        }

        suspend fun withScreenshotCensoringConfig(result: ObserveScreenshotCensoringConfigResult) = apply {
            coEvery { observeScreenshotCensoringConfigUseCase() } returns flowOf(result)
        }

        suspend fun withThemeOption(themeOption: ThemeOption) = apply {
            coEvery { globalDataStore.selectedThemeOptionFlow() } returns flowOf(themeOption)
        }

        fun arrange() = this to viewModel
    }

    companion object {
        val USER_ID = UserId("user_id", "domain.de")
        val TEST_ACCOUNT_INFO = AccountInfo.Valid(USER_ID)

        private fun mockedIntent(isFromHistory: Boolean = false): Intent {
            return mockk<Intent>().also {
                every { it.data } returns mockk()
                every { it.flags } returns if (isFromHistory) Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY else 0
                every { it.action } returns null
            }
        }
    }
}
