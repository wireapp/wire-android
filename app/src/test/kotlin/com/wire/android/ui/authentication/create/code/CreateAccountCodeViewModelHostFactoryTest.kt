package com.wire.android.ui.authentication.create.code

import com.wire.android.di.ClientScopeProvider
import com.wire.android.navigation.routes.auth.CreateAccountRegistrationInfo
import com.wire.android.navigation.routes.auth.CreateAccountRouteFlowType
import com.wire.android.navigation.routes.auth.toAuthenticationServerLinks
import com.wire.android.util.ui.CountdownTimer
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.AddAuthenticatedUserUseCase
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNotSame
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CreateAccountCodeViewModelHostFactoryTest {
    @Test fun `host maps nav input custom default and creates distinct timer per view model`() {
        val host = CreateAccountCodeViewModelHostFactory(
            mockk<CoreLogic>(), mockk<AddAuthenticatedUserUseCase>(), mockk<ClientScopeProvider.Factory>(), ServerConfig.STAGING, false,
        )
        val info = CreateAccountRegistrationInfo("alice@example.com", firstName = "Alice", lastName = "Wire", password = "secret")
        val custom = host.create(CreateAccountRouteFlowType.TEAM, info, ServerConfig.PRODUCTION.toAuthenticationServerLinks())
        val fallback = host.create(CreateAccountRouteFlowType.PERSONAL, info, null)
        assertEquals(CreateAccountRouteFlowType.TEAM, custom.flowType); assertEquals(ServerConfig.PRODUCTION, custom.customServerConfig)
        assertEquals(ServerConfig.PRODUCTION, custom.serverConfig); assertNull(fallback.customServerConfig);
            assertEquals(ServerConfig.STAGING, fallback.serverConfig)
        val inputField = CreateAccountCodeViewModel::class.java.getDeclaredField("input").apply { isAccessible = true }
        val input = inputField.get(custom) as CreateAccountCodeInput<*, *>
        assertEquals("alice@example.com", input.email); assertEquals("Alice", input.firstName); assertEquals("Wire", input.lastName)
        assertEquals("secret", input.password); assertEquals(true, input.isTeam)
        val timerField = CreateAccountCodeViewModel::class.java.getDeclaredField("resendCodeTimer").apply { isAccessible = true }
        assertNotSame(timerField.get(custom), timerField.get(fallback))
    }

    @Test fun `android timer adapter delegates seconds and callbacks exactly`() = runTest {
        val countdown = mockk<CountdownTimer>(); val update: (String) -> Unit = {}; val finish: () -> Unit = {}
        coEvery { countdown.start(300L, update, finish) } returns Unit
        AndroidCreateAccountCodeResendTimer(countdown).start(300L, update, finish)
        coVerify(exactly = 1) { countdown.start(300L, update, finish) }
    }
}
