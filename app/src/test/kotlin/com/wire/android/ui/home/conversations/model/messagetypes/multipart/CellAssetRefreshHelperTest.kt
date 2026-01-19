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
package com.wire.android.ui.home.conversations.model.messagetypes.multipart

import com.wire.kalium.cells.domain.model.CellNode
import com.wire.kalium.cells.domain.usecase.RefreshCellAssetStateUseCase
import com.wire.kalium.common.functional.right
import com.wire.kalium.logic.data.asset.AssetTransferStatus
import com.wire.kalium.logic.data.message.CellAssetContent
import com.wire.kalium.logic.featureFlags.KaliumConfigs
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes

class CellAssetRefreshHelperTest {

    //
    // Tests for regular attachments
    //

    @Test
    fun `with attachment with url expiration available when displayed first time then refresh scheduled with url expiration`() = runTest {

        val (_, refreshHelper) = Arrangement().arrange(this)

        val expiration = currentTime + 300

        refreshHelper.onAttachmentsVisible(
            listOf(
                cellAsset.copy(
                    contentUrlExpiresAt = expiration
                )
            )
        )

        assertTrue(refreshHelper.regularAssets.contains(cellAsset.id))

        advanceTimeBy(301)

        assertFalse(refreshHelper.regularAssets.contains(cellAsset.id))
    }

    @Test
    fun `with attachment with no url expiration available when displayed first time then refresh scheduled with default expiration`() = runTest {

        val (_, refreshHelper) = Arrangement().arrange(this)

        refreshHelper.onAttachmentsVisible(
            listOf(
                cellAsset.copy(
                    contentUrlExpiresAt = null
                )
            )
        )

        assertTrue(refreshHelper.regularAssets.contains(cellAsset.id))

        advanceTimeBy(59.minutes.inWholeMilliseconds)

        assertTrue(refreshHelper.regularAssets.contains(cellAsset.id))

        advanceTimeBy(2.minutes.inWholeMilliseconds)

        assertFalse(refreshHelper.regularAssets.contains(cellAsset.id))
    }

    @Test
    fun `with attachment when displayed first time then refresh called`() = runTest(UnconfinedTestDispatcher()) {

        var count = 0

        val (_, refreshHelper) = Arrangement()
            .withRefreshAsset {
                count++
                testNode.right()
            }
            .arrange(this)

        refreshHelper.onAttachmentsVisible(
            listOf(cellAsset)
        )

        assertEquals(1, count)
    }

    @Test
    fun `with attachment when displayed second time then no refresh called`() = runTest(UnconfinedTestDispatcher()) {

        var count = 0

        val (_, refreshHelper) = Arrangement()
            .withRefreshAsset {
                count++
                testNode.right()
            }
            .arrange(this)

        refreshHelper.regularAssets[cellAsset.id] = Unit

        refreshHelper.onAttachmentsVisible(
            listOf(cellAsset)
        )

        assertEquals(0, count)
    }

    @Test
    fun `with attachment when expire then refresh is called`() = runTest(UnconfinedTestDispatcher()) {

        var count = 0

        val (_, refreshHelper) = Arrangement()
            .withRefreshAsset {
                count++
                testNode.right()
            }
            .arrange(this)

        val expiration = currentTime + 300

        refreshHelper.onAttachmentsVisible(
            listOf(
                cellAsset.copy(
                    contentUrlExpiresAt = expiration
                )
            )
        )

        assertTrue(refreshHelper.regularAssets.contains(cellAsset.id))
        assertEquals(1, count) // First refresh when added

        advanceTimeBy(301)

        assertFalse(refreshHelper.regularAssets.contains(cellAsset.id))
        assertEquals(2, count) // Second refresh when expired
    }

    //
    // Tests for editable attachments
    //

    //
    // TODO: Refresh reschedule for editable assets causes infinite reschedule - expire loop in test scope.
    //

    private inner class Arrangement {

        val featureFlags = KaliumConfigs(
            collaboraIntegration = true
        )

        var refreshAsset = RefreshCellAssetStateUseCase { testNode.right() }

        fun withRefreshAsset(useCase: RefreshCellAssetStateUseCase) = apply {
            refreshAsset = useCase
        }

        fun arrange(scope: TestScope) = this to CellAssetRefreshHelper(
            refreshAsset = refreshAsset,
            featureFlags = featureFlags,
            coroutineScope = scope,
            currentTime = { scope.currentTime }
        )
    }
}

private val testNode = CellNode(
    uuid = "uuid",
    versionId = "versionId",
    path = "path",
    modified = 0,
    size = 0,
    eTag = "eTag",
    type = "type",
    isRecycled = false,
    isDraft = false
)

private val cellAsset = CellAssetContent(
    id = "assetId1",
    versionId = "v1",
    mimeType = "image/png",
    assetPath = null,
    assetSize = 1024,
    metadata = null,
    transferStatus = AssetTransferStatus.UPLOADED
)
