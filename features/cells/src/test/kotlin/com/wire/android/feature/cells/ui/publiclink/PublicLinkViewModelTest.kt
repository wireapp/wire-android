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
package com.wire.android.feature.cells.ui.publiclink

import androidx.lifecycle.SavedStateHandle
import app.cash.turbine.test
import com.wire.android.config.NavigationTestExtension
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.feature.cells.util.FileHelper
import com.wire.kalium.cells.domain.model.PublicLink
import com.wire.kalium.cells.domain.usecase.publiclink.CreatePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.DeletePublicLinkUseCase
import com.wire.kalium.cells.domain.usecase.publiclink.GetPublicLinkUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.left
import com.wire.kalium.common.functional.right
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(NavigationTestExtension::class)
class PublicLinkViewModelTest {

    private companion object {
        val testLink = PublicLink(
            uuid = "linkUuid",
            url = "https://publicLink.com",
        )
    }

    @BeforeEach
    fun beforeEach() {
        Dispatchers.setMain(UnconfinedTestDispatcher())
    }

    @AfterEach
    fun afterEach() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given public link not available when view model is created empty state is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withoutPublicLink()
            .arrange()

        viewModel.state.test {
            with(expectMostRecentItem()) {
                assertFalse(isEnabled)
                assertFalse(isLinkAvailable)
            }
        }
    }

    @Test
    fun `given public link available when view model is created link data is loaded`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPublicLink()
            .withLoadSuccess()
            .arrange()

        viewModel.state.test {
            with(expectMostRecentItem()) {
                assertTrue(isEnabled)
                assertTrue(isLinkAvailable)
            }
        }
    }

    @Test
    fun `given public link available when link load fails error is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPublicLink()
            .withLoadFailure()
            .arrange()

        viewModel.actions.test {
            with(expectMostRecentItem()) {
                assertTrue(this is ShowError)
                assertTrue((this as ShowError).closeScreen)
            }
        }
    }

    @Test
    fun `given public link available and loaded when disabled then confirmation is shown`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPublicLink()
            .withLoadSuccess()
            .withDeleteSuccess()
            .arrange()

        viewModel.actions.test {
            viewModel.onEnabledClick()
            assertEquals(ShowRemoveConfirmation, awaitItem())
        }
    }

    @Test
    fun `given public link available and loaded when disable confirmed then link is deleted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPublicLink()
            .withLoadSuccess()
            .withDeleteSuccess()
            .arrange()

        viewModel.state.test {

            viewModel.onConfirmRemoval(true)

            with(expectMostRecentItem()) {
                assertFalse(isEnabled)
                assertFalse(isLinkAvailable)
            }
        }
    }

    @Test
    fun `given public link available and loaded when delete fails then error is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withPublicLink()
            .withLoadSuccess()
            .withDeleteFailure()
            .arrange()

        viewModel.actions.test {
            viewModel.onConfirmRemoval(true)
            assertEquals(ShowErrorDialog(PublicLinkError.Remove), awaitItem())
        }
    }

    @Test
    fun `given public link not available when enabled then link is created`() = runTest {
        val (_, viewModel) = Arrangement()
            .withoutPublicLink()
            .withCreateSuccess()
            .arrange()

        viewModel.state.test {

            viewModel.onEnabledClick()

            with(expectMostRecentItem()) {
                assertTrue(isEnabled)
                assertTrue(isLinkAvailable)
            }
        }
    }

    @Test
    fun `given public link not available when create fails then error is emitted`() = runTest {
        val (_, viewModel) = Arrangement()
            .withoutPublicLink()
            .withCreateFailure()
            .arrange()

        viewModel.actions.test {
            viewModel.onEnabledClick()
            assertEquals(ShowErrorDialog(PublicLinkError.Create), awaitItem())
        }
    }

    @Test
    fun `given public link available when share is called then share chooser is opened`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withPublicLink()
            .withLoadSuccess()
            .arrange()

        viewModel.shareLink()

        coVerify(exactly = 1) { arrangement.fileHelper.shareUrlChooser(any(), any()) }
    }

    private class Arrangement {

        @MockK
        lateinit var savedStateHandle: SavedStateHandle

        @MockK
        lateinit var createPublicLinkUseCase: CreatePublicLinkUseCase

        @MockK
        lateinit var getPublicLinkUseCase: GetPublicLinkUseCase

        @MockK
        lateinit var deletePublicLinkUseCase: DeletePublicLinkUseCase

        @MockK
        lateinit var fileHelper: FileHelper

        init {

            MockKAnnotations.init(this, relaxUnitFun = true)

            every { savedStateHandle.navArgs<PublicLinkNavArgs>() } returns PublicLinkNavArgs(
                assetId = "assetId",
                fileName = "fileName",
                publicLinkId = "publicLinkId",
                isFolder = false,
            )
        }

        fun withPublicLink() = apply {
            every { savedStateHandle.navArgs<PublicLinkNavArgs>() } returns PublicLinkNavArgs(
                assetId = "assetId",
                fileName = "fileName",
                publicLinkId = "publicLinkId",
                isFolder = false,
            )
        }

        fun withoutPublicLink() = apply {
            every { savedStateHandle.navArgs<PublicLinkNavArgs>() } returns PublicLinkNavArgs(
                assetId = "assetId",
                fileName = "fileName",
                publicLinkId = null,
                isFolder = false,
            )
        }

        fun withLoadSuccess() = apply {
            coEvery { getPublicLinkUseCase(any()) } returns testLink.right()
        }

        fun withLoadFailure() = apply {
            coEvery { getPublicLinkUseCase(any()) } returns CoreFailure.Unknown(IllegalStateException("Test")).left()
        }

        fun withCreateSuccess() = apply {
            coEvery { createPublicLinkUseCase(any(), any()) } returns testLink.right()
        }

        fun withCreateFailure() = apply {
            coEvery { createPublicLinkUseCase(any(), any()) } returns CoreFailure.Unknown(IllegalStateException("Test")).left()
        }

        fun withDeleteSuccess() = apply {
            coEvery { deletePublicLinkUseCase(any()) } returns Unit.right()
        }

        fun withDeleteFailure() = apply {
            coEvery { deletePublicLinkUseCase(any()) } returns CoreFailure.Unknown(IllegalStateException("Test")).left()
        }

        fun arrange(): Pair<Arrangement, PublicLinkViewModel> {
            return this to PublicLinkViewModel(
                savedStateHandle = savedStateHandle,
                createPublicLink = createPublicLinkUseCase,
                getPublicLinkUseCase = getPublicLinkUseCase,
                deletePublicLinkUseCase = deletePublicLinkUseCase,
                fileHelper = fileHelper,
            )
        }
    }
}
