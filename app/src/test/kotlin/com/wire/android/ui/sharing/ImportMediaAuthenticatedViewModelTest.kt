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

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.text.input.setTextAndPlaceCursorAtEnd
import androidx.paging.PagingData
import app.cash.turbine.test
import com.wire.android.config.CoroutineTestExtension
import com.wire.android.config.SnapshotExtension
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.config.mockUri
import com.wire.android.framework.TestConversationItem
import com.wire.android.framework.TestUser
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.usecase.GetConversationsFromSearchUseCase
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.kalium.logic.data.asset.AttachmentType
import com.wire.kalium.logic.feature.selfDeletingMessages.ObserveSelfDeletionTimerSettingsForConversationUseCase
import com.wire.kalium.logic.feature.selfDeletingMessages.PersistNewSelfDeletionTimerUseCase
import com.wire.kalium.logic.feature.user.ObserveSelfUserUseCase
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
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
    fun `given content uri from Wire file provider, when checking provider uri, then match it`() {
        val uri = testUri(authority = "com.wire.android.provider")

        assertTrue(uri.isWireFileProviderUri("com.wire.android.provider"))
    }

    @Test
    fun `given uppercase content scheme from Wire file provider, when checking provider uri, then match it`() {
        val uri = testUri(scheme = "CONTENT", authority = "com.wire.android.provider")

        assertTrue(uri.isWireFileProviderUri("com.wire.android.provider"))
    }

    @Test
    fun `given file uri with Wire authority, when checking provider uri, then reject it`() {
        val uri = testUri(scheme = "file", authority = "com.wire.android.provider")

        assertFalse(uri.isWireFileProviderUri("com.wire.android.provider"))
    }

    @Test
    fun `given uri from provider with Wire authority prefix, when checking provider uri, then reject it`() {
        val uri = testUri(authority = "com.wire.android.provider.evil")

        assertFalse(uri.isWireFileProviderUri("com.wire.android.provider"))
    }

    @Test
    fun `given uri from another content provider, when checking provider uri, then reject it`() {
        val uri = testUri(authority = "com.android.providers.media.documents")

        assertFalse(uri.isWireFileProviderUri("com.wire.android.provider"))
    }

    @Test
    fun `given Wire provider uri under shared files, when checking internal share uri, then allow it`() {
        val uri = testUri(pathSegments = listOf("shared_files", "wire-logs.zip"))

        assertTrue(uri.isWireInternalShareUri("com.wire.android.provider"))
    }

    @Test
    fun `given uri from another provider under shared files, when checking internal share uri, then reject it`() {
        val uri = testUri(
            authority = "com.android.providers.media.documents",
            pathSegments = listOf("shared_files", "wire-logs.zip")
        )

        assertFalse(uri.isWireInternalShareUri("com.wire.android.provider"))
    }

    @Test
    fun `given Wire provider uri outside shared files, when checking internal share uri, then reject it`() {
        val uri = testUri(pathSegments = listOf("cached_files", "private-file.zip"))

        assertFalse(uri.isWireInternalShareUri("com.wire.android.provider"))
    }

    @Test
    fun `given Wire provider uri with shared files prefix confusion, when checking internal share uri, then reject it`() {
        val uri = testUri(pathSegments = listOf("shared_files_evil", "private-file.zip"))

        assertFalse(uri.isWireInternalShareUri("com.wire.android.provider"))
    }

    @Test
    fun `given Wire provider uri with only shared files root, when checking internal share uri, then reject it`() {
        val uri = testUri(pathSegments = listOf("shared_files"))

        assertFalse(uri.isWireInternalShareUri("com.wire.android.provider"))
    }

    @Test
    fun `given Wire provider uri with traversal segment, when checking internal share uri, then reject it`() {
        val uri = testUri(pathSegments = listOf("shared_files", "..", "cached_files", "private-file.zip"))

        assertFalse(uri.isWireInternalShareUri("com.wire.android.provider"))
    }

    @Test
    fun `given internal Wire file provider uri, when handling internal share, then import it`() = runTest(dispatcherProvider.main()) {
        val uri = testUri(pathSegments = listOf("shared_files", "wire-logs.zip"))
        val assetBundle = AssetBundle(
            key = "key",
            mimeType = "application/zip",
            dataPath = "/tmp/wire-logs.zip".toPath(),
            dataSize = 100L,
            fileName = "wire-logs.zip",
            assetType = AttachmentType.GENERIC_FILE
        )
        val (arrangement, viewModel) = Arrangement()
            .withHandleUriAsset(HandleUriAssetUseCase.Result.Success(assetBundle))
            .arrange()

        viewModel.handleReceivedDataFromInternalShare(listOf(uri))

        assertEquals(assetBundle, viewModel.importMediaState.importedAssets.single().assetBundle)
        coVerify(exactly = 1) {
            arrangement.handleUriAssetUseCase.invoke(uri, saveToDeviceIfInvalid = false)
        }
    }

    @Test
    fun `given mixed internal share uris, when handling internal share, then import only allowed Wire shared files`() =
        runTest(dispatcherProvider.main()) {
            val validUri = testUri(pathSegments = listOf("shared_files", "wire-logs.zip"))
            val wrongRootUri = testUri(pathSegments = listOf("cached_files", "private-file.zip"))
            val wrongProviderUri = testUri(
                authority = "com.android.providers.media.documents",
                pathSegments = listOf("shared_files", "wire-logs.zip")
            )
            val assetBundle = assetBundle(fileName = "wire-logs.zip")
            val (arrangement, viewModel) = Arrangement()
                .withHandleUriAsset(HandleUriAssetUseCase.Result.Success(assetBundle))
                .arrange()

            viewModel.handleReceivedDataFromInternalShare(listOf(validUri, wrongRootUri, wrongProviderUri))

            assertEquals(listOf(assetBundle), viewModel.importMediaState.importedAssets.map { it.assetBundle })
            coVerify(exactly = 1) {
                arrangement.handleUriAssetUseCase.invoke(validUri, saveToDeviceIfInvalid = false)
            }
            coVerify(exactly = 0) {
                arrangement.handleUriAssetUseCase.invoke(wrongRootUri, any())
            }
            coVerify(exactly = 0) {
                arrangement.handleUriAssetUseCase.invoke(wrongProviderUri, any())
            }
        }

    @Test
    fun `given internal Wire provider uri outside shared files, when handling internal share, then reject it`() =
        runTest(dispatcherProvider.main()) {
            val uri = testUri(pathSegments = listOf("cached_files", "private-file.zip"))
            val (arrangement, viewModel) = Arrangement().arrange()

            viewModel.handleReceivedDataFromInternalShare(listOf(uri))

            assertTrue(viewModel.importMediaState.importedAssets.isEmpty())
            coVerify(exactly = 0) {
                arrangement.handleUriAssetUseCase.invoke(any(), any())
            }
        }

    @Test
    fun `given internal uri from another provider, when handling internal share, then reject it`() =
        runTest(dispatcherProvider.main()) {
            val uri = testUri(
                authority = "com.android.providers.media.documents",
                pathSegments = listOf("shared_files", "wire-logs.zip")
            )
            val (arrangement, viewModel) = Arrangement().arrange()

            viewModel.handleReceivedDataFromInternalShare(listOf(uri))

            assertTrue(viewModel.importMediaState.importedAssets.isEmpty())
            coVerify(exactly = 0) {
                arrangement.handleUriAssetUseCase.invoke(any(), any())
            }
        }

    private fun testUri(
        scheme: String = "content",
        authority: String = "com.wire.android.provider",
        pathSegments: List<String> = emptyList()
    ): Uri = mockk {
        every { this@mockk.scheme } returns scheme
        every { this@mockk.authority } returns authority
        every { this@mockk.pathSegments } returns pathSegments
    }

    private fun assetBundle(fileName: String) = AssetBundle(
        key = "key",
        mimeType = "application/zip",
        dataPath = "/tmp/$fileName".toPath(),
        dataSize = 100L,
        fileName = fileName,
        assetType = AttachmentType.GENERIC_FILE
    )

    inner class Arrangement {
        val context = mockk<Context> {
            every { packageName } returns "com.wire.android"
        }

        @MockK
        lateinit var getSelfUser: ObserveSelfUserUseCase

        @MockK
        lateinit var getConversationsPaginated: GetConversationsFromSearchUseCase

        @MockK
        lateinit var handleUriAssetUseCase: HandleUriAssetUseCase

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
            mockUri()
        }

        fun withHandleUriAsset(result: HandleUriAssetUseCase.Result) = apply {
            coEvery { handleUriAssetUseCase.invoke(any(), any()) } returns result
        }

        fun arrange() = this to ImportMediaAuthenticatedViewModel(
            context = context,
            getSelf = getSelfUser,
            getConversationsPaginated = getConversationsPaginated,
            handleUriAsset = handleUriAssetUseCase,
            persistNewSelfDeletionTimerUseCase = persistNewSelfDeletionTimerUseCase,
            observeSelfDeletionSettingsForConversation = observeSelfDeletionSettingsForConversation,
            dispatchers = dispatcherProvider,
        )
    }
}
