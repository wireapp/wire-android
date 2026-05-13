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
package com.wire.android.ui.home.conversations.media.preview

import androidx.core.net.toUri
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.sharing.ImportedMediaAsset
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.data.id.ConversationId
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.ExtendWith

@OptIn(ExperimentalCoroutinesApi::class)
@ExtendWith(CoroutineTestExtension::class)
class ImagesPreviewViewModelTest {

    @Test
    fun givenAssetUris_whenViewModelIsCreated_thenImportsAssets() = runTest {
        val firstAsset = ImportedMediaAsset(testBundle("first.jpg"), assetSizeExceeded = null)
        val secondAsset = ImportedMediaAsset(testBundle("second.jpg"), assetSizeExceeded = 25)
        val (arrangement, viewModel) = Arrangement()
            .withImportedAsset("content://asset/first", firstAsset)
            .withImportedAsset("content://asset/second", secondAsset)
            .arrange(assetUris = arrayListOf("content://asset/first", "content://asset/second"))

        runCurrent()

        assertEquals(listOf("content://asset/first", "content://asset/second"), arrangement.assetImporter.importedUris)
        assertEquals(listOf(firstAsset, secondAsset), viewModel.viewState.assetBundleList)
        assertFalse(viewModel.viewState.isLoading)
    }

    @Test
    fun givenAssetImportFails_whenViewModelIsCreated_thenKeepsSuccessfulAssets() = runTest {
        val importedAsset = ImportedMediaAsset(testBundle("clean.jpg"), assetSizeExceeded = null)
        val (arrangement, viewModel) = Arrangement()
            .withImportedAsset("content://asset/clean", importedAsset)
            .withImportedAsset("content://asset/broken", null)
            .arrange(assetUris = arrayListOf("content://asset/clean", "content://asset/broken"))

        runCurrent()

        assertEquals(listOf("content://asset/clean", "content://asset/broken"), arrangement.assetImporter.importedUris)
        assertEquals(listOf(importedAsset), viewModel.viewState.assetBundleList)
        assertFalse(viewModel.viewState.isLoading)
    }

    @Test
    fun givenImportedAssets_whenSelectingAndRemovingAsset_thenUpdatesState() = runTest {
        val firstAsset = ImportedMediaAsset(testBundle("first.jpg"), assetSizeExceeded = null)
        val secondAsset = ImportedMediaAsset(testBundle("second.jpg"), assetSizeExceeded = null)
        val (_, viewModel) = Arrangement()
            .withImportedAsset("content://asset/first", firstAsset)
            .withImportedAsset("content://asset/second", secondAsset)
            .arrange(assetUris = arrayListOf("content://asset/first", "content://asset/second"))
        runCurrent()

        viewModel.onSelected(1)
        viewModel.onRemove(0)

        assertEquals(1, viewModel.viewState.selectedIndex)
        assertEquals(listOf(secondAsset), viewModel.viewState.assetBundleList)
    }

    private fun testBundle(fileName: String) = AssetBundle(
        key = fileName,
        mimeType = "image/jpeg",
        dataPath = "/tmp/$fileName".toPath(),
        dataSize = 100L,
        fileName = fileName,
        assetType = AttachmentType.IMAGE,
    )

    private class Arrangement {
        val assetImporter = FakeImagesPreviewAssetImporter()

        fun withImportedAsset(uri: String, importedMediaAsset: ImportedMediaAsset?) = apply {
            assetImporter.assets[uri] = importedMediaAsset
        }

        fun arrange(
            assetUris: ArrayList<String> = arrayListOf(),
        ): Pair<Arrangement, ImagesPreviewViewModel> = this to ImagesPreviewViewModel(
            navArgs = navArgs(assetUris),
            assetImporter = assetImporter,
        )

        private fun navArgs(assetUris: ArrayList<String>): ImagesPreviewNavArgs =
            ImagesPreviewNavArgs(
                conversationId = ConversationId("conversation-value", "conversation-domain"),
                conversationName = "Conversation",
                assetUriList = ArrayList(assetUris.map { it.toUri() })
            )
    }

    private class FakeImagesPreviewAssetImporter : ImagesPreviewAssetImporter {
        val importedUris = mutableListOf<String>()
        val assets = mutableMapOf<String, ImportedMediaAsset?>()

        override suspend fun importAsset(uri: String): ImportedMediaAsset? {
            importedUris += uri
            return assets[uri]
        }
    }
}
