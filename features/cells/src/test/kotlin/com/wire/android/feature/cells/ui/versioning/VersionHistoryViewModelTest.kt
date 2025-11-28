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
package com.wire.android.feature.cells.ui.versioning

import androidx.lifecycle.SavedStateHandle
import com.wire.android.feature.cells.R
import com.wire.android.navigation.di.ResourceProvider
import com.wire.kalium.cells.domain.model.NodeVersion
import com.wire.kalium.cells.domain.usecase.versioning.GetNodeVersionsUseCase
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import io.mockk.coEvery
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

@ExperimentalCoroutinesApi
class VersionHistoryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val savedStateHandle: SavedStateHandle = mockk(relaxed = true)
    private val resourceProvider: ResourceProvider = mockk(relaxed = true)
    private val getNodeVersionsUseCase: GetNodeVersionsUseCase = mockk()

    private val testNodeUuid = "test-node-uuid"

    @BeforeEach
    fun setUp() {
        Dispatchers.setMain(testDispatcher)

        every { savedStateHandle.get<String>("uuid") } returns testNodeUuid

        every { resourceProvider.getString(R.string.today_label) } returns "Today"
        every { resourceProvider.getString(R.string.yesterday_label) } returns "Yesterday"
        every { resourceProvider.getString(R.string.size_unit_bytes) } returns "B"
        every { resourceProvider.getString(R.string.size_unit_kilobytes) } returns "KB"
        every { resourceProvider.getString(R.string.size_unit_megabytes) } returns "MB"
        every { resourceProvider.getString(R.string.size_unit_gigabytes) } returns "GB"
        every { resourceProvider.getString(R.string.size_unit_terabytes) } returns "TB"
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun givenViewModel_whenItInits_thenIsFetchingStateIsManagedCorrectly() = runTest {
        coEvery { getNodeVersionsUseCase(testNodeUuid) } returns Either.Right(emptyList())

        val viewModel = VersionHistoryViewModel(savedStateHandle, resourceProvider, getNodeVersionsUseCase)

        assertTrue(viewModel.isFetchingContent.value)
        advanceUntilIdle()
        assertFalse(viewModel.isFetchingContent.value)
    }

    @Suppress("LongMethod")
    @Test
    fun givenSuccessfulFetch_whenViewModelInits_thenVersionsAreGroupedCorrectly() = runTest {
        val today = LocalDate.now()
        val yesterday = today.minusDays(1)
        val twoDaysAgo = today.minusDays(2)
        val versionNode = NodeVersion(
            id = "v1",
            hash = null,
            description = null,
            isDraft = true,
            etag = "etag",
            editorUrls = null,
            filePreviews = null,
            isHead = false,
            modifiedTime = today.atTime(10, 30).toEpochSecond(ZoneOffset.UTC).toString(),
            ownerName = "User A",
            ownerUuid = "uuid",
            getUrl = null,
            size = "1500"
        )

        val versionsFromApi = listOf(
            versionNode,
            versionNode.copy(
                id = "v2",
                ownerName = "User B",
                modifiedTime = yesterday.atTime(14, 0).toEpochSecond(ZoneOffset.UTC).toString(),
                size = "2048"
            ),
            versionNode.copy(
                id = "v3",
                ownerName = "User A",
                modifiedTime = yesterday.atTime(9, 15).toEpochSecond(ZoneOffset.UTC).toString(),
                size = "5000000"
            ),
            versionNode.copy(
                id = "v4",
                ownerName = "User C",
                modifiedTime = twoDaysAgo.atStartOfDay().toEpochSecond(ZoneOffset.UTC).toString(),
                size = "123"
            ),
        )
        coEvery { getNodeVersionsUseCase(testNodeUuid) } returns Either.Right(versionsFromApi)

        val viewModel = VersionHistoryViewModel(savedStateHandle, resourceProvider, getNodeVersionsUseCase)
        testDispatcher.scheduler.advanceUntilIdle()

        // Versions should be grouped into three sections (Today, Yesterday, and an older date)
        val groupedVersions = viewModel.versionsGroupedByTime.value
        assertEquals(3, groupedVersions.size)

        // Verify "Today" group is correct
        val todayFormattedDate = today.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        assertEquals("Today, $todayFormattedDate", groupedVersions[0].dateLabel)
        assertEquals(1, groupedVersions[0].versions.size)
        assertEquals("User A", groupedVersions[0].versions[0].modifiedBy)
        assertEquals("11:30", groupedVersions[0].versions[0].modifiedAt)
        assertEquals("1.46 KB", groupedVersions[0].versions[0].fileSize)

        // Verify "Yesterday" group is correct
        val yesterdayFormattedDate = yesterday.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        assertEquals("Yesterday, $yesterdayFormattedDate", groupedVersions[1].dateLabel)
        assertEquals(2, groupedVersions[1].versions.size)
        assertEquals("User B", groupedVersions[1].versions[0].modifiedBy)
        assertEquals("2.00 KB", groupedVersions[1].versions[0].fileSize)
        assertEquals("User A", groupedVersions[1].versions[1].modifiedBy)
        assertEquals("4.77 MB", groupedVersions[1].versions[1].fileSize)

        // Verify older date group is correct
        val twoDaysAgoFormatted = twoDaysAgo.format(DateTimeFormatter.ofPattern("d MMM yyyy"))
        assertEquals(twoDaysAgoFormatted, groupedVersions[2].dateLabel)
        assertEquals(1, groupedVersions[2].versions.size)
        assertEquals("123.00 B", groupedVersions[2].versions[0].fileSize)
    }

    @Test
    fun givenApiFailure_whenViewModelInits_thenVersionListIsEmpty() = runTest {
        coEvery { getNodeVersionsUseCase(testNodeUuid) } returns Either.Left(CoreFailure.MissingClientRegistration)

        val viewModel = VersionHistoryViewModel(savedStateHandle, resourceProvider, getNodeVersionsUseCase)
        advanceUntilIdle()

        assertTrue(viewModel.versionsGroupedByTime.value.isEmpty())
        assertFalse(viewModel.isFetchingContent.value)
    }

    @Test
    fun givenMissingUuid_whenViewModelInits_thenNoFetchIsAttempted() = runTest {
        every { savedStateHandle.get<String>("uuid") } returns null

        val viewModel = VersionHistoryViewModel(savedStateHandle, resourceProvider, getNodeVersionsUseCase)
        advanceUntilIdle()

        assertTrue(viewModel.versionsGroupedByTime.value.isEmpty())
        assertFalse(viewModel.isFetchingContent.value)
    }
}
