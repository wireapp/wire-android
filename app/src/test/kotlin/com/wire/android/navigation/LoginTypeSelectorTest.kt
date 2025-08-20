/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.navigation

import com.wire.android.config.DefaultServerConfig
import com.wire.android.util.newServerConfig
import com.wire.kalium.logic.CoreLogic
import com.wire.kalium.logic.configuration.server.ServerConfig
import com.wire.kalium.logic.feature.auth.LoginContext
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test

class LoginTypeSelectorTest {

    @Test
    fun `given default config with enterprise context, then can use new login`() =
        runTest {
            val (_, loginTypeSelector) = Arrangement()
                .withContextFlowForConfig(DefaultServerConfig, flowOf(LoginContext.EnterpriseLogin))
                .arrange(true)
            val result = loginTypeSelector.canUseNewLogin()
            assertEquals(true, result)
        }

    @Test
    fun `given custom config with enterprise context, then can use new login`() =
        runTest {
            val config = newServerConfig(1)
            val (_, loginTypeSelector) = Arrangement()
                .withContextFlowForConfig(DefaultServerConfig, flowOf(LoginContext.FallbackLogin))
                .withContextFlowForConfig(config.links, flowOf(LoginContext.EnterpriseLogin))
                .arrange(true)
            val result = loginTypeSelector.canUseNewLogin(config.links)
            assertEquals(true, result)
        }

    @Test
    fun `given default config with fallback context, then cannot use new login`() =
        runTest {
            val (_, loginTypeSelector) = Arrangement()
                .withContextFlowForConfig(DefaultServerConfig, flowOf(LoginContext.FallbackLogin))
                .arrange(false)
            val result = loginTypeSelector.canUseNewLogin()
            assertEquals(false, result)
        }

    @Test
    fun `given custom config with fallback context, then cannot use new login`() =
        runTest {
            val config = newServerConfig(1)
            val (_, loginTypeSelector) = Arrangement()
                .withContextFlowForConfig(DefaultServerConfig, flowOf(LoginContext.EnterpriseLogin))
                .withContextFlowForConfig(config.links, flowOf(LoginContext.FallbackLogin))
                .arrange(true)
            val result = loginTypeSelector.canUseNewLogin(config.links)
            assertEquals(false, result)
        }

    @Test
    fun `given custom config with fallback context, when context changes to enterprise, then can use new login after it changes`() =
        runTest {
            val config = newServerConfig(1)
            val contextFlow = MutableStateFlow<LoginContext>(LoginContext.FallbackLogin)
            val (_, loginTypeSelector) = Arrangement()
                .withContextFlowForConfig(DefaultServerConfig, flowOf(LoginContext.FallbackLogin))
                .withContextFlowForConfig(config.links, contextFlow)
                .arrange(true)
            val resultBeforeChange = loginTypeSelector.canUseNewLogin(config.links)
            assertEquals(false, resultBeforeChange)
            contextFlow.value = LoginContext.EnterpriseLogin
            val resultAfterChange = loginTypeSelector.canUseNewLogin(config.links)
            assertEquals(true, resultAfterChange)
        }

    inner class Arrangement {
        @MockK
        lateinit var coreLogic: CoreLogic

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        fun arrange(useNewLoginForDefaultBackend: Boolean) = this to LoginTypeSelector(
            coreLogic = { coreLogic },
            useNewLoginForDefaultBackend = useNewLoginForDefaultBackend
        )

        fun withContextFlowForConfig(config: ServerConfig.Links, contextFlow: Flow<LoginContext>) = apply {
            coEvery { coreLogic.getGlobalScope().observeLoginContext(config) } returns contextFlow
        }
    }
}
