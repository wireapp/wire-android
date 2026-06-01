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

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.LoadState
import androidx.paging.LoadStates
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.ramcosta.composedestinations.generated.cells.destinations.SearchScreenDestination
import com.wire.android.feature.cells.ui.CellFileLocalPathCache
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.ui.model.withOpenLoadState
import com.wire.android.feature.cells.ui.search.filter.data.FilterConversationUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterOwnerUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterTypeUi
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.feature.cells.ui.search.sort.toKaliumCriteria
import com.wire.android.model.ImageAsset
import com.wire.kalium.cells.data.FileFilters
import com.wire.kalium.cells.data.MIMEType
import com.wire.kalium.cells.data.SortingSpec
import com.wire.kalium.cells.domain.model.CellConversation
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCaseResult
import com.wire.kalium.cells.domain.usecase.GetPaginatedCellConversationsFlowUseCase
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.user.UserAssetId
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emitAll
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

private const val SEARCH_DEBOUNCE_MILLIS = 200L

@Suppress("TooManyFunctions")
class SearchScreenViewModel(
    val savedStateHandle: SavedStateHandle,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val getOwners: GetOwnersUseCase,
    private val getPaginatedConversations: GetPaginatedCellConversationsFlowUseCase,
    private val sharedPathCache: CellFileLocalPathCache,
) : ViewModel() {

    private data class SearchParams(
        val query: String,
        val tagIds: List<String>,
        val ownerIds: List<String>,
        val mimeTypes: List<MIMEType>,
        val filesWithPublicLink: Boolean?,
        val sortingCriteria: SortingCriteria,
        val conversationId: String?,
    )

    private val navArgs: SearchNavArgs = SearchScreenDestination.argsFrom(savedStateHandle)

    val screenType = navArgs.screenType
    val parentRoute = navArgs.parentRoute

    /**
     * The default (no-filter) sorting for this screen type.
     * DRIVE (all-files) defaults to newest-first; SHARED_DRIVE (conversation) defaults to folders-first.
     */
    val defaultSortingCriteria: SortingCriteria = if (screenType == DriveSearchScreenType.DRIVE) {
        SortingCriteria.ByDate.NewestFirst
    } else {
        SortingCriteria.FoldersFirst
    }

    private val _uiState = MutableStateFlow(
        SearchUiState(sortingCriteria = defaultSortingCriteria)
    )
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    private val debouncedQueryFlow: Flow<String> = queryFlow
        .debounce(SEARCH_DEBOUNCE_MILLIS)
        .distinctUntilChanged()

    private val searchParamsFlow: Flow<SearchParams> =
        combine(
            debouncedQueryFlow,
            uiState,
        ) { query, state ->
            val selectedConversationId = state.selectedConversation?.id?.toString()
            SearchParams(
                query = query,
                tagIds = state.availableTags.filter { it.selected }.map { it.id },
                ownerIds = state.availableOwners.filter { it.selected }.map { it.id },
                mimeTypes = state.availableTypes.filter { it.selected }.map { it.mimeType },
                filesWithPublicLink = state.filesWithPublicLink,
                sortingCriteria = state.sortingCriteria,
                conversationId = selectedConversationId ?: navArgs.conversationId,
            )
        }.distinctUntilChanged()

    val cellNodesFlow: Flow<PagingData<CellNodeUi>> =
        combine(
            searchParamsFlow.flatMapLatest<SearchParams, PagingData<CellNodeUi>> { params: SearchParams ->
                val hasFilters = params.sortingCriteria != defaultSortingCriteria ||
                        params.query.isNotEmpty() ||
                        params.tagIds.isNotEmpty() ||
                        params.ownerIds.isNotEmpty() ||
                        params.mimeTypes.isNotEmpty() ||
                        params.filesWithPublicLink == true

                if (!hasFilters) {
                    return@flatMapLatest kotlinx.coroutines.flow.flowOf(
                        PagingData.empty(
                            LoadStates(
                                refresh = LoadState.Loading,
                                prepend = LoadState.NotLoading(true),
                                append = LoadState.NotLoading(true),
                            )
                        )
                    )
                }

                getCellFilesPaged(
                    conversationId = params.conversationId,
                    query = params.query,
                    fileFilters = FileFilters(
                        tags = params.tagIds,
                        owners = params.ownerIds,
                        mimeTypes = params.mimeTypes,
                        hasPublicLink = params.filesWithPublicLink,
                    ),
                    sortingSpec = SortingSpec(
                        criteria = params.sortingCriteria.toKaliumCriteria(),
                        descending = params.sortingCriteria.isDescending
                    )
                ).map { pagingData: PagingData<Node> ->
                    pagingData.map { node: Node ->
                        when (node) {
                            is Node.Folder -> node.toUiModel()
                            is Node.File -> node.toUiModel()
                        }
                    }
                }
            }.cachedIn(viewModelScope),
            sharedPathCache.openLoadStates,
        ) { pagingData, states ->
            pagingData.map { node ->
                if (node is CellNodeUi.File) {
                    node.withOpenLoadState(states[node.uuid])
                } else {
                    node
                }
            }
        }

    private val _conversationSearchQuery = MutableStateFlow("")

    val conversationsFlow: Flow<PagingData<FilterConversationUi>> = flow {
        if (screenType == DriveSearchScreenType.DRIVE) {
            emitAll(
                _conversationSearchQuery
                    .flatMapLatest { query ->
                        getPaginatedConversations(query)
                            .map { pagingData -> pagingData.map { it.toFilterConversationUi() } }
                    }
            )
        }
    }.cachedIn(viewModelScope)

    fun onConversationSearchQueryChanged(query: String) {
        _conversationSearchQuery.value = query
    }

    init {
        loadTags()
        loadOwners()
    }

    internal fun loadTags() = viewModelScope.launch {
        getAllTagsUseCase().onSuccess { updated ->
            _uiState.update {
                it.copy(
                    availableTags = updated.map { tag ->
                        FilterTagUi(
                            id = tag,
                            name = tag,
                        )
                    }
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        queryFlow.value = query
    }

    fun onSetSearchActive(active: Boolean) {
        _uiState.update { it.copy(isSearchActive = active) }
    }

    internal fun loadOwners(conversationId: String? = navArgs.conversationId) {
        viewModelScope.launch {
            when (val result = getOwners(conversationId = conversationId)) {
                is GetOwnersUseCaseResult.Success -> {
                    val ownersUi = result.owners.mapNotNull { owner ->
                        val name = owner.name?.takeIf { it.isNotBlank() }
                        val handle = owner.handle?.takeIf { it.isNotBlank() }
                        if (name == null || handle == null) return@mapNotNull null

                        val picture = owner.completePicture ?: owner.previewPicture
                        val avatarAsset = picture?.let { pic ->
                            ImageAsset.UserAvatarAsset(
                                UserAssetId(
                                    value = pic.value,
                                    domain = pic.domain
                                )
                            )
                        }

                        FilterOwnerUi(
                            id = owner.id.toString(),
                            displayName = name,
                            handle = handle,
                            userAvatarAsset = avatarAsset,
                            selected = false
                        )
                    }.sortedBy { it.displayName.uppercase() }

                    _uiState.update { state ->
                        state.copy(availableOwners = ownersUi)
                    }
                }

                is GetOwnersUseCaseResult.Failure -> {}
            }
        }
    }

    fun onSaveConversation(selectedConversation: FilterConversationUi?) {
        _uiState.update { state ->
            state.copy(selectedConversation = selectedConversation)
        }
    }

    fun onRemoveConversations() {
        _uiState.update { state ->
            state.copy(selectedConversation = null)
        }
    }

    private fun applySelectedTags(selectedIds: Set<String>) {
        _uiState.update { state ->
            state.copy(
                availableTags = state.availableTags.map { tag ->
                    tag.copy(selected = tag.id in selectedIds)
                }
            )
        }
    }

    fun onSaveTags(selectedTags: List<FilterTagUi>) {
        applySelectedTags(selectedTags.filter { it.selected }.map { it.id }.toSet())
    }

    fun onRemoveAllTags() {
        _uiState.update { state ->
            state.copy(availableTags = state.availableTags.map { it.copy(selected = false) })
        }
    }

    private fun applySelectedTypes(selectedIds: Set<String>) {
        _uiState.update { state ->
            state.copy(
                availableTypes = state.availableTypes.map { tag ->
                    tag.copy(selected = tag.id in selectedIds)
                }
            )
        }
    }

    fun onSaveTypes(selectedOwners: List<FilterTypeUi>) {
        applySelectedTypes(selectedOwners.filter { it.selected }.map { it.id }.toSet())
    }

    fun onRemoveTypeFilter() {
        _uiState.update { state ->
            state.copy(availableTypes = state.availableTypes.map { it.copy(selected = false) })
        }
    }

    fun onSharedByMeClicked() {
        _uiState.update { it.copy(filesWithPublicLink = !it.filesWithPublicLink) }
    }

    private fun applySelectedOwners(selectedIds: Set<String>) {
        _uiState.update { state ->
            state.copy(
                availableOwners = state.availableOwners.map { tag ->
                    tag.copy(selected = tag.id in selectedIds)
                }
            )
        }
    }

    fun onSaveOwners(selectedOwners: List<FilterOwnerUi>) {
        applySelectedOwners(selectedOwners.filter { it.selected }.map { it.id }.toSet())
    }

    fun onRemoveOwners() {
        _uiState.update { state ->
            state.copy(availableOwners = state.availableOwners.map { it.copy(selected = false) })
        }
    }

    fun onRemoveAllFilters() {
        onRemoveAllTags()
        onRemoveOwners()
        onRemoveTypeFilter()
        onRemoveConversations()
        _uiState.update {
            it.copy(filesWithPublicLink = false)
        }
    }

    fun setSortBy(by: SortBy) {
        _uiState.update { current ->
            val currentCriteria = current.sortingCriteria
            if (currentCriteria.by == by) {
                current
            } else {
                current.copy(sortingCriteria = defaultCriteriaFor(by))
            }
        }
    }

    fun setSorting(criteria: SortingCriteria) {
        _uiState.update { current ->
            current.copy(sortingCriteria = criteria)
        }
    }
}

private fun CellConversation.toFilterConversationUi() = FilterConversationUi(
    id = id,
    name = name,
    isChannel = isChannel,
    isPrivateChannel = channelAccess == ConversationDetails.Group.Channel.ChannelAccess.PRIVATE,
)

fun defaultCriteriaFor(by: SortBy): SortingCriteria = when (by) {
    SortBy.Default -> SortingCriteria.FoldersFirst
    SortBy.Modified -> SortingCriteria.ByDate.NewestFirst
    SortBy.Name -> SortingCriteria.ByName.AtoZ
    SortBy.Size -> SortingCriteria.BySize.SmallestFirst
}
