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
package com.wire.android.ui.home.settings.about.dependencies

import com.wire.android.config.CoroutineTestExtension
import kotlinx.collections.immutable.persistentMapOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class DependenciesViewModelTest {

    @Test
    fun givenDependenciesInfoProvider_whenViewModelIsCreated_thenStateUsesProvidedDependencies() = runTest {
        val viewModel = DependenciesViewModel(
            dependenciesInfoProvider = FakeDependenciesInfoProvider(
                dependencies = mapOf(
                    "coil" to "3.0.0",
                    "local-tooling" to null
                )
            )
        )
        advanceUntilIdle()

        assertEquals(
            DependenciesState(
                dependencies = persistentMapOf(
                    "coil" to "3.0.0",
                    "local-tooling" to null
                )
            ),
            viewModel.state
        )
    }

    private class FakeDependenciesInfoProvider(
        private val dependencies: Map<String, String?>
    ) : DependenciesInfoProvider {
        override suspend fun dependenciesVersion(): Map<String, String?> = dependencies
    }
}
