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

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test

class CreateAccountOverviewViewModelTest {

    @Test
    fun `custom links remain optional input and become effective config`() {
        val custom = TestLinks("custom", "custom-pricing")
        val viewModel = createViewModel(customServerConfig = custom)

        assertEquals(custom, viewModel.customServerConfig)
        assertEquals(custom, viewModel.serverConfig)
    }

    @Test
    fun `absent custom links preserve null while default becomes effective config`() {
        val default = TestLinks("default", "default-pricing")
        val viewModel = createViewModel(defaultServerConfig = default)

        assertNull(viewModel.customServerConfig)
        assertEquals(default, viewModel.serverConfig)
    }

    @Test
    fun `learn more URL derives from custom effective config`() {
        val custom = TestLinks("custom", "custom-pricing")

        assertEquals(
            "custom-pricing",
            createViewModel(customServerConfig = custom).learnMoreUrl(),
        )
    }

    @Test
    fun `learn more URL derives from default effective config`() {
        val default = TestLinks("default", "default-pricing")

        assertEquals(
            "default-pricing",
            createViewModel(defaultServerConfig = default).learnMoreUrl(),
        )
    }

    private fun createViewModel(
        customServerConfig: TestLinks? = null,
        defaultServerConfig: TestLinks = TestLinks("default", "default-pricing"),
    ) = CreateAccountOverviewViewModel(
        customServerConfig = customServerConfig,
        defaultServerConfig = defaultServerConfig,
        pricingUrl = TestLinks::pricing,
    )

    private data class TestLinks(
        val name: String,
        val pricing: String,
    )
}
