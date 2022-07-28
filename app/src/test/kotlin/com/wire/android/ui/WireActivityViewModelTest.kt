package com.wire.android.ui

import android.content.Intent
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.NavigationManager
import com.wire.android.navigation.VoyagerNavigationItem
import com.wire.android.navigation.nav
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import org.amshove.kluent.internal.assertEquals
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

        val startDestination = viewModel.startVoyagerNavigationScreen()
        assertEquals(VoyagerNavigationItem.Home, startDestination)
    }

    @Test
    fun `given Intent is null, when currentSession is absent, then startNavigation is Welcome`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .arrange()

        viewModel.handleDeepLink(null)

        val startDestination = viewModel.startVoyagerNavigationScreen()
        assertEquals(VoyagerNavigationItem.Welcome, startDestination)
    }

    @Test
    fun `given Intent with SSOLogin, when currentSession is present, then startNavigation is Login and navArguments contains SSOLogin`() {
        val ssoLogin = DeepLinkResult.SSOLogin.Success("cookie", "config")
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(ssoLogin)
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(VoyagerNavigationItem.Login(ssoLogin), viewModel.startVoyagerNavigationScreen())
    }

    @Test
    fun `given Intent with ServerConfig, when currentSession is present, then startNavigation is Login and no SSOLogin in navArguments`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(VoyagerNavigationItem.Login(), viewModel.startVoyagerNavigationScreen())
        val deepLinkDestination = viewModel.deepLinkDestination
        assert(deepLinkDestination is WireActivityViewModel.DeepLinkDestination.Login && deepLinkDestination.ssoLogin == null)
    }

    @Test
    fun `given Intent with ServerConfig, when currentSession is absent, then startNavigation is Login and no SSOLogin in navArguments`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(VoyagerNavigationItem.Login(), viewModel.startVoyagerNavigationScreen())
        val deepLinkDestination = viewModel.deepLinkDestination
        assert(deepLinkDestination is WireActivityViewModel.DeepLinkDestination.Login && deepLinkDestination.ssoLogin == null)
    }

    @Test
    fun `given Intent with IncomingCall, when currentSession is present, then startNavigation is IncomingCall`() {
        val callConversationId = ConversationId("val", "dom")
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(callConversationId))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(VoyagerNavigationItem.IncomingCall(callConversationId.nav()), viewModel.startVoyagerNavigationScreen())
        val deepLinkDestination = viewModel.deepLinkDestination
        assert(
            deepLinkDestination is WireActivityViewModel.DeepLinkDestination.IncomingCall
                    && deepLinkDestination.conversationId == callConversationId
        )
    }

    @Test
    fun `given Intent with IncomingCall, when currentSession is absent, then startNavigation is IncomingCall`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(VoyagerNavigationItem.Welcome, viewModel.startVoyagerNavigationScreen())
    }

    @Test
    fun `given IncomingCall Intent, when currentSession is there AND activity was created from history, then startNavigation is Home`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent(true))

        assertEquals(VoyagerNavigationItem.Home, viewModel.startVoyagerNavigationScreen())
    }

    private class Arrangement {
        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            mockUri()
            coEvery { currentSessionFlow() } returns flowOf()
            coEvery { getServerConfigUseCase(any()) } returns GetServerConfigResult.Success(newServerConfig(1))
            coEvery { deepLinkProcessor(any()) } returns DeepLinkResult.Unknown
            coEvery { notificationManager.observeNotificationsAndCalls(any(), any(), any()) } returns Unit
            coEvery { navigationManager.navigate(any()) } returns Unit
        }

        @MockK
        lateinit var userSessionScope: UserSessionScope

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
        private lateinit var authServerConfigProvider: AuthServerConfigProvider

        private val viewModel by lazy {
            WireActivityViewModel(
                dispatchers = TestDispatcherProvider(),
                currentSessionFlow = currentSessionFlow,
                getServerConfigUseCase = getServerConfigUseCase,
                deepLinkProcessor = deepLinkProcessor,
                notificationManager = notificationManager,
                navigationManager = navigationManager,
                authServerConfigProvider = authServerConfigProvider
            )
        }

        fun withSomeCurrentSession(): Arrangement {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Success(TEST_AUTH_SESSION))
            return this
        }

        fun withNoCurrentSession(): Arrangement {
            coEvery { currentSessionFlow() } returns flowOf(CurrentSessionResult.Failure.SessionNotFound)
            return this
        }

        fun withDeepLinkResult(result: DeepLinkResult): Arrangement {
            coEvery { deepLinkProcessor(any()) } returns result
            return this
        }

        fun arrange() = this to viewModel

    }


    companion object {
        val TEST_AUTH_SESSION =
            AuthSession(
                AuthSession.Tokens(
                    userId = UserId("user_id", "domain.de"),
                    accessToken = "access_token",
                    refreshToken = "refresh_token",
                    tokenType = "token_type",
                ),
                newServerConfig(1).links
            )

        private fun mockedIntent(isFromHistory: Boolean = false): Intent {
            return mockk<Intent>().also {
                every { it.data } returns mockk()
                every { it.flags } returns if (isFromHistory) Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY else 0
            }
        }
    }
}
