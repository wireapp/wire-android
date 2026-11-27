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
package com.wire.android.feature.cells.ui.search

import com.wire.android.feature.cells.ui.CellNodeChangeEvents
import androidx.paging.testing.asSnapshot
import com.wire.android.feature.cells.ui.CellNodeUpdate
import kotlinx.coroutines.launch
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import com.wire.android.feature.cells.ui.CellFileLocalPathCache
import com.wire.kalium.network.NetworkStateObserver
import com.wire.kalium.network.NetworkState
import com.wire.android.feature.cells.ui.CellNodesSource
import com.wire.android.feature.cells.ui.CellFeatureFlags
import com.wire.android.feature.cells.ui.OfflineDownloadStates
import com.wire.android.feature.cells.ui.offline.SessionOfflineDownloads
import com.wire.android.feature.cells.ui.search.filter.data.FilterConversationUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortCriteriaNavArg
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCaseResult
import com.wire.kalium.cells.domain.usecase.GetPaginatedCellConversationsFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.cells.domain.usecase.offline.ObserveOfflineFilesUseCase
import com.wire.kalium.cells.domain.usecase.offline.OfflineFileInfo
import com.wire.kalium.common.error.CoreFailure
import com.wire.kalium.common.functional.Either
import com.wire.kalium.logic.data.id.ConversationId
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.impl.annotations.MockK
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test

@OptIn(ExperimentalCoroutinesApi::class)
class SearchScreenViewModelTest {

    private companion object {
        const val CONVERSATION_ID = "conversationId123"
        const val FILE_UUID = "fileUuid"
        val mockTags = setOf("tag1", "tag2", "tag3")
    }

    private val dispatcher = StandardTestDispatcher()

    @MockK
    private lateinit var getAllTagsUseCase: GetAllTagsUseCase

    @MockK
    private lateinit var getCellFilesPaged: GetPaginatedFilesFlowUseCase

    @MockK
    private lateinit var getOwners: GetOwnersUseCase

    @MockK
    private lateinit var getPaginatedConversations: GetPaginatedCellConversationsFlowUseCase

    @MockK
    private lateinit var observeOfflineFiles: ObserveOfflineFilesUseCase

    private val sharedPathCache = CellFileLocalPathCache()
    private val nodeChangeEvents = CellNodeChangeEvents()
    private val offlineDownloads = mockk<SessionOfflineDownloads> {
        every { states } returns MutableStateFlow(OfflineDownloadStates())
    }
    private val networkState = MutableStateFlow<NetworkState>(NetworkState.ConnectedWithInternet)
    private val networkStateObserver = mockk<NetworkStateObserver> {
        every { observeNetworkState() } returns networkState
    }
    private val featureFlags = CellFeatureFlags(
        kaliumConfigs = mockk(relaxed = true),
        offlineFilesEnabled = true,
        inAppImageViewerEnabled = true,
        drivePermissionsEnabled = true,
        driveDirectUploadEnabled = true,
        driveBulkActionsEnabled = true,
        driveOfflineEnabled = true,
        bulkRestoreEnabled = true,
        driveOffboardingEnabled = true,
    )

    private fun nodesSource() = CellNodesSource(
        getCellFilesPaged = getCellFilesPaged,
        isAtLeastOneCellAvailable = mockk(relaxed = true),
        observeOfflineFiles = observeOfflineFiles,
        sharedPathCache = sharedPathCache,
        offlineDownloads = offlineDownloads,
        getConversationName = mockk(relaxed = true),
        getUserName = mockk(relaxed = true),
    )

    @BeforeEach
    fun setup() {
        Dispatchers.setMain(dispatcher)

        MockKAnnotations.init(this)

        coEvery { getAllTagsUseCase() } returns Either.Right(mockTags)
        coEvery { getCellFilesPaged(any(), any(), any(), any()) } returns emptyFlow<PagingData<Node>>()
        coEvery { getOwners(any()) } returns GetOwnersUseCaseResult.Failure(CoreFailure.InvalidEventSenderID)
        every { getPaginatedConversations(any()) } returns emptyFlow()
        every { observeOfflineFiles() } returns emptyFlow()
    }

