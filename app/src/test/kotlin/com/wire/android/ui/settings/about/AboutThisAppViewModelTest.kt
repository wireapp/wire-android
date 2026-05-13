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
package com.wire.android.ui.settings.about

import com.wire.android.config.CoroutineTestExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class AboutThisAppViewModelTest {

    @Test
    fun givenAppInfoProvider_whenViewModelIsCreated_thenStateUsesProvidedAppInfo() = runTest {
        val viewModel = AboutThisAppViewModel(
            aboutThisAppInfoProvider = FakeAboutThisAppInfoProvider(
                appName = "5.0.0-123-dev",
                gitBuildId = "abc123"
            )
        )

        assertEquals(
            AboutThisAppState(
                appName = "5.0.0-123-dev",
                commitish = "abc123"
            ),
            viewModel.state
        )
    }

    private class FakeAboutThisAppInfoProvider(
        override val appName: String,
        private val gitBuildId: String
    ) : AboutThisAppInfoProvider {
        override fun gitBuildId(): String = gitBuildId
    }
}
