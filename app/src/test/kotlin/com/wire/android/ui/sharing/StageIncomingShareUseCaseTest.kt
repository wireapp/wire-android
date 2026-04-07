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
package com.wire.android.ui.sharing

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import com.wire.android.config.TestDispatcherProvider
import com.wire.android.ui.home.conversations.model.AssetBundle
import com.wire.android.ui.home.conversations.usecase.HandleUriAssetUseCase
import com.wire.kalium.logic.data.asset.AttachmentType
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import kotlinx.coroutines.test.runTest
import okio.Path.Companion.toPath
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(application = Application::class)
class StageIncomingShareUseCaseTest {

    @Test
    fun `given text-only share intent, when staging, then store import session without assets`() = runTest {
        val (arrangement, useCase) = Arrangement()
            .withStoredSessionId("session-id")
            .arrange()

        val result = useCase(
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_TEXT, "hello")
            }
        )

        assertEquals(StageIncomingShareUseCase.Result.Success("session-id"), result)
        coVerify(exactly = 1) { arrangement.importSessionStore.store("hello", emptyList()) }
    }

    @Test
    fun `given share intent with Wire provider uri, when staging, then reject it`() = runTest {
        val (_, useCase) = Arrangement().arrange()

        val result = useCase(
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, Uri.parse("content://com.wire.provider/shared.jpg"))
            }
        )

        assertTrue(result is StageIncomingShareUseCase.Result.Failure.InvalidContent)
    }

    @Test
    fun `given valid shared uri, when staging, then import asset and store session`() = runTest {
        val assetBundle = AssetBundle(
            key = "key",
            mimeType = "image/jpeg",
            dataPath = "tmp/shared.jpg".toPath(),
            dataSize = 42L,
            fileName = "shared.jpg",
            assetType = AttachmentType.IMAGE
        )
        val sharedUri = Uri.parse("content://external.provider/shared.jpg")
        val (arrangement, useCase) = Arrangement()
            .withStoredSessionId("session-id")
            .withImportedAsset(sharedUri, HandleUriAssetUseCase.Result.Success(assetBundle))
            .arrange()

        val result = useCase(
            Intent(Intent.ACTION_SEND).apply {
                putExtra(Intent.EXTRA_STREAM, sharedUri)
            }
        )

        assertEquals(StageIncomingShareUseCase.Result.Success("session-id"), result)
        coVerify(exactly = 1) { arrangement.handleUriAsset.invoke(sharedUri, false) }
        coVerify(exactly = 1) {
            arrangement.importSessionStore.store(
                null,
                listOf(ImportedMediaAsset(assetBundle, null))
            )
        }
    }

    private class Arrangement {

        @MockK
        lateinit var context: Context

        @MockK
        lateinit var handleUriAsset: HandleUriAssetUseCase

        @MockK
        lateinit var importSessionStore: ImportSessionStore

        init {
            MockKAnnotations.init(this, relaxUnitFun = true)
            coEvery { context.packageName } returns "com.wire"
            withStoredSessionId("session-id")
        }

        fun withStoredSessionId(sessionId: String) = apply {
            coEvery { importSessionStore.store(any(), any()) } returns sessionId
        }

        fun withImportedAsset(uri: Uri, result: HandleUriAssetUseCase.Result) = apply {
            coEvery { handleUriAsset.invoke(uri, false, any(), any()) } returns result
        }

        fun arrange() = this to StageIncomingShareUseCase(
            context = context,
            handleUriAsset = handleUriAsset,
            importSessionStore = importSessionStore,
            dispatchers = TestDispatcherProvider(),
        )
    }
}