    @AfterEach
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `given initial state, when ViewModel is created, then tags are loaded`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(3, viewModel.uiState.value.availableTags.size)
    }

    @Test
    fun `given SHARED_DRIVE ViewModel is created, then initial sorting is FoldersFirst`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(SortingCriteria.FoldersFirst, viewModel.uiState.value.sortingCriteria)
    }

    @Test
    fun `given DRIVE ViewModel is created, then initial sorting is ByDate NewestFirst`() = runTest {
        val viewModel = createViewModelWithScreenType(DriveSearchScreenType.DRIVE)
        advanceUntilIdle()

        assertEquals(SortingCriteria.ByDate.NewestFirst, viewModel.uiState.value.sortingCriteria)
    }

    @Test
    fun `given SHARED_DRIVE ViewModel, then defaultSortingCriteria is FoldersFirst`() = runTest {
        val viewModel = createViewModel()
        assertEquals(SortingCriteria.FoldersFirst, viewModel.defaultSortingCriteria)
    }

    @Test
    fun `given DRIVE ViewModel, then defaultSortingCriteria is ByDate NewestFirst`() = runTest {
        val viewModel = createViewModelWithScreenType(DriveSearchScreenType.DRIVE)
        assertEquals(SortingCriteria.ByDate.NewestFirst, viewModel.defaultSortingCriteria)
    }

    @Test
    fun `given initial sorting in nav args, when ViewModel is created, then it is inherited`() = runTest {
        val viewModel = createViewModelWithInitialSorting(SortCriteriaNavArg.NameAZ)
        advanceUntilIdle()

        assertEquals(SortingCriteria.ByName.AtoZ, viewModel.inheritedSortingCriteria)
        assertEquals(SortingCriteria.ByName.AtoZ, viewModel.uiState.value.sortingCriteria)
    }

    @Test
    fun `given no initial sorting in nav args, when ViewModel is created, then inherited sorting is the default`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        assertEquals(viewModel.defaultSortingCriteria, viewModel.inheritedSortingCriteria)
    }

    @Test
    fun `given parentRoute in nav args, when ViewModel is created, then parentRoute is exposed`() = runTest {
        val expectedRoute = "app/global_cells_screen"
        val viewModel = SearchScreenViewModel(
            navArgs = SearchNavArgs(CONVERSATION_ID, DriveSearchScreenType.SHARED_DRIVE, expectedRoute),
            getAllTagsUseCase = getAllTagsUseCase,
            getCellFilesPaged = getCellFilesPaged,
            getOwners = getOwners,
            getPaginatedConversations = getPaginatedConversations,
            sharedPathCache = sharedPathCache,
            offlineDownloads = offlineDownloads,
            observeOfflineFiles = observeOfflineFiles,
            nodeChangeEvents = nodeChangeEvents,
            nodesSource = nodesSource(),
            networkStateObserver = networkStateObserver,
            featureFlags = featureFlags,
        )
        advanceUntilIdle()

        assertEquals(expectedRoute, viewModel.parentRoute)
    }

    @Test
    fun `given ViewModel, when onSearchQueryChanged is called, then no exception is thrown`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSearchQueryChanged("test query")
        advanceUntilIdle()

        // No exception means the query was successfully updated in the internal queryFlow
    }

    @Test
    fun `given tags in state, when onSaveTags is called, then tags are marked as selected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val tagsToSelect = listOf(
            FilterTagUi(id = "tag1", name = "tag1", selected = true),
            FilterTagUi(id = "tag2", name = "tag2", selected = true)
        )

        viewModel.onSaveTags(tagsToSelect)
        advanceUntilIdle()

        assertEquals(2, viewModel.uiState.value.tagsCount)
    }

    @Test
    fun `given selected tags, when onRemoveAllTags is called, then all tags are deselected`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveTags(listOf(FilterTagUi(id = "tag1", name = "tag1", selected = true)))
        advanceUntilIdle()

        viewModel.onRemoveAllTags()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.tagsCount)
        assertFalse(viewModel.uiState.value.availableTags.any { it.selected })
    }

    @Test
    fun `given state, when onSharedByMeClicked is called, then filesWithPublicLink is toggled`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSharedByMeClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filesWithPublicLink)

        viewModel.onSharedByMeClicked()
        advanceUntilIdle()
        assertFalse(viewModel.uiState.value.filesWithPublicLink)
    }

    @Test
    fun `given filters applied, when onRemoveAllFilters is called, then all filters are cleared`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveTags(listOf(FilterTagUi(id = "tag1", name = "tag1", selected = true)))
        advanceUntilIdle()

        viewModel.onSharedByMeClicked()
        advanceUntilIdle()

        viewModel.onRemoveAllFilters()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.tagsCount)
        assertFalse(viewModel.uiState.value.filesWithPublicLink)
    }

    @Test
    fun `given state, when setSortBy is called with new sort criteria, then sorting is updated`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.setSortBy(SortBy.Name)
        advanceUntilIdle()

        assertEquals(SortBy.Name, viewModel.uiState.value.sortingCriteria.by)
    }

    @Test
    fun `given state, when setSortBy is called with the same sort criteria, then state is not changed`() = runTest {
        // DRIVE starts with ByDate.NewestFirst (by = Modified); calling setSortBy(Modified) should be a no-op
        val viewModel = createViewModelWithScreenType(DriveSearchScreenType.DRIVE)
        advanceUntilIdle()

        val initialSortingCriteria = viewModel.uiState.value.sortingCriteria

        viewModel.setSortBy(SortBy.Modified)
        advanceUntilIdle()

        assertEquals(initialSortingCriteria, viewModel.uiState.value.sortingCriteria)
    }

    @Test
    fun `given state, when setSorting is called, then sorting criteria is updated`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val newCriteria = SortingCriteria.ByName.ZtoA

        viewModel.setSorting(newCriteria)
        advanceUntilIdle()

        assertEquals(newCriteria, viewModel.uiState.value.sortingCriteria)
    }

    @Test
    fun givenState_whenOnRemoveTypeFilterIsCalled_thenAllTypesAreDeselected() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onRemoveTypeFilter()
        advanceUntilIdle()

        assertEquals(0, viewModel.uiState.value.typeCount)
    }

    @Test
    fun givenNoConversationSelected_whenOnSaveConversation_thenSelectedConversationIsStored() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val conversation = FilterConversationUi(
            id = ConversationId("conv1", "domain"),
            name = "Engineering",
        )

        viewModel.onSaveConversation(conversation)
        advanceUntilIdle()

        assertEquals(conversation, viewModel.uiState.value.selectedConversation)
        assertEquals(1, viewModel.uiState.value.conversationCount)
    }

    @Test
    fun givenConversationSelected_whenOnRemoveConversations_thenSelectedConversationIsNull() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveConversation(
            FilterConversationUi(id = ConversationId("conv1", "domain"), name = "Engineering")
        )
        advanceUntilIdle()

        viewModel.onRemoveConversations()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedConversation)
        assertEquals(0, viewModel.uiState.value.conversationCount)
    }

    @Test
    fun givenConversationSelected_whenOnRemoveAllFilters_thenConversationFilterIsCleared() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onSaveConversation(
            FilterConversationUi(id = ConversationId("conv1", "domain"), name = "Engineering")
        )
        advanceUntilIdle()

        viewModel.onRemoveAllFilters()
        advanceUntilIdle()

        assertNull(viewModel.uiState.value.selectedConversation)
    }

    @Test
    fun givenNoQuery_whenOnConversationSearchQueryChanged_thenConversationsFlowIsNotNull() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.onConversationSearchQueryChanged("Engineering")
        advanceUntilIdle()

        // conversationsFlow is a valid non-null property
        assertNotNull(viewModel.conversationsFlow)
    }

    @Test
    fun givenDriveSearchWithoutAnyFilter_whenFilesFlowIsCollected_thenNoFilesAreRequested() = runTest {
        withCollectableFilesFlow()
        val viewModel = createDriveViewModel()

        viewModel.cellNodesFlow.first()

        coVerify(exactly = 0) { getCellFilesPaged(any(), any(), any(), any()) }
    }

    @Test
    fun givenDriveSearch_whenConversationFilterIsApplied_thenFilesAreRequestedForThatConversation() = runTest {
        withCollectableFilesFlow()
        val viewModel = createDriveViewModel()
        val conversation = FilterConversationUi(
            id = ConversationId("conv1", "domain"),
            name = "Engineering",
        )

        viewModel.onSaveConversation(conversation)
        advanceUntilIdle()

        viewModel.cellNodesFlow.first()

        coVerify(exactly = 1) { getCellFilesPaged(conversation.id.toString(), "", any(), any()) }
    }

    @Test
    fun givenResultRenamedLocally_whenResultsAreFetchedAgain_thenTheServerNameIsShownNotTheLocalOne() = runTest {
        every { observeOfflineFiles() } returns flowOf(emptyList())
        coEvery { getCellFilesPaged(any(), any(), any(), any(), any()) } returnsMany listOf(
            flowOf(pageOf(fileNamed("name.txt"))),
            // Renamed again elsewhere since.
            flowOf(pageOf(fileNamed("other_name.txt"))),
        )
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.cellNodesFlow.collect {} }
        viewModel.onSearchQueryChanged("name")
        advanceUntilIdle()
        flowOf(viewModel.cellNodesFlow.first()).asSnapshot()

        nodeChangeEvents.onNodeUpdated(CellNodeUpdate.Renamed(FILE_UUID, name = "my_name.txt", remotePath = "my_name.txt"))
        advanceUntilIdle()

        // Made while these results were the latest: they can't have it yet.
        assertEquals("my_name.txt", flowOf(viewModel.cellNodesFlow.first()).asSnapshot().single().name)

        viewModel.onSearchQueryChanged("na")
        advanceUntilIdle()

        // Fetched after the rename: the server already has it, and more recent changes too.
        assertEquals("other_name.txt", flowOf(viewModel.cellNodesFlow.first()).asSnapshot().single().name)
    }

    @Test
    fun givenOpenedFromOfflineOnlyListing_whenSearching_thenMatchingSavedFilesAreShownWithoutAskingTheServer() = runTest {
        every { observeOfflineFiles() } returns flowOf(
            listOf(
                offlineFile("1", "$CONVERSATION_ID/report.pdf"),
                offlineFile("2", "$CONVERSATION_ID/folder/report-2.pdf"),
                offlineFile("3", "$CONVERSATION_ID/photo.png"),
                offlineFile("4", "otherConversation/report-3.pdf"),
            )
        )
        val viewModel = createViewModel(SearchNavArgs(CONVERSATION_ID, DriveSearchScreenType.SHARED_DRIVE, isOfflineOnly = true))
        backgroundScope.launch { viewModel.cellNodesFlow.collect {} }

        viewModel.onSearchQueryChanged("REPORT")
        advanceUntilIdle()

        val names = flowOf(viewModel.cellNodesFlow.first()).asSnapshot().map { it.name }
        assertEquals(listOf("report-2.pdf", "report.pdf"), names)
        coVerify(exactly = 0) { getCellFilesPaged(any(), any(), any(), any(), any()) }
    }

    @Test
    fun givenSearchingOffline_whenBackOnline_thenTheServerIsSearched() = runTest {
        networkState.value = NetworkState.NotConnected
        every { observeOfflineFiles() } returns flowOf(listOf(offlineFile("1", "$CONVERSATION_ID/report.pdf")))
        coEvery { getCellFilesPaged(any(), any(), any(), any(), any()) } returns emptyFlow()
        val viewModel = createViewModel()
        backgroundScope.launch { viewModel.cellNodesFlow.collect {} }
        viewModel.onSearchQueryChanged("report")
        advanceUntilIdle()
        assertTrue(viewModel.isOfflineSearch.value)
        assertEquals(listOf("report.pdf"), flowOf(viewModel.cellNodesFlow.first()).asSnapshot().map { it.name })
        coVerify(exactly = 0) { getCellFilesPaged(any(), any(), any(), any(), any()) }

        networkState.value = NetworkState.ConnectedWithInternet
        advanceUntilIdle()

        assertFalse(viewModel.isOfflineSearch.value)
        coVerify(exactly = 1) { getCellFilesPaged(CONVERSATION_ID, "report", any(), any(), any()) }
    }

    @Test
    fun givenOpenedFromOfflineOnlyListing_whenBackOnline_thenSavedFilesAreStillSearched() = runTest {
        networkState.value = NetworkState.NotConnected
        val viewModel = createViewModel(SearchNavArgs(CONVERSATION_ID, DriveSearchScreenType.SHARED_DRIVE, isOfflineOnly = true))
        backgroundScope.launch { viewModel.isOfflineSearch.collect {} }

        networkState.value = NetworkState.ConnectedWithInternet
        advanceUntilIdle()

        assertTrue(viewModel.isOfflineSearch.value)
    }

    @Test
    fun givenPublicLinkFilterSelected_whenSearchingOffline_thenItIsCleared() = runTest {
        val viewModel = createViewModel()
        viewModel.onSharedByMeClicked()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.filesWithPublicLink)

        networkState.value = NetworkState.NotConnected
        advanceUntilIdle()

        assertFalse(viewModel.uiState.value.filesWithPublicLink)
    }

    private fun offlineFile(id: String, remotePath: String) = OfflineFileInfo(
        id = id,
        conversationId = remotePath.substringBefore('/'),
        name = remotePath.substringAfterLast('/'),
        mimeType = "application/pdf",
        owner = "owner",
        localPath = "/local/$id",
        size = 1024,
        downloadedAt = 0L,
        remotePath = remotePath,
    )

    private fun createViewModel(navArgs: SearchNavArgs): SearchScreenViewModel = SearchScreenViewModel(
        navArgs = navArgs,
        getAllTagsUseCase = getAllTagsUseCase,
        getCellFilesPaged = getCellFilesPaged,
        getOwners = getOwners,
        getPaginatedConversations = getPaginatedConversations,
        sharedPathCache = sharedPathCache,
        offlineDownloads = offlineDownloads,
        observeOfflineFiles = observeOfflineFiles,
        nodeChangeEvents = nodeChangeEvents,
        nodesSource = nodesSource(),
        networkStateObserver = networkStateObserver,
        featureFlags = featureFlags,
    )

    private fun fileNamed(name: String) = Node.File(
        uuid = FILE_UUID,
        conversationId = CONVERSATION_ID,
        versionId = "versionId",
        name = name,
        mimeType = "text/plain",
        remotePath = name,
        localPath = null,
        contentUrl = null,
        size = 1024,
        modifiedTime = 1234567890L,
    )

    private fun pageOf(vararg nodes: Node) = PagingData.from(
        data = nodes.toList(),
        sourceLoadStates = LoadStates(
            prepend = LoadState.NotLoading(true),
            append = LoadState.NotLoading(true),
            refresh = LoadState.NotLoading(true),
        ),
    )

    /**
     * [SearchScreenViewModel.cellNodesFlow] combines the paged files with the offline files flow,
     * so both have to emit for the combined flow to produce a value that can be collected.
     */
    private fun withCollectableFilesFlow() {
        every { observeOfflineFiles() } returns flowOf(emptyList())
        coEvery { getCellFilesPaged(any(), any(), any(), any()) } returns flowOf(
            PagingData.from(
                data = emptyList<Node>(),
                sourceLoadStates = LoadStates(
                    prepend = LoadState.NotLoading(true),
                    append = LoadState.NotLoading(true),
                    refresh = LoadState.NotLoading(true),
                ),
            )
        )
    }

    private fun createViewModel(): SearchScreenViewModel {
        return SearchScreenViewModel(
            navArgs = SearchNavArgs(CONVERSATION_ID, DriveSearchScreenType.SHARED_DRIVE),
            getAllTagsUseCase = getAllTagsUseCase,
            getCellFilesPaged = getCellFilesPaged,
            getOwners = getOwners,
            getPaginatedConversations = getPaginatedConversations,
            sharedPathCache = sharedPathCache,
            offlineDownloads = offlineDownloads,
            observeOfflineFiles = observeOfflineFiles,
            nodeChangeEvents = nodeChangeEvents,
            nodesSource = nodesSource(),
            networkStateObserver = networkStateObserver,
            featureFlags = featureFlags,
        )
    }

    private fun createViewModelWithInitialSorting(sortCriteria: SortCriteriaNavArg): SearchScreenViewModel {
        return SearchScreenViewModel(
            navArgs = SearchNavArgs(
                conversationId = CONVERSATION_ID,
                screenType = DriveSearchScreenType.SHARED_DRIVE,
                initialSortingCriteria = sortCriteria,
            ),
            getAllTagsUseCase = getAllTagsUseCase,
            getCellFilesPaged = getCellFilesPaged,
            getOwners = getOwners,
            getPaginatedConversations = getPaginatedConversations,
            sharedPathCache = sharedPathCache,
            offlineDownloads = offlineDownloads,
            observeOfflineFiles = observeOfflineFiles,
            nodeChangeEvents = nodeChangeEvents,
            nodesSource = nodesSource(),
            networkStateObserver = networkStateObserver,
            featureFlags = featureFlags,
        )
    }

    /** All-files search: no conversation in nav args, so the conversation filter is available. */
    private fun createDriveViewModel(): SearchScreenViewModel {
        return SearchScreenViewModel(
            navArgs = SearchNavArgs(conversationId = null, screenType = DriveSearchScreenType.DRIVE),
            getAllTagsUseCase = getAllTagsUseCase,
            getCellFilesPaged = getCellFilesPaged,
            getOwners = getOwners,
            getPaginatedConversations = getPaginatedConversations,
            sharedPathCache = sharedPathCache,
            offlineDownloads = offlineDownloads,
            observeOfflineFiles = observeOfflineFiles,
            nodeChangeEvents = nodeChangeEvents,
            nodesSource = nodesSource(),
            networkStateObserver = networkStateObserver,
            featureFlags = featureFlags,
        )
    }

    private fun createViewModelWithScreenType(screenType: DriveSearchScreenType): SearchScreenViewModel {
        return SearchScreenViewModel(
            navArgs = SearchNavArgs(CONVERSATION_ID, screenType),
            getAllTagsUseCase = getAllTagsUseCase,
            getCellFilesPaged = getCellFilesPaged,
            getOwners = getOwners,
            getPaginatedConversations = getPaginatedConversations,
            sharedPathCache = sharedPathCache,
            offlineDownloads = offlineDownloads,
            observeOfflineFiles = observeOfflineFiles,
            nodeChangeEvents = nodeChangeEvents,
            nodesSource = nodesSource(),
            networkStateObserver = networkStateObserver,
            featureFlags = featureFlags,
        )
    }
}
