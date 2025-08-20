/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.settings.account.deleteAccount

import com.wire.android.config.CoroutineTestExtension
import com.wire.kalium.logic.feature.user.DeleteAccountUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class DeleteAccountViewModelTest {

    @Test
    fun `when delete account button clicked, then start the delete account flow`() = runTest {
        val (_, viewModel) = Arrangement()
            .arrange()

        viewModel.onDeleteAccountClicked()

        assertTrue(viewModel.state.startDeleteAccountFlow)
    }

    @Test
    fun `when delete account button confirmed, then call use case`() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withDeleteAccountUseCase(DeleteAccountUseCase.Result.Success)
            .arrange()

        viewModel.onDeleteAccountClicked()
        assertTrue(viewModel.state.startDeleteAccountFlow)

        viewModel.onDeleteAccountDialogConfirmed()

        assertFalse(viewModel.state.startDeleteAccountFlow)
        coVerify(exactly = 1) { arrangement.deleteAccountUseCase(null) }
    }

    private class Arrangement {

        @MockK
        lateinit var deleteAccountUseCase: DeleteAccountUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
        }

        private val viewModel: DeleteAccountViewModel = DeleteAccountViewModel(
            deleteAccount = deleteAccountUseCase,
        )

        fun withDeleteAccountUseCase(result: DeleteAccountUseCase.Result) = apply {
            coEvery { deleteAccountUseCase(any()) } returns result
        }
        fun arrange() = this to viewModel
    }
}
