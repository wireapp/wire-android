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
package com.wire.android.ui.home.settings.about.licenses

import com.mikepenz.aboutlibraries.entity.Library
import com.wire.android.config.CoroutineTestExtension
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class)
class LicensesViewModelTest {

    @Test
    fun givenLicensesProvider_whenViewModelIsCreated_thenStateUsesProvidedLibraries() = runTest {
        val libraries = listOf(
            library(uniqueId = "kotlinx-coroutines", name = "Kotlinx Coroutines"),
            library(uniqueId = "aboutlibraries", name = "AboutLibraries")
        )

        val viewModel = LicensesViewModel(
            licensesProvider = FakeLicensesProvider(libraries)
        )

        assertEquals(
            LicensesState(libraryList = libraries),
            viewModel.state
        )
    }

    private class FakeLicensesProvider(
        private val libraries: List<Library>
    ) : LicensesProvider {
        override suspend fun getLibraries(): List<Library> = libraries
    }

    private fun library(uniqueId: String, name: String): Library = Library(
        uniqueId = uniqueId,
        artifactVersion = null,
        name = name,
        description = null,
        website = null,
        developers = emptyList(),
        organization = null,
        scm = null
    )
}
