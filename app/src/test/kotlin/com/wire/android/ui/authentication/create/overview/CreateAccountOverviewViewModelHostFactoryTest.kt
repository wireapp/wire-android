/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.authentication.create.overview

import com.wire.kalium.logic.configuration.server.ServerConfig
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CreateAccountOverviewViewModelHostFactoryTest {

    @Test
    fun `custom server config remains optional input and supplies effective pricing`() {
        val viewModel = CreateAccountOverviewViewModelHostFactory(ServerConfig.STAGING)
            .create(CreateAccountOverviewNavArgs(ServerConfig.PRODUCTION))

        assertEquals(ServerConfig.PRODUCTION, viewModel.customServerConfig)
        assertEquals(ServerConfig.PRODUCTION, viewModel.serverConfig)
        assertEquals(ServerConfig.PRODUCTION.pricing, viewModel.learnMoreUrl())
    }

    @Test
    fun `default server config supplies effective config and pricing without becoming custom`() {
        val viewModel = CreateAccountOverviewViewModelHostFactory(ServerConfig.STAGING)
            .create(CreateAccountOverviewNavArgs())

        assertNull(viewModel.customServerConfig)
        assertEquals(ServerConfig.STAGING, viewModel.serverConfig)
        assertEquals(ServerConfig.STAGING.pricing, viewModel.learnMoreUrl())
    }
}
