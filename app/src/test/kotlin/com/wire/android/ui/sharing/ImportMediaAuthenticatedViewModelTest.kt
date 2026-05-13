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
package com.wire.android.ui.sharing

import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.paging.PagingData
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.framework.TestConversationItem
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class, SnapshotExtension::class)
class ImportMediaAuthenticatedViewModelTest {
    private val dispatcherProvider = TestDispatcherProvider()

    @Test
    fun `given search query, when collecting conversations, then call use case with proper params`() = runTest(dispatcherProvider.main()) {
        // Given
        val searchQueryText = "search"
        val (arrangement, viewModel) = Arrangement().arrange()
        viewModel.importMediaState.conversations.test {
            // When
            viewModel.searchQueryTextState.setTextAndPlaceCursorAtEnd(searchQueryText)
            advanceUntilIdle()
            // Then
            coVerify(exactly = 1) {
                arrangement.getConversationsPaginated(
                    searchQuery = searchQueryText,
                    fromArchive = false,
                    newActivitiesOnTop = false,
                    onlyInteractionEnabled = true,
                    useStrictMlsFilter = false,
                )
            }
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `given shared text, when handling received content, then import text without importing assets`() =
        runTest(dispatcherProvider.main()) {
            // Given
            val (arrangement, viewModel) = Arrangement().arrange()

            // When
            viewModel.handleReceivedDataFromSharingIntent(
                ImportMediaSharingContent(text = "shared text", assetUris = emptyList())
            )

            // Then
            assertEquals("shared text", viewModel.importMediaState.importedText)
            assertTrue(viewModel.importMediaState.importedAssets.isEmpty())
            assertEquals(emptyList<String>(), arrangement.importMediaAssetImporter.importedUris)
        }

    @Test
    fun `given single shared asset, when handling received content, then import asset`() = runTest(dispatcherProvider.main()) {
        // Given
        val importedAsset = ImportedMediaAsset(testBundle("image.jpg"), assetSizeExceeded = null)
        val (arrangement, viewModel) = Arrangement()
            .withImportedAsset("content://asset/1", importedAsset)
            .arrange()

        // When
        viewModel.handleReceivedDataFromSharingIntent(
            ImportMediaSharingContent(text = null, assetUris = listOf("content://asset/1"))
        )

        // Then
        assertEquals(listOf("content://asset/1"), arrangement.importMediaAssetImporter.importedUris)
        assertEquals(listOf(importedAsset), viewModel.importMediaState.importedAssets)
    }

    @Test
    fun `given shared asset exceeds max size, when handling received content, then emit snackbar`() = runTest(dispatcherProvider.main()) {
        // Given
        val importedAsset = ImportedMediaAsset(testBundle("large.mov"), assetSizeExceeded = 25)
        val (_, viewModel) = Arrangement()
            .withImportedAsset("content://asset/large", importedAsset)
            .arrange()

        // Then
        viewModel.infoMessage.test {
            // When
            viewModel.handleReceivedDataFromSharingIntent(
                ImportMediaSharingContent(text = null, assetUris = listOf("content://asset/large"))
            )

            assertTrue(awaitItem() is SendMessagesSnackbarMessages.MaxAssetSizeExceeded)
        }
    }

    @Test
    fun `given multiple shared assets, when one fails to import, then keep successful assets`() = runTest(dispatcherProvider.main()) {
        // Given
        val importedAsset = ImportedMediaAsset(testBundle("clean.pdf"), assetSizeExceeded = null)
        val (arrangement, viewModel) = Arrangement()
            .withImportedAsset("content://asset/clean", importedAsset)
            .withImportedAsset("content://asset/broken", null)
            .arrange()

        // When
        viewModel.handleReceivedDataFromSharingIntent(
            ImportMediaSharingContent(
                text = null,
                assetUris = listOf("content://asset/clean", "content://asset/broken")
            )
        )

        // Then
        assertEquals(listOf("content://asset/clean", "content://asset/broken"), arrangement.importMediaAssetImporter.importedUris)
        assertEquals(listOf(importedAsset), viewModel.importMediaState.importedAssets)
    }

    private fun testBundle(fileName: String) = AssetBundle(
        key = "key",
        mimeType = "text/plain",
        dataPath = "/tmp/file".toPath(),
        dataSize = 100L,
        fileName = fileName,
        assetType = AttachmentType.GENERIC_FILE,
    )

    inner class Arrangement {

        @MockK
        lateinit var getSelfUser: ObserveSelfUserUseCase

        @MockK
        lateinit var getConversationsPaginated: GetConversationsFromSearchUseCase

        val importMediaAssetImporter = FakeImportMediaAssetImporter()

        @MockK
        lateinit var persistNewSelfDeletionTimerUseCase: PersistNewSelfDeletionTimerUseCase

        @MockK
        lateinit var observeSelfDeletionSettingsForConversation: ObserveSelfDeletionTimerSettingsForConversationUseCase

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery {
                getConversationsPaginated.invoke(any(), any(), any(), any(), useStrictMlsFilter = any())
            } returns flowOf(
                PagingData.from(listOf(TestConversationItem.CONNECTION, TestConversationItem.PRIVATE, TestConversationItem.GROUP))
            )
            coEvery {
                getSelfUser.invoke()
            } returns flowOf(TestUser.SELF_USER)
        }

        fun withImportedAsset(uri: String, importedMediaAsset: ImportedMediaAsset?) = apply {
            importMediaAssetImporter.assets[uri] = importedMediaAsset
        }

        fun arrange() = this to ImportMediaAuthenticatedViewModel(
            getSelf = getSelfUser,
            getConversationsPaginated = getConversationsPaginated,
            importMediaAssetImporter = importMediaAssetImporter,
            persistNewSelfDeletionTimerUseCase = persistNewSelfDeletionTimerUseCase,
            observeSelfDeletionSettingsForConversation = observeSelfDeletionSettingsForConversation,
            dispatchers = dispatcherProvider,
        )
    }

    private class FakeImportMediaAssetImporter : ImportMediaAssetImporter {
        val importedUris = mutableListOf<String>()
        val assets = mutableMapOf<String, ImportedMediaAsset?>()

        override suspend fun importAsset(uri: String): ImportedMediaAsset? {
            importedUris += uri
            return assets[uri]
        }
    }
}
