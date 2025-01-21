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
package com.wire.android.ui.common.bottomsheet.folder

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import app.cash.turbine.test
import com.wire.android.R
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.model.DefaultSnackBarMessage
import com.wire.android.ui.home.conversations.folder.FolderNameState
import com.wire.android.ui.home.conversations.folder.NewFolderViewModel
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.CoreFailure
import com.wire.kalium.logic.data.conversation.ConversationFolder
import com.wire.kalium.logic.data.conversation.FolderType
import com.wire.kalium.logic.feature.conversation.folder.CreateConversationFolderUseCase
import com.wire.kalium.logic.feature.conversation.folder.ObserveUserFoldersUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.consumeAsFlow
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class NewFolderViewModelTest {

    @Test
    fun `given initial empty text, then no error should be set and button should remain disabled`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
        }

        arrangement.userFoldersChannel.send(listOf())
        advanceUntilIdle()

        arrangement.updateTextState("")

        assertFalse(viewModel.folderNameState.buttonEnabled)
        assertEquals(FolderNameState.NameError.None, viewModel.folderNameState.error)
    }

    @Test
    fun `given folder name is empty, then buttonEnabled should be false and error should be NameEmptyError`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
        }

        arrangement.userFoldersChannel.send(listOf())
        arrangement.updateTextState("3434")
        advanceUntilIdle()

        arrangement.updateTextState("")
        advanceUntilIdle()

        assertFalse(viewModel.folderNameState.buttonEnabled)
        assertEquals(
            FolderNameState.NameError.TextFieldError.NameEmptyError,
            viewModel.folderNameState.error
        )
    }

    @Test
    fun `given folder name exceeds limit, then buttonEnabled should be false and error should be NameExceedLimitError`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {}
        arrangement.userFoldersChannel.send(listOf())
        arrangement.updateTextState("a".repeat(NewFolderViewModel.NAME_MAX_COUNT + 1))

        advanceUntilIdle()

        assertFalse(viewModel.folderNameState.buttonEnabled)
        assertEquals(
            FolderNameState.NameError.TextFieldError.NameExceedLimitError,
            viewModel.folderNameState.error
        )
    }

    @Test
    fun `given folder name already exists, then buttonEnabled should be false and error should be NameAlreadyExistError`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
        }

        arrangement.userFoldersChannel.send(listOf(ConversationFolder(id = "folderId", name = "ExistingFolder", type = FolderType.USER)))
        arrangement.updateTextState("ExistingFolder")
        advanceUntilIdle()

        assertFalse(viewModel.folderNameState.buttonEnabled)
        assertEquals(
            FolderNameState.NameError.TextFieldError.NameAlreadyExistError,
            viewModel.folderNameState.error
        )
    }

    @Test
    fun `given valid folder name, then buttonEnabled should be true and error should be None`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
        }

        arrangement.userFoldersChannel.send(listOf(ConversationFolder(id = "folderId", name = "OtherFolder", type = FolderType.USER)))
        arrangement.updateTextState("NewFolder")
        advanceUntilIdle()

        assertTrue(viewModel.folderNameState.buttonEnabled)
        assertEquals(
            FolderNameState.NameError.None,
            viewModel.folderNameState.error
        )
    }

    @Test
    fun `when folder creation fails, then infoMessage should emit failure message`() = runTest {
        val (arrangement, viewModel) = Arrangement().arrange {
            withCreateFolderResult(CreateConversationFolderUseCase.Result.Failure(CoreFailure.Unknown(null)))
        }
        arrangement.userFoldersChannel.send(listOf())

        viewModel.infoMessage.test {
            viewModel.createFolder("NewFolder")
            val result = awaitItem()
            assertEquals(
                DefaultSnackBarMessage(UIText.StringResource(R.string.new_folder_failure, "NewFolder")),
                result
            )
        }
    }

    @Test
    fun `when folder creation succeeds, then folderId should be set in state`() = runTest {
        val folderId = "123"
        val (arrangement, viewModel) = Arrangement().arrange {
            withCreateFolderResult(CreateConversationFolderUseCase.Result.Success(folderId))
        }

        arrangement.userFoldersChannel.send(listOf())
        viewModel.createFolder("NewFolder")

        assertEquals(folderId, viewModel.folderNameState.folderId)
    }

    private class Arrangement {

        @MockK
        lateinit var observeUserFolders: ObserveUserFoldersUseCase

        @MockK
        lateinit var createConversationFolder: CreateConversationFolderUseCase

        val userFoldersChannel = Channel<List<ConversationFolder>>(capacity = Channel.UNLIMITED)

        private lateinit var viewModel: NewFolderViewModel

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { observeUserFolders() } returns userFoldersChannel.consumeAsFlow()
        }

        fun withCreateFolderResult(result: CreateConversationFolderUseCase.Result) = apply {
            coEvery { createConversationFolder(any()) } returns result
        }

        fun updateTextState(text: String) {
            viewModel.textState.setTextAndPlaceCursorAtEnd(text)
        }

        fun arrange(block: Arrangement.() -> Unit) = apply(block).let {
            viewModel = NewFolderViewModel(
                observeUserFolders,
                createConversationFolder
            )
            this to viewModel
        }
    }
}
