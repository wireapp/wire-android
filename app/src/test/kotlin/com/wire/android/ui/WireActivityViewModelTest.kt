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

package com.wire.android.ui

import android.content.Intent
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.di.ObserveSyncStateUseCaseProvider
import com.wire.android.feature.AccountSwitchUseCase
import com.wire.android.framework.TestUser
import com.wire.android.migration.MigrationManager
import com.wire.android.navigation.BackStackMode
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.NotificationChannelsManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.services.ServicesManager
import com.wire.android.ui.joinConversation.JoinConversationViaCodeState
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.QualifiedID
import com.wire.kalium.logic.data.team.Team
import com.wire.kalium.logic.data.user.SelfUser
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.appVersioning.ObserveIfAppUpdateRequiredUseCase
import com.wire.kalium.logic.feature.auth.AccountInfo
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase
import com.wire.kalium.logic.feature.conversation.JoinConversationViaCodeUseCase
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.feature.session.GetSessionsUseCase
import com.wire.kalium.logic.feature.user.ObserveValidAccountsUseCase
import com.wire.kalium.logic.feature.user.webSocketStatus.ObservePersistentWebSocketConnectionStatusUseCase
import com.wire.kalium.logic.sync.ObserveSyncStateUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.internal.assertEquals
import org.amshove.kluent.`should be equal to`
import org.amshove.kluent.`should not be equal to`
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class WireActivityViewModelTest {

    @Test
    fun `given Intent is null, when currentSession is present, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .arrange()

        viewModel.handleDeepLink(null)

        val startDestination = viewModel.startNavigationRoute()
        assertEquals(NavigationItem.Home.getRouteWithArgs(), startDestination)
    }

    @Test
    fun `given Intent is null, when currentSession is absent, then startNavigation is Welcome`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .arrange()

        viewModel.handleDeepLink(null)

        val startDestination = viewModel.startNavigationRoute()
        assertEquals(NavigationItem.Welcome.getRouteWithArgs(), startDestination)
    }

    @Test
    fun `given Intent with SSOLogin, when currentSession is present, then startNavigation is Home and navArguments contains SSOLogin`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.SSOLogin.Success("cookie", "config"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<DeepLinkResult.SSOLogin>().isNotEmpty())
    }

    @Test
    fun `given Intent with ServerConfig, when currentSession is present, then startNavigation is Home and no SSOLogin in navArguments`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<DeepLinkResult.SSOLogin>().isEmpty())
    }

    @Test
    fun `given Intent with ServerConfig, when currentSession is absent, then startNavigation is Welcome and no SSOLogin in navArguments`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Welcome.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<DeepLinkResult.SSOLogin>().isEmpty())
    }

    @Test
    fun `given Intent with IncomingCall, when currentSession is present, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<ConversationId>().isNotEmpty())
    }

    @Test
    fun `given Intent with IncomingCall, when currentSession is absent, then startNavigation is Welcome`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Welcome.getRouteWithArgs(), viewModel.startNavigationRoute())
    }

    @Test
    fun `given IncomingCall Intent, when currentSession is there AND activity was created from history, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent(true))

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
    }

    @Test
    fun `given newIntent with IncomingCall, when currentSession is present, then no recreation and navigate to IncomingCall is called`() {
        val conversationId = ConversationId("val", "dom")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(conversationId))
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(mockedIntent())

        assert(!shouldReCreate)
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(
                NavigationCommand(NavigationItem.IncomingCall.getRouteWithArgs(listOf(conversationId)))
            )
        }
    }

    @Test
    fun `given newIntent with IncomingCall, when currentSession is absent, then should recreate and navigate no any navigation`() {
        val conversationId = ConversationId("val", "dom")
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(conversationId))
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(mockedIntent())

        assert(shouldReCreate)
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given Intent with OpenConversation, when currentSession is present, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenConversation(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<ConversationId>().isNotEmpty())
    }

    @Test
    fun `given Intent with OpenConversation, when currentSession is absent, then startNavigation is Welcome`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenConversation(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Welcome.getRouteWithArgs(), viewModel.startNavigationRoute())
    }

    @Test
    fun `given OpenConversation Intent, when currentSession is there AND activity created from history, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenConversation(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent(true))

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
    }

    @Test
    fun `given OpenConversation newIntent, when currentSession is present, then no recreation and navigate to Conversation is called`() {
        val conversationId = ConversationId("val", "dom")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenConversation(conversationId))
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(mockedIntent())

        assert(!shouldReCreate)
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(
                NavigationCommand(NavigationItem.Conversation.getRouteWithArgs(listOf(conversationId)), BackStackMode.UPDATE_EXISTED)
            )
        }
    }

    @Test
    fun `given newIntent with OpenConversation, when currentSession is absent, then should recreate and navigate no any navigation`() {
        val conversationId = ConversationId("val", "dom")
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenConversation(conversationId))
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(mockedIntent())

        assert(shouldReCreate)
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given Intent with OpenOtherUser, when currentSession is present, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenOtherUserProfile(QualifiedID("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<QualifiedID>().isNotEmpty())
    }

    @Test
    fun `given Intent with OpenOtherUser, when currentSession is absent, then startNavigation is Welcome`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenOtherUserProfile(QualifiedID("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Welcome.getRouteWithArgs(), viewModel.startNavigationRoute())
    }

    @Test
    fun `given OpenOtherUser Intent, when currentSession is there AND activity was created from history, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenConversation(QualifiedID("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent(true))

        assertEquals(NavigationItem.Home.getRouteWithArgs(), viewModel.startNavigationRoute())
    }

    @Test
    fun `given OpenOtherUser newIntent, when currentSession is present, then no recreation and navigate to OtherUser is called`() {
        val userId = QualifiedID("val", "dom")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenOtherUserProfile(userId))
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(mockedIntent())

        assert(!shouldReCreate)
        coVerify(exactly = 1) {
            arrangement.navigationManager.navigate(
                NavigationCommand(NavigationItem.OtherUserProfile.getRouteWithArgs(listOf(userId)), BackStackMode.UPDATE_EXISTED)
            )
        }
    }

    @Test
    fun `given newIntent with OpenOtherUser, when currentSession is absent, then should recreate and navigate no any navigation`() {
        val userId = QualifiedID("val", "dom")
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.OpenOtherUserProfile(userId))
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(mockedIntent())

        assert(shouldReCreate)
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given newIntent with SSOLogin, when currentSession is present, then should recreate and no any navigation`() {
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.SSOLogin.Success("cookie", "config"))
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(mockedIntent())

        assert(shouldReCreate)
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given newIntent with null, when currentSession is present, then should not recreate and no any navigation`() {
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(null)

        assert(!shouldReCreate)
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given newIntent with null, when currentSession is absent, then should recreate and no any navigation`() {
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .arrange()

        val shouldReCreate = viewModel.handleDeepLinkOnNewIntent(null)

        assert(shouldReCreate)
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given appUpdate is required, then should show the appUpdate dialog`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withAppUpdateRequired(true)
            .arrange()

        assertEquals(true, viewModel.globalAppState.updateAppDialog)
    }

    @Test
    fun `given few valid accounts, then notificationChannels creating is called`() {
        val accs = listOf(
            TestUser.SELF_USER,
            TestUser.SELF_USER.copy(id = TestUser.USER_ID.copy(value = "something else"))
        )
        val (arrangement, _) = Arrangement()
            .withSomeCurrentSession()
            .withValidAccounts(accs.map { it to null })
            .arrange()

        coVerify(exactly = 1) { arrangement.notificationChannelsManager.createNotificationChannels(listOf()) }
        coVerify(exactly = 1) { arrangement.notificationChannelsManager.createNotificationChannels(accs) }
    }

    @Test
    fun `given newIntent with Join Conversation Deep link, when user is not a member, then start join converstion flow`() {
        val (code, key, domain) = Triple("code", "key", "domain")
        val (conversationName, conversationId, isSelfMember) = Triple("conversation_name", ConversationId("id", "domain"), false)
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.JoinConversation(code, key, domain))
            .withCheckConversationCode(
                code,
                key,
                domain,
                CheckConversationInviteCodeUseCase.Result.Success(conversationName, conversationId, isSelfMember)
            )
            .arrange()

        assert(viewModel.handleDeepLinkOnNewIntent(mockedIntent()))
        viewModel.globalAppState.conversationJoinedDialog `should be equal to` JoinConversationViaCodeState.Show(
            conversationName,
            code,
            key,
            domain
        )
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given newIntent with Join Conversation Deep link, when user is a member, then navigate to the conversation`() {
        val (code, key, domain) = Triple("code", "key", "domain")
        val (conversationName, conversationId, isSelfMember) = Triple("conversation_name", ConversationId("id", "domain"), true)
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.JoinConversation(code, key, domain))
            .withCheckConversationCode(
                code,
                key,
                domain,
                CheckConversationInviteCodeUseCase.Result.Success(conversationName, conversationId, isSelfMember)
            )
            .arrange()

        assert(viewModel.handleDeepLinkOnNewIntent(mockedIntent()))
        viewModel.globalAppState.conversationJoinedDialog `should be equal to` null
        coVerify(exactly = 1) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given valid code, when joining conversion success, then navigate to the conversation`() {
        val (code, key, domain) = Triple("code", "key", "domain")
        val conversationId = ConversationId("id", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.JoinConversation(code, key, domain))
            .withJoinConversationCode(
                code,
                key,
                domain,
                JoinConversationViaCodeUseCase.Result.Success.Changed(conversationId)
            )
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        viewModel.globalAppState.conversationJoinedDialog `should be equal to` null
        coVerify(exactly = 1) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given valid code, when joining conversion and user us already a member, then navigate to the conversation`() {
        val (code, key, domain) = Triple("code", "key", "domain")
        val conversationId = ConversationId("id", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.JoinConversation(code, key, domain))
            .withJoinConversationCode(
                code,
                key,
                domain,
                JoinConversationViaCodeUseCase.Result.Success.Unchanged(conversationId)
            )
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        viewModel.globalAppState.conversationJoinedDialog `should be equal to` null
        coVerify(exactly = 1) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given invalid code, when try to join conversation, then get error and don't navigate`() {
        val (code, key, domain) = Triple("code", "key", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.JoinConversation(code, key, domain))
            .withJoinConversationCodeError(
                code,
                key,
                domain,
                JoinConversationViaCodeUseCase.Result.Failure(CoreFailure.Unknown(RuntimeException("Error")))
            )
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        viewModel.globalAppState.conversationJoinedDialog `should be equal to` null
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
    }

    @Test
    fun `given No session, when try to join conversation, then get error and don't navigate`() {
        val (code, key, domain) = Triple("code", "key", "domain")
        val conversationId = ConversationId("id", "domain")
        val (arrangement, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.JoinConversation(code, key, domain))
            .withJoinConversationCode(
                code,
                key,
                domain,
                JoinConversationViaCodeUseCase.Result.Success.Changed(conversationId)
            )
            .arrange()

        viewModel.joinConversationViaCode(code, key, domain)
        viewModel.globalAppState.conversationJoinedDialog `should be equal to` null
        coVerify(exactly = 0) { arrangement.navigationManager.navigate(any()) }
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
            coEvery { notificationManager.observeNotificationsAndCallsWhileRunning(any(), any(), any()) } returns Unit
            coEvery { navigationManager.navigate(any()) } returns Unit
            coEvery { observePersistentWebSocketConnectionStatus() } returns
                    ObservePersistentWebSocketConnectionStatusUseCase.Result.Success(
                        flowOf(listOf())
                    )
            coEvery { getSessionsUseCase.invoke() }
            coEvery { migrationManager.shouldMigrate() } returns false
            every { observeSyncStateUseCaseProviderFactory.create(any()).observeSyncState } returns observeSyncStateUseCase
            every { observeSyncStateUseCase() } returns emptyFlow()
            coEvery { observeIfAppUpdateRequired(any()) } returns flowOf(false)
            every { notificationChannelsManager.createNotificationChannels(any()) } returns Unit
            coEvery { observeValidAccounts() } returns flowOf(listOf())
        }

        @MockK
        lateinit var currentSessionFlow: CurrentSessionFlowUseCase

        @MockK
        lateinit var getServerConfigUseCase: GetServerConfigUseCase

        @MockK
        lateinit var deepLinkProcessor: DeepLinkProcessor

        @MockK
        lateinit var notificationManager: WireNotificationManager

        @MockK
        lateinit var navigationManager: NavigationManager

        @MockK
        lateinit var observePersistentWebSocketConnectionStatus: ObservePersistentWebSocketConnectionStatusUseCase

        @MockK
        lateinit var getSessionsUseCase: GetSessionsUseCase

        @MockK
        private lateinit var authServerConfigProvider: AuthServerConfigProvider

        @MockK
        private lateinit var switchAccount: AccountSwitchUseCase

        @MockK
        private lateinit var migrationManager: MigrationManager

        @MockK
        private lateinit var observeSyncStateUseCase: ObserveSyncStateUseCase

        @MockK
        private lateinit var observeSyncStateUseCaseProviderFactory: ObserveSyncStateUseCaseProvider.Factory

        @MockK
        lateinit var servicesManager: ServicesManager

        @MockK
        lateinit var observeIfAppUpdateRequired: ObserveIfAppUpdateRequiredUseCase

        @MockK
        lateinit var observeValidAccounts: ObserveValidAccountsUseCase

        @MockK
        lateinit var notificationChannelsManager: NotificationChannelsManager

        @MockK
        lateinit var coreLogic: CoreLogic

        private val viewModel by lazy {
            WireActivityViewModel(
                dispatchers = TestDispatcherProvider(),
                currentSessionFlow = currentSessionFlow,
                getServerConfigUseCase = getServerConfigUseCase,
                deepLinkProcessor = deepLinkProcessor,
                notificationManager = notificationManager,
                navigationManager = navigationManager,
                authServerConfigProvider = authServerConfigProvider,
                observePersistentWebSocketConnectionStatus = observePersistentWebSocketConnectionStatus,
                getSessions = getSessionsUseCase,
                accountSwitch = switchAccount,
                migrationManager = migrationManager,
                observeSyncStateUseCaseProviderFactory = observeSyncStateUseCaseProviderFactory,
                servicesManager = servicesManager,
                observeIfAppUpdateRequired = observeIfAppUpdateRequired,
                observeValidAccounts = observeValidAccounts,
                notificationChannelsManager = notificationChannelsManager,
                coreLogic = coreLogic
            )
        }

        fun withSomeCurrentSession(): Arrangement = apply {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Success(TEST_ACCOUNT_INFO))
            coEvery { coreLogic.getGlobalScope().session.currentSession() } returns CurrentSessionResult.Success(TEST_ACCOUNT_INFO)
        }

        fun withNoCurrentSession(): Arrangement {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Failure.SessionNotFound)
            return this
        }

        fun withDeepLinkResult(result: DeepLinkResult): Arrangement {
            coEvery { deepLinkProcessor(any(), any()) } returns result
            return this
        }

        fun withAppUpdateRequired(result: Boolean): Arrangement = apply {
            coEvery { observeIfAppUpdateRequired(any()) } returns flowOf(result)
        }

        fun withValidAccounts(list: List<Pair<SelfUser, Team?>>): Arrangement = apply {
            coEvery { observeValidAccounts() } returns flowOf(list)
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

        fun withJoinConversationCode(
            code: String,
            key: String,
            domain: String,
            result: JoinConversationViaCodeUseCase.Result
        ): Arrangement = apply {
            coEvery { coreLogic.getSessionScope(TEST_ACCOUNT_INFO.userId).conversations.joinConversationViaCode(code, key, domain) } returns
                    result
        }

        fun withJoinConversationCodeError(
            code: String,
            key: String,
            domain: String,
            result: JoinConversationViaCodeUseCase.Result.Failure
        ): Arrangement = apply {
            coEvery { coreLogic.getSessionScope(TEST_ACCOUNT_INFO.userId).conversations.joinConversationViaCode(code, key, domain) } returns
                    result
        }

        fun arrange() = this to viewModel
    }

    companion object {
        val TEST_ACCOUNT_INFO = AccountInfo.Valid(UserId("user_id", "domain.de"))

        private fun mockedIntent(isFromHistory: Boolean = false): Intent {
            return mockk<Intent>().also {
                every { it.data } returns mockk()
                every { it.flags } returns if (isFromHistory) Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY else 0
            }
        }
    }
}
