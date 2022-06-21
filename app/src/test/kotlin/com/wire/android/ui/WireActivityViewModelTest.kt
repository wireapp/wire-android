package com.wire.android.ui

import android.content.Intent
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.di.AuthServerConfigProvider
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.NavigationItem
import com.wire.android.navigation.NavigationManager
import com.wire.android.notification.WireNotificationManager
import com.wire.android.util.deeplink.DeepLinkProcessor
import com.wire.android.util.deeplink.DeepLinkResult
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.user.UserId
import com.wire.kalium.logic.feature.UserSessionScope
import com.wire.kalium.logic.feature.auth.AuthSession
import com.wire.kalium.logic.feature.server.GetServerConfigResult
import com.wire.kalium.logic.feature.server.GetServerConfigUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionFlowUseCase
import com.wire.kalium.logic.feature.session.CurrentSessionResult
import com.wire.kalium.logic.sync.ListenToEventsUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
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
    fun `given Intent with SSOLogin, when currentSession is present, then startNavigation is Login and navArguments contains SSOLogin`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.SSOLogin.Success("cookie", "config"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Login.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<DeepLinkResult.SSOLogin>().isNotEmpty())
    }

    @Test
    fun `given Intent with ServerConfig, when currentSession is present, then startNavigation is Login and no SSOLogin in navArguments`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Login.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<DeepLinkResult.SSOLogin>().isEmpty())
    }

    @Test
    fun `given Intent with ServerConfig, when currentSession is absent, then startNavigation is Login and no SSOLogin in navArguments`() {
        val (_, viewModel) = Arrangement()
            .withNoCurrentSession()
            .withDeepLinkResult(DeepLinkResult.CustomServerConfig("url"))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.Login.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<DeepLinkResult.SSOLogin>().isEmpty())
    }

    @Test
    fun `given Intent with IncomingCall, when currentSession is present, then startNavigation is IncomingCall`() {
        val (_, viewModel) = Arrangement()
            .withSomeCurrentSession()
            .withDeepLinkResult(DeepLinkResult.IncomingCall(ConversationId("val", "dom")))
            .arrange()

        viewModel.handleDeepLink(mockedIntent())

        assertEquals(NavigationItem.IncomingCall.getRouteWithArgs(), viewModel.startNavigationRoute())
        assert(viewModel.navigationArguments().filterIsInstance<ConversationId>().isNotEmpty())
    }

    @Test
    fun `given Intent with IncomingCall, when currentSession is absent, then startNavigation is IncomingCall`() {
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

    private class Arrangement {
        init {
            // Tests setup
            MockKAnnotations.init(this, relaxUnitFun = true)

            // Default empty values
            mockUri()
            coEvery { listenToEventsUseCase() } returns Unit
            coEvery { userSessionScope.listenToEvents } returns listenToEventsUseCase
            coEvery { coreLogic.getSessionScope(any()) } returns userSessionScope
            coEvery { currentSessionFlow() } returns flowOf()
            coEvery { getServerConfigUseCase(any()) } returns GetServerConfigResult.Success(newServerConfig(1))
            coEvery { deepLinkProcessor(any()) } returns DeepLinkResult.Unknown
            coEvery { notificationManager.observeMessageNotifications(any()) } returns Unit
            coEvery { navigationManager.navigate(any()) } returns Unit
        }

        @MockK
        lateinit var listenToEventsUseCase: ListenToEventsUseCase

        @MockK
        lateinit var userSessionScope: UserSessionScope

        @MockK
        lateinit var coreLogic: CoreLogic

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
                coreLogic = coreLogic,
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
