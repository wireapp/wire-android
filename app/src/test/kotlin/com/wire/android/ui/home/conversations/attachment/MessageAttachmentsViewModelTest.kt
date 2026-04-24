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
package com.wire.android.ui.home.conversations.attachment

import androidx.lifecycle.SavedStateHandle
import com.ramcosta.composedestinations.generated.app.navargs.toSavedStateHandle
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.ConversationNavArgs
import com.wire.android.ui.home.conversations.MessageSharedState
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.android.util.FileManager
import com.wire.kalium.cells.domain.CellUploadManager
import com.wire.kalium.cells.domain.usecase.AddAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.ObserveAttachmentDraftsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveAttachmentDraftUseCase
import com.wire.kalium.cells.domain.usecase.RetryAttachmentUploadUseCase
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.ConversationId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class MessageAttachmentsViewModelTest {

    @Test
    fun givenFileWithCleanName_whenFilesSelected_thenDialogIsNotShownAndFileIsAdded() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withHandleUriAssetSuccess("photo.jpg")
            .withAddAttachmentSuccess()
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Hidden)
        coVerify(exactly = 1) { arrangement.addAttachment(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenFileStartingWithDot_whenFilesSelected_thenDialogIsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".hidden.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible)
    }

    @Test
    fun givenFileWithForwardSlash_whenFilesSelected_thenDialogIsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess("bad/name.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible)
    }

    @Test
    fun givenFileWithBackSlash_whenFilesSelected_thenDialogIsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess("bad\\name.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible)
    }

    @Test
    fun givenFileWithDoubleQuote_whenFilesSelected_thenDialogIsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess("bad\"name.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible)
    }

    @Test
    fun givenFileWithIncompatibleName_whenFilesSelected_thenFileIsNotImmediatelyAdded() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".hidden.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        coVerify(exactly = 0) { arrangement.addAttachment(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenFileStartingWithDot_whenFilesSelected_thenDialogStateHasSanitizedName() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".hidden.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        val state = viewModel.incompatibleFileNameDialogState as IncompatibleFileNameDialogState.Visible
        assertEquals("hidden.txt", state.sanitizedFileName)
    }

    @Test
    fun givenFileWithSlashes_whenFilesSelected_thenDialogStateReplacesSlashesWithUnderscore() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess("bad/slash\\name.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        val state = viewModel.incompatibleFileNameDialogState as IncompatibleFileNameDialogState.Visible
        assertEquals("bad_slash_name.txt", state.sanitizedFileName)
    }

    @Test
    fun givenFileWithOnlyDots_whenFilesSelected_thenDialogStateUsesFallbackName() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess("...")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        val state = viewModel.incompatibleFileNameDialogState as IncompatibleFileNameDialogState.Visible
        assertEquals("file", state.sanitizedFileName)
    }

    @Test
    fun givenFileNameEqualToDot_whenFilesSelected_thenDialogIsShown() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible)
    }

    @Test
    fun givenFileNameEqualToDot_whenFilesSelected_thenDialogStateUsesFallbackName() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))

        val state = viewModel.incompatibleFileNameDialogState as IncompatibleFileNameDialogState.Visible
        assertEquals("file", state.sanitizedFileName)
    }

    @Test
    fun givenDialogVisible_whenReplaceAutomatically_thenFileIsAddedWithSanitizedName() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".hidden.txt")
            .withAddAttachmentSuccess()
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))
        viewModel.onReplaceFileNameAutomatically()

        coVerify(exactly = 1) {
            arrangement.addAttachment(
                any(),
                eq("hidden.txt"),
                any(),
                any(),
                any(),
                any()
            )
        }
    }

    @Test
    fun givenDialogVisible_whenReplaceAutomatically_thenDialogIsHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".hidden.txt")
            .withAddAttachmentSuccess()
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))
        viewModel.onReplaceFileNameAutomatically()

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Hidden)
    }

    @Test
    fun givenDialogVisible_whenDismissed_thenDialogIsHiddenAndFileIsNotAdded() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".hidden.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk()))
        viewModel.onDismissIncompatibleFileNameDialog()

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Hidden)
        coVerify(exactly = 0) { arrangement.addAttachment(any(), any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenTwoFilesWithIncompatibleNames_whenFilesSelected_thenDialogIsShownForFirst() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".first.txt", ".second.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk(), mockk()))

        val state = viewModel.incompatibleFileNameDialogState as IncompatibleFileNameDialogState.Visible
        assertEquals("first.txt", state.sanitizedFileName)
    }

    @Test
    fun givenTwoFilesWithIncompatibleNames_whenFirstIsReplaced_thenDialogIsShownForSecond() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".first.txt", ".second.txt")
            .withAddAttachmentSuccess()
            .arrange()

        viewModel.onFilesSelected(listOf(mockk(), mockk()))
        viewModel.onReplaceFileNameAutomatically()

        val state = viewModel.incompatibleFileNameDialogState as IncompatibleFileNameDialogState.Visible
        assertEquals("second.txt", state.sanitizedFileName)
    }

    @Test
    fun givenTwoFilesWithIncompatibleNames_whenBothReplaced_thenDialogIsHidden() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".first.txt", ".second.txt")
            .withAddAttachmentSuccess()
            .arrange()

        viewModel.onFilesSelected(listOf(mockk(), mockk()))
        viewModel.onReplaceFileNameAutomatically()
        viewModel.onReplaceFileNameAutomatically()

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Hidden)
    }

    @Test
    fun givenTwoFilesWithIncompatibleNames_whenFirstIsDismissed_thenDialogIsShownForSecond() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess(".first.txt", ".second.txt")
            .arrange()

        viewModel.onFilesSelected(listOf(mockk(), mockk()))
        viewModel.onDismissIncompatibleFileNameDialog()

        val state = viewModel.incompatibleFileNameDialogState as IncompatibleFileNameDialogState.Visible
        assertEquals("second.txt", state.sanitizedFileName)
    }

    @Test
    fun givenMixedFiles_whenFilesSelected_thenCleanFileIsAddedImmediatelyAndIncompatibleShowsDialog() = runTest {
        val (_, viewModel) = Arrangement()
            .withHandleUriAssetSuccess("clean.txt", ".bad.txt")
            .withAddAttachmentSuccess()
            .arrange()

        viewModel.onFilesSelected(listOf(mockk(), mockk()))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible)
    }

    @Test
    fun givenBundleWithIncompatibleName_whenFilesAddedAsBundle_thenDialogIsShown() = runTest {
        val (_, viewModel) = Arrangement().arrange()

        viewModel.onFilesAddedAsBundle(listOf(testBundle(".bad.txt")))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Visible)
    }

    @Test
    fun givenBundleWithCleanName_whenFilesAddedAsBundle_thenDialogIsNotShownAndFileIsAdded() = runTest {
        val (arrangement, viewModel) = Arrangement()
            .withAddAttachmentSuccess()
            .arrange()

        viewModel.onFilesAddedAsBundle(listOf(testBundle("clean.pdf")))

        assertTrue(viewModel.incompatibleFileNameDialogState is IncompatibleFileNameDialogState.Hidden)
        coVerify(exactly = 1) { arrangement.addAttachment(any(), eq("clean.pdf"), any(), any(), any(), any()) }
    }

    private fun testBundle(fileName: String) = AssetBundle(
        key = "key",
        mimeType = "text/plain",
        dataPath = "/tmp/file".toPath(),
        dataSize = 100L,
        fileName = fileName,
        assetType = AttachmentType.GENERIC_FILE,
    )

    private class Arrangement {

        // Use the generated toSavedStateHandle() extension which correctly serializes via
        // qualifiedIDNavType (QualifiedID uses @SerialName("id") for the value field, not "value").
        private val savedStateHandle: SavedStateHandle = ConversationNavArgs(
            conversationId = ConversationId("conv-value", "conv-domain")
        ).toSavedStateHandle()

        @MockK
        lateinit var handleUriAsset: HandleUriAssetUseCase

        @MockK
        lateinit var observeAttachments: ObserveAttachmentDraftsUseCase

        @MockK
        lateinit var addAttachment: AddAttachmentDraftUseCase

        @MockK
        lateinit var removeAttachment: RemoveAttachmentDraftUseCase

        @MockK
        lateinit var retryUpload: RetryAttachmentUploadUseCase

        @MockK
        lateinit var uploadManager: CellUploadManager

        @MockK
        lateinit var fileManager: FileManager

        private val sharedState = MessageSharedState()

        private val uriAssetQueue = ArrayDeque<String>()

        private var isInitialized = false

        private fun initializeMocks() {
            if (!isInitialized) {
                MockKAnnotations.init(this, relaxUnitFun = true)
                coEvery { observeAttachments(any()) } returns MutableSharedFlow()
                coEvery { uploadManager.getUploadInfo(any()) } returns null
                isInitialized = true
            }
        }

        fun withHandleUriAssetSuccess(vararg fileNames: String) = apply {
            initializeMocks()
            uriAssetQueue.addAll(fileNames)
            coEvery { handleUriAsset.invoke(any(), any()) } answers {
                val name = uriAssetQueue.removeFirstOrNull() ?: "file.txt"
                HandleUriAssetUseCase.Result.Success(
                    assetBundle = AssetBundle(
                        key = "key",
                        mimeType = "text/plain",
                        dataPath = "/tmp/$name".toPath(),
                        dataSize = 100L,
                        fileName = name,
                        assetType = AttachmentType.GENERIC_FILE,
                    )
                )
            }
        }

        fun withAddAttachmentSuccess() = apply {
            initializeMocks()
            coEvery { addAttachment(any(), any(), any(), any(), any(), any()) } returns Unit.right()
        }

        fun arrange(): Pair<Arrangement, MessageAttachmentsViewModel> {
            initializeMocks()
            val viewModel = MessageAttachmentsViewModel(
                savedStateHandle = savedStateHandle,
                handleUriAsset = handleUriAsset,
                observeAttachments = observeAttachments,
                addAttachment = addAttachment,
                removeAttachment = removeAttachment,
                retryUpload = retryUpload,
                uploadManager = uploadManager,
                fileManager = fileManager,
                sharedState = sharedState,
            )
            return this to viewModel
        }
    }
}
