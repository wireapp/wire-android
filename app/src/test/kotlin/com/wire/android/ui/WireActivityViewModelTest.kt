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

@file:Suppress("MaximumLineLength")

package com.wire.android.ui

import android.content.Intent
import androidx.work.WorkManager
import androidx.work.impl.OperationImpl
import app.cash.turbine.test
import com.wire.android.BuildConfig
import com.wire.android.assertions.shouldBeEqualTo
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.datastore.GlobalDataStore
import com.wire.android.di.IsProfileQRCodeEnabledUseCaseProvider
import com.wire.android.di.ObserveIfE2EIRequiredDuringLoginUseCaseProvider
import com.wire.android.di.ObserveScreenshotCensoringConfigUseCaseProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.framework.TestClient
import com.wire.android.framework.TestUser
import com.wire.android.services.ServicesManager
import com.wire.android.ui.common.dialogs.CustomServerDetailsDialogState
import com.wire.android.ui.common.dialogs.CustomServerNoNetworkDialogState
import com.wire.android.ui.common.topappbar.CommonTopAppBarViewModelTest
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.ui.theme.ThemeOption
import com.wire.android.util.CurrentScreen
import com.wire.android.util.CurrentScreenManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.newServerConfig
import com.wire.kalium.common.error.NetworkFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.auth.AccountInfo
import com.wire.kalium.logic.data.auth.PersistentWebSocketStatus
import com.wire.kalium.logic.data.call.Call
import com.wire.kalium.logic.data.call.CallStatus
import com.wire.kalium.logic.data.call.RecentlyEndedCallMetadata
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.logout.LogoutReason
import com.wire.kalium.logic.data.sync.SyncState
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.call.usecase.ObserveEstablishedCallsUseCase
import com.wire.kalium.logic.feature.client.ClearNewClientsForUserUseCase
import com.wire.kalium.logic.feature.client.IsProfileQRCodeEnabledUseCase
import com.wire.kalium.logic.feature.client.NewClientResult
import com.wire.kalium.logic.feature.client.ObserveNewClientsUseCase
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistResult
import com.wire.kalium.logic.feature.session.DoesValidSessionExistUseCase
import com.wire.kalium.logic.feature.session.GetAllSessionsResult
import com.wire.kalium.logic.feature.session.ObserveSessionsUseCase
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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertInstanceOf
import org.junit.jupiter.api.Assertions.assertTrue
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

        viewModel.handleDeepLink(null)

        assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState())
    }

    @Test
    fun `given Intent is null, when currentSession is absent, then initialAppState is NOT_LOGGED_IN`() = runTest {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .arrange()

        viewModel.handleDeepLink(null)

        assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState())
    }

    @Test
    fun `given Intent with SSOLogin, when currentSession is present, then return SSOLogin result`() = runTest {
        val result = DeepLinkResult.SSOLogin.Success("cookie", "config")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(result)
            .withNoOngoingCall()
            .arrange()

        viewModel.actions.test {
            viewModel.handleDeepLink(mockedIntent())
            coVerify(exactly = 1) { arrangement.deepLinkProcessor.invoke(any(), any()) }
            assertEquals(OnSSOLogin(result), expectMostRecentItem())
        }
    }

    @Test
    fun `given intent with correct ServerConfig json, when no network is present, then initialAppState is LOGGED_IN and no network dialog is shown`() =
        runTest {
            val result = DeepLinkResult.CustomServerConfig("url")
            val (_, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .withNoNetworkConnectionWhenGettingServerConfig()
                .withNoOngoingCall()
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())

                assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState())
                assertInstanceOf(CustomServerNoNetworkDialogState::class.java, viewModel.globalAppState.customBackendDialog)
                expectNoEvents()
            }
        }

    @Test
    fun `given Intent with malformed ServerConfig json, when currentSessions is absent, then initialAppState is NOT_LOGGED_IN and no network dialog is shown`() =
        runTest {
            val result = DeepLinkResult.CustomServerConfig("url")
            val (_, viewModel) = Arrangement()
                .withNoCurrentSession()
                .withDeepLinkResult(result)
                .withNoNetworkConnectionWhenGettingServerConfig()
                .withNoOngoingCall()
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())
                assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState())
                assertInstanceOf(CustomServerNoNetworkDialogState::class.java, viewModel.globalAppState.customBackendDialog)
                expectNoEvents()
            }
        }

    @Test
    fun `given Intent with ServerConfig, when currentSession is present, then initialAppState is LOGGED_IN and customBackEnd dialog is shown`() =
        runTest {
            val result = DeepLinkResult.CustomServerConfig("url")
            val (_, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .withNoOngoingCall()
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())

                assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState())
                assertInstanceOf(CustomServerDetailsDialogState::class.java, viewModel.globalAppState.customBackendDialog)
                assertEquals(
                    newServerConfig(1).links,
                    (viewModel.globalAppState.customBackendDialog as CustomServerDetailsDialogState).serverLinks
                )
                expectNoEvents()
            }
        }

    @Test
    fun `given Intent with ServerConfig, when currentSession is absent, then initialAppState is NOT_LOGGED_IN and customBackEnd dialog is shown`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withNoCurrentSession()
                .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
                .withNoOngoingCall()
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())
                assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState())
                assertInstanceOf(CustomServerDetailsDialogState::class.java, viewModel.globalAppState.customBackendDialog)
                assertEquals(
                    newServerConfig(1).links,
                    (viewModel.globalAppState.customBackendDialog as CustomServerDetailsDialogState).serverLinks
                )
                expectNoEvents()
            }
        }

    @Test
    fun `given Intent with ServerConfig during an ongoing call, when handling deep links, then error message shown `() =
        runTest {
            val result = DeepLinkResult.SwitchAccountFailure.OngoingCall
            val (_, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .withOngoingCall()
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())
                assertTrue(expectMostRecentItem() is ShowToast)
            }
        }

    @Test
    fun `given Intent with SSOLogin, when currentSession is present, then initialAppState is LOGGED_IN and result SSOLogin`() = runTest {
        val ssoLogin = DeepLinkResult.SSOLogin.Success("cookie", "serverConfig")
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(ssoLogin)
            .withNoOngoingCall()
            .arrange()

        viewModel.actions.test {
            viewModel.handleDeepLink(mockedIntent())
            assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState())
            assertEquals(OnSSOLogin(ssoLogin), expectMostRecentItem())
        }
    }

    @Test
    fun `given Intent with SSOLogin, when currentSession is absent, then initialAppState is NOT_LOGGED_IN and result SSOLogin`() = runTest {
        val ssoLogin = DeepLinkResult.SSOLogin.Success("cookie", "serverConfig")
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(ssoLogin)
            .withNoOngoingCall()
            .arrange()

        viewModel.actions.test {

            viewModel.handleDeepLink(mockedIntent())
            assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState())

            assertEquals(OnSSOLogin(ssoLogin), expectMostRecentItem())
        }
    }

    @Test
    fun `given Intent with MigrationLogin, when currentSession is present, then initialAppState is LOGGED_IN and result MigrationLogin`() =
        runTest {
            val result = DeepLinkResult.MigrationLogin("handle")
            val (_, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())

                assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState())
                assertEquals(OnMigrationLogin(result), expectMostRecentItem())
            }
        }

    @Test
    fun `given Intent with MigrationLogin, when currentSession is absent, then initialAppState is NOT_LOGGED_IN and result MigrationLogin`() =
        runTest {
            val result = DeepLinkResult.MigrationLogin("handle")
            val (_, viewModel) = Arrangement()
                .withNoCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())

                assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState())
                assertEquals(OnMigrationLogin(result), expectMostRecentItem())
            }
        }

    @Test
    fun `given Intent with OpenConversation, when currentSession is present, then initialAppState is LOGGED_IN and result OpenConversation`() =
        runTest {
            val result = DeepLinkResult.OpenConversation(ConversationId("val", "dom"))
            val (_, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())

                assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState())
                assertEquals(OpenConversation(result), expectMostRecentItem())
            }
        }

    @Test
    fun `given Intent with OpenOtherUser, when currentSession is present, then then initialAppState is LOGGED_IN and result OpenOtherUserProfile`() =
        runTest {
            val userId = QualifiedID("val", "dom")
            val result = DeepLinkResult.OpenOtherUserProfile(userId)
            val (_, viewModel) = Arrangement()
                .withSomeCurrentSession()
                .withDeepLinkResult(result)
                .withProfileQRCodeEnabled()
                .arrange()

            viewModel.actions.test {
                viewModel.handleDeepLink(mockedIntent())
                assertEquals(InitialAppState.LOGGED_IN, viewModel.initialAppState())
                assertEquals(OnOpenUserProfile(result), expectMostRecentItem())
            }
        }

    @Test
    fun `given no current session, when deep link is opened, then Authorization Needed result returned`() = runTest {
        val result = DeepLinkResult.AuthorizationNeeded
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(result)
            .arrange()

        viewModel.actions.test {
            viewModel.handleDeepLink(mockedIntent())
            assertEquals(InitialAppState.NOT_LOGGED_IN, viewModel.initialAppState())
            assertEquals(OnAuthorizationNeeded, expectMostRecentItem())
        }
    }

    @Test
    fun `given Intent is null, when currentSession is present, then unknown deeplink result returned`() = runTest {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .arrange()

        viewModel.actions.test {
            viewModel.handleDeepLink(null)
            assertEquals(OnUnknownDeepLink, expectMostRecentItem())
        }
    }

    @Test
    fun `given app started, then should  clean any unfinished analytics state`() = runTest {
        val (arrangement, _) = Arrangement()
            .withSomeCurrentSession()
            .withAppUpdateRequired(false)
            .arrange()

        coEvery { arrangement.globalDataStore.setAnonymousRegistrationEnabled(eq(false)) }
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
        val (_, viewModel) = Arrangement()
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

        viewModel.actions.test {
            viewModel.handleDeepLink(mockedIntent())
            viewModel.globalAppState.conversationJoinedDialog shouldBeEqualTo JoinConversationViaCodeState.Show(
                conversationName,
                code,
                key,
                domain,
                isPasswordRequired
            )
            expectNoEvents()
        }
    }

    @Test
    fun `given newIntent with Join Conversation Deep link, when user is a member, then result JoinConversation deeplink`() = runTest {
        val (code, key, domain) = Triple("code", "key", "domain")
        val isPasswordRequired = false
        val (conversationName, conversationId, isSelfMember) = Triple("conversation_name", ConversationId("id", "domain"), true)
        val result = DeepLinkResult.JoinConversation(code, key, domain)
        val (_, viewModel) = Arrangement()
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

        viewModel.actions.test {
            viewModel.handleDeepLink(mockedIntent())
            viewModel.globalAppState.conversationJoinedDialog shouldBeEqualTo null
            assertEquals(OpenConversation(DeepLinkResult.OpenConversation(conversationId, false)), expectMostRecentItem())
            expectNoEvents()
        }
    }

    @Test
    fun `given Intent with Unknown deep link, when handling deep links, then onUnknown is called `() = runTest {
        val result = DeepLinkResult.Unknown
        val (_, viewModel) = Arrangement()
            .withDeepLinkResult(result)
            .arrange()

        viewModel.actions.test {
            viewModel.handleDeepLink(mockedIntent())
            assertEquals(OnUnknownDeepLink, expectMostRecentItem())
        }
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
            .withCurrentScreen(MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther()))
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
            .withCurrentScreen(MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther()))
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
        val currentScreenFlow = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther())
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
        val currentScreenFlow = MutableStateFlow<CurrentScreen>(CurrentScreen.SomeOther())
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withNewClient(NewClientResult.InCurrentAccount(listOf(TestClient.CLIENT), USER_ID))
            .withCurrentScreen(currentScreenFlow)
            .arrange()

        currentScreenFlow.value = CurrentScreen.ImportMedia

        assertEquals(null, viewModel.globalAppState.newClientDialog)
    }

    @Test
    fun `given session exists, when dismissNewClientsDialog is called, then cleared NewClients for user`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .arrange()

        viewModel.dismissNewClientsDialog(USER_ID)

        coVerify(exactly = 1) { arrangement.clearNewClientsForUser(USER_ID) }
    }

    @Test
    fun `given session does not exist, when dismissNewClientsDialog is called, then do nothing`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .arrange()

        viewModel.dismissNewClientsDialog(USER_ID)

        coVerify(exactly = 0) { arrangement.clearNewClientsForUser(USER_ID) }
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
    fun `given session changes, when observing screenshot censoring, then update screenshot censoring state`() = runTest {
        val firstSession = AccountInfo.Valid(UserId("user1", "domain1"))
        val secondSession = AccountInfo.Valid(UserId("user2", "domain2"))
        val firstSessionScreenshotCensoringConfig = ObserveScreenshotCensoringConfigResult.Disabled
        val secondSessionScreenshotCensoringConfig = ObserveScreenshotCensoringConfigResult.Enabled.ChosenByUser
        val currentSessionFlow = MutableStateFlow(firstSession)
        val (_, viewModel) = Arrangement()
            .withCurrentSessionFlow(currentSessionFlow.map { CurrentSessionResult.Success(it) })
            .withScreenshotCensoringConfigForUser(firstSession.userId, firstSessionScreenshotCensoringConfig)
            .withScreenshotCensoringConfigForUser(secondSession.userId, secondSessionScreenshotCensoringConfig)
            .arrange()
        advanceUntilIdle()
        assertEquals(false, viewModel.globalAppState.screenshotCensoringEnabled)

        currentSessionFlow.emit(secondSession)
        advanceUntilIdle()
        assertEquals(true, viewModel.globalAppState.screenshotCensoringEnabled)
    }

    @Test
    fun `given session changes, when observing sync state, then update sync state`() = runTest {
        val firstSession = AccountInfo.Valid(UserId("user1", "domain1"))
        val secondSession = AccountInfo.Valid(UserId("user2", "domain2"))
        val firstSessionSyncState = SyncState.Live
        val secondSessionSyncState = SyncState.SlowSync
        val currentSessionFlow = MutableStateFlow(firstSession)
        val (_, viewModel) = Arrangement()
            .withCurrentSessionFlow(currentSessionFlow.map { CurrentSessionResult.Success(it) })
            .withSyncStateForUser(firstSession.userId, firstSessionSyncState)
            .withSyncStateForUser(secondSession.userId, secondSessionSyncState)
            .arrange()
        advanceUntilIdle()
        viewModel.observeSyncFlowState.test {
            assertEquals(firstSessionSyncState, awaitItem())

            currentSessionFlow.emit(secondSession)
            advanceUntilIdle()
            assertEquals(secondSessionSyncState, awaitItem())

            expectNoEvents()
        }
    }

    @Test
    fun `given app theme change, when observing it, then update state with theme option`() = runTest {
        val (_, viewModel) = Arrangement()
            .withThemeOption(ThemeOption.DARK)
            .arrange()
        advanceUntilIdle()
        assertEquals(ThemeOption.DARK, viewModel.globalAppState.themeOption)
    }

    @Test
    fun `given user, when current client was removed, then use should see logged out dialog`() =
        runTest {
            val (_, viewModel) = Arrangement()
                .withInvalidCurrentSession(logoutReason = LogoutReason.REMOVED_CLIENT)
                .arrange()

            advanceUntilIdle()

            viewModel.globalAppState.blockUserUI shouldBeEqualTo CurrentSessionErrorState.RemovedClient
        }

    @Test
    fun `given no valid session, when checking number of sessions, then return true`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withObserveSessionsFlow(flowOf(GetAllSessionsResult.Failure.NoSessionFound))
            .arrange()
        viewModel.initValidSessionsFlowIfNeeded()
        advanceUntilIdle()
        // when
        val result = viewModel.checkNumberOfSessions()
        // then
        result shouldBeEqualTo true
        viewModel.globalAppState.maxAccountDialog shouldBeEqualTo false
    }

    @Test
    fun `given valid sessions lower than max, when checking number of sessions, then return true`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withObserveSessionsFlow(flowOf(GetAllSessionsResult.Success(mockedTestAccounts(BuildConfig.MAX_ACCOUNTS - 1))))
            .arrange()
        viewModel.initValidSessionsFlowIfNeeded()
        advanceUntilIdle()
        // when
        val result = viewModel.checkNumberOfSessions()
        // then
        result shouldBeEqualTo true
        viewModel.globalAppState.maxAccountDialog shouldBeEqualTo false
    }

    @Test
    fun `given valid sessions equal to max, when checking number of sessions, then return false and show max account dialog`() = runTest {
        // given
        val (_, viewModel) = Arrangement()
            .withObserveSessionsFlow(flowOf(GetAllSessionsResult.Success(mockedTestAccounts(BuildConfig.MAX_ACCOUNTS))))
            .arrange()
        viewModel.initValidSessionsFlowIfNeeded()
        advanceUntilIdle()
        // when
        val result = viewModel.checkNumberOfSessions()
        // then
        result shouldBeEqualTo false
        viewModel.globalAppState.maxAccountDialog shouldBeEqualTo true
    }

    private class Arrangement {

        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            mockUri()
            coEvery { currentSessionFlow() } returns flowOf()
            coEvery { getServerConfigUseCase(any()) } returns GetServerConfigResult.Success(newServerConfig(1).links)
            coEvery { deepLinkProcessor(any(), any()) } returns DeepLinkResult.Unknown
            coEvery { observeSessionsUseCase.invoke() } returns flowOf(GetAllSessionsResult.Failure.NoSessionFound)
            every { observeSyncStateUseCaseProviderFactory.create(any()).observeSyncState } returns observeSyncStateUseCase
            every { observeSyncStateUseCase() } returns emptyFlow()
            coEvery { observeIfAppUpdateRequired(any()) } returns flowOf(false)
            coEvery { observeNewClients() } returns flowOf()
            every { observeScreenshotCensoringConfigUseCaseProviderFactory.create(any()).observeScreenshotCensoringConfig } returns
                    observeScreenshotCensoringConfigUseCase
            coEvery { observeScreenshotCensoringConfigUseCase() } returns flowOf(ObserveScreenshotCensoringConfigResult.Disabled)
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns MutableStateFlow(CurrentScreen.SomeOther())
            coEvery { globalDataStore.selectedThemeOptionFlow() } returns flowOf(ThemeOption.LIGHT)
            coEvery {
                observeIfE2EIRequiredDuringLoginUseCaseProviderFactory.create(any()).observeIfE2EIIsRequiredDuringLogin()
            } returns
                    flowOf(false)
            every { workManager.cancelAllWorkByTag(any()) } returns OperationImpl()
            every { workManager.enqueueUniquePeriodicWork(any(), any(), any()) } returns OperationImpl()
        }

        @MockK
        lateinit var currentSessionFlow: CurrentSessionFlowUseCase

        @MockK
        lateinit var doesValidSessionExist: DoesValidSessionExistUseCase

        @MockK
        lateinit var getServerConfigUseCase: GetServerConfigUseCase

        @MockK
        lateinit var deepLinkProcessor: DeepLinkProcessor

        @MockK
        lateinit var observeSessionsUseCase: ObserveSessionsUseCase

        @MockK
        private lateinit var switchAccount: AccountSwitchUseCase

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
        private lateinit var observeIfE2EIRequiredDuringLoginUseCaseProviderFactory: ObserveIfE2EIRequiredDuringLoginUseCaseProvider.Factory

        @MockK
        lateinit var globalDataStore: GlobalDataStore

        @MockK
        lateinit var workManager: WorkManager

        @MockK
        lateinit var observeEstablishedCalls: ObserveEstablishedCallsUseCase

        @MockK
        lateinit var isProfileQRCodeEnabledFactory: IsProfileQRCodeEnabledUseCaseProvider.Factory

        private val viewModel by lazy {
            WireActivityViewModel(
                coreLogic = { coreLogic },
                dispatchers = TestDispatcherProvider(),
                currentSessionFlow = { currentSessionFlow },
                doesValidSessionExist = { doesValidSessionExist },
                getServerConfigUseCase = { getServerConfigUseCase },
                deepLinkProcessor = { deepLinkProcessor },
                observeSessions = { observeSessionsUseCase },
                accountSwitch = { switchAccount },
                servicesManager = { servicesManager },
                observeSyncStateUseCaseProviderFactory = observeSyncStateUseCaseProviderFactory,
                observeIfAppUpdateRequired = { observeIfAppUpdateRequired },
                observeNewClients = { observeNewClients },
                clearNewClientsForUser = { clearNewClientsForUser },
                currentScreenManager = { currentScreenManager },
                observeScreenshotCensoringConfigUseCaseProviderFactory = observeScreenshotCensoringConfigUseCaseProviderFactory,
                globalDataStore = { globalDataStore },
                observeIfE2EIRequiredDuringLoginUseCaseProviderFactory = observeIfE2EIRequiredDuringLoginUseCaseProviderFactory,
                workManager = { workManager },
                isProfileQRCodeEnabledFactory = isProfileQRCodeEnabledFactory,
            )
        }

        fun withSomeCurrentSession(): Arrangement = apply {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Success(TEST_ACCOUNT_INFO))
            coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Success(TEST_ACCOUNT_INFO)
            coEvery { doesValidSessionExist(any()) } returns DoesValidSessionExistResult.Success(true)
        }

        fun withInvalidCurrentSession(logoutReason: LogoutReason): Arrangement = apply {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Success(invalidAccountInfo(logoutReason)))
            coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Success(
                invalidAccountInfo(
                    logoutReason
                )
            )
            coEvery { doesValidSessionExist(any()) } returns DoesValidSessionExistResult.Success(true)
        }

        fun withNoCurrentSession(): Arrangement {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Failure.SessionNotFound)
            coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Failure.SessionNotFound
            coEvery { doesValidSessionExist(any()) } returns DoesValidSessionExistResult.Success(false)
            return this
        }

        fun withCurrentSessionFlow(result: Flow<CurrentSessionResult>): Arrangement = apply {
            coEvery { currentSessionFlow() } returns result
        }

        fun withObserveSessionsFlow(result: Flow<GetAllSessionsResult>): Arrangement = apply {
            coEvery { observeSessionsUseCase() } returns result
        }

        fun withDeepLinkResult(result: DeepLinkResult): Arrangement {
            coEvery { deepLinkProcessor(any(), any()) } returns result
            return this
        }

        fun withNoOngoingCall(): Arrangement {
            coEvery { coreLogic.getSessionScope(any()).calls.establishedCall } returns observeEstablishedCalls
            coEvery { observeEstablishedCalls() } returns flowOf(emptyList())
            return this
        }

        fun withOngoingCall(): Arrangement {
            coEvery { coreLogic.getSessionScope(any()).calls.establishedCall } returns observeEstablishedCalls
            coEvery { observeEstablishedCalls() } returns flowOf(listOf(ongoingCall))
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

        fun withNewClient(result: NewClientResult) = apply {
            coEvery { observeNewClients() } returns flowOf(result)
        }

        fun withNewClient(resultFlow: Flow<NewClientResult>) = apply {
            coEvery { observeNewClients() } returns resultFlow
        }

        fun withCurrentScreen(currentScreenFlow: StateFlow<CurrentScreen>) = apply {
            coEvery { currentScreenManager.observeCurrentScreen(any()) } returns currentScreenFlow
            coEvery { coreLogic.getSessionScope(TEST_ACCOUNT_INFO.userId).observeIfE2EIRequiredDuringLogin() } returns flowOf(false)
        }

        fun withNoNetworkConnectionWhenGettingServerConfig() = apply {
            coEvery { getServerConfigUseCase(any()) } returns
                    GetServerConfigResult.Failure.Generic(NetworkFailure.NoNetworkConnection(null))
        }

        suspend fun withScreenshotCensoringConfig(result: ObserveScreenshotCensoringConfigResult) = apply {
            coEvery { observeScreenshotCensoringConfigUseCase() } returns flowOf(result)
        }

        suspend fun withScreenshotCensoringConfigForUser(id: UserId, result: ObserveScreenshotCensoringConfigResult) = apply {
            val useCase = mockk<ObserveScreenshotCensoringConfigUseCase>()
            coEvery {
                observeScreenshotCensoringConfigUseCaseProviderFactory.create(id).observeScreenshotCensoringConfig
            } returns useCase
            coEvery { useCase() } returns flowOf(result)
        }

        fun withSyncStateForUser(id: UserId, result: SyncState) = apply {
            val useCase = mockk<ObserveSyncStateUseCase>()
            coEvery { observeSyncStateUseCaseProviderFactory.create(id).observeSyncState } returns useCase
            coEvery { useCase() } returns flowOf(result)
        }

        suspend fun withThemeOption(themeOption: ThemeOption) = apply {
            coEvery { globalDataStore.selectedThemeOptionFlow() } returns flowOf(themeOption)
        }

        suspend fun withProfileQRCodeEnabled(isEnabled: Boolean = true) = apply {
            val useCase = mockk<IsProfileQRCodeEnabledUseCase>()
            coEvery { isProfileQRCodeEnabledFactory.create(any()).isProfileQRCodeEnabled } returns useCase
            coEvery { useCase() } returns isEnabled
        }

        fun arrange() = this to viewModel
    }

    companion object {
        val USER_ID = UserId("user_id", "domain.de")
        val TEST_ACCOUNT_INFO = AccountInfo.Valid(USER_ID)

        private fun mockedTestAccounts(count: Int) = List(count) { i ->
            TEST_ACCOUNT_INFO.copy(userId = USER_ID.copy("user_$i"))
        }

        private fun mockedIntent(isFromHistory: Boolean = false): Intent {
            return mockk<Intent>().also {
                every { it.data } returns mockk()
                every { it.flags } returns if (isFromHistory) Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY else 0
                every { it.action } returns null
            }
        }

        val ongoingCall = Call(
            CommonTopAppBarViewModelTest.conversationId,
            CallStatus.ESTABLISHED,
            isMuted = true,
            isCameraOn = false,
            isCbrEnabled = false,
            callerId = UserId("caller", "domain"),
            conversationName = "ONE_ON_ONE Name",
            conversationType = Conversation.Type.OneOnOne,
            callerName = "otherUsername",
            callerTeamName = "team1"
        )

        val recentlyEndedCallMetadata = RecentlyEndedCallMetadata(
            callEndReason = 1,
            callDetails = RecentlyEndedCallMetadata.CallDetails(
                isCallScreenShare = false,
                screenShareDurationInSeconds = 20L,
                callScreenShareUniques = 5,
                isOutgoingCall = true,
                callDurationInSeconds = 100L,
                callParticipantsCount = 5,
                conversationServices = 1,
                callAVSwitchToggle = false,
                callVideoEnabled = false
            ),
            conversationDetails = RecentlyEndedCallMetadata.ConversationDetails(
                conversationType = Conversation.Type.OneOnOne,
                conversationSize = 5,
                conversationGuests = 2,
                conversationGuestsPro = 1
            ),
            isTeamMember = true
        )

        fun invalidAccountInfo(logoutReason: LogoutReason): AccountInfo.Invalid = AccountInfo.Invalid(USER_ID, logoutReason)
    }
}
