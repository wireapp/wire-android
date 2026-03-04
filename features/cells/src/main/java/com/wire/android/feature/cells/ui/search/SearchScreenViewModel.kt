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
import androidx.paging.PagingData
import androidx.paging.cachedIn
import androidx.paging.map
import com.ramcosta.composedestinations.generated.cells.destinations.SearchScreenDestination
import com.wire.android.feature.cells.ui.model.CellNodeUi
import com.wire.android.feature.cells.ui.model.toUiModel
import com.wire.android.feature.cells.ui.search.filter.data.FilterConversationUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterOwnerUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterTagUi
import com.wire.android.feature.cells.ui.search.filter.data.FilterTypeUi
import com.wire.android.feature.cells.ui.search.sort.SortBy
import com.wire.android.feature.cells.ui.search.sort.SortingCriteria
import com.wire.android.model.ImageAsset
import com.wire.kalium.cells.data.FileFilters
import com.wire.kalium.cells.data.MIMEType
import com.wire.kalium.cells.data.SortingSpec
import com.wire.kalium.cells.domain.model.Node
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.GetCellGroupConversationsUseCase
import com.wire.kalium.cells.domain.usecase.GetConversationsUseCaseResult
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCase
import com.wire.kalium.cells.domain.usecase.GetOwnersUseCaseResult
import com.wire.kalium.cells.domain.usecase.GetPaginatedFilesFlowUseCase
import com.wire.kalium.common.functional.onSuccess
import com.wire.kalium.logic.data.conversation.ConversationDetails
import com.wire.kalium.logic.data.user.UserAssetId
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// TODO: to cover it with  unit test in upcoming PR
// TODO add search debounce to avoid triggering search on every keystroke
@Suppress("TooManyFunctions")
@HiltViewModel
class SearchScreenViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val getCellFilesPaged: GetPaginatedFilesFlowUseCase,
    private val getOwners: GetOwnersUseCase,
    private val getCellGroupConversations: GetCellGroupConversationsUseCase,
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

    private val _uiState = MutableStateFlow(SearchUiState())
    val uiState: StateFlow<SearchUiState> = _uiState.asStateFlow()

    private val queryFlow = MutableStateFlow("")

    private val searchParamsFlow: Flow<SearchParams> =
        combine(
            queryFlow,
            uiState,
        ) { query, state ->
            val selectedConversationId = state.availableConversations.firstOrNull { it.selected }?.id?.toString()
            SearchParams(
                query = query,
                tagIds = state.availableTags.filter { it.selected }.map { it.id },
                ownerIds = state.availableOwners.filter { it.selected }.map { it.id },
                mimeTypes = state.availableTypes.filter { it.selected }.map { it.mimeType },
                filesWithPublicLink = state.filesWithPublicLink,
                sortingCriteria = state.sortingCriteria,
                conversationId = selectedConversationId ?: navArgs.conversationId,
            )
        }
            .distinctUntilChanged()

    val cellNodesFlow: Flow<PagingData<CellNodeUi>> =
        searchParamsFlow.flatMapLatest<SearchParams, PagingData<CellNodeUi>> { params: SearchParams ->
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
        }.cachedIn(viewModelScope)

    init {
        loadTags()
        loadOwners()
        if (screenType == DriveScreenType.DRIVE) {
            loadConversations()
        }
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

    internal fun loadConversations() = viewModelScope.launch {
        val result = getCellGroupConversations()
        if (result is GetConversationsUseCaseResult.Success) {
            _uiState.update {
                it.copy(
                    availableConversations = result.conversations.map { conversation ->
                        FilterConversationUi(
                            id = conversation.id,
                            name = conversation.name,
                            isChannel = conversation.isChannel,
                            isPrivateChannel = conversation.channelAccess == ConversationDetails.Group.Channel.ChannelAccess.PRIVATE,
                        )
                    }.sortedBy { it.name.uppercase() }
                )
            }
        }
    }

    fun onSearchQueryChanged(query: String) {
        queryFlow.value = query
    }

    fun loadOwners(conversationId: String? = navArgs.conversationId) {
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
                            id = owner.id.value,
                            displayName = name,
                            handle = handle,
                            userAvatarAsset = avatarAsset,
                            selected = false
                        )
                    }
                        .sortedBy { it.displayName.uppercase() }

                    _uiState.update { state ->
                        state.copy(
                            availableOwners = ownersUi
                        )
                    }
                }

                is GetOwnersUseCaseResult.Failure -> {
                    // no need to show error, just keep the owners list empty
                }
            }
        }
    }

    private fun applySelectedConversations(selectedId: String?) {
        _uiState.update { state ->
            state.copy(
                availableConversations = state.availableConversations.map { conversation ->
                    conversation.copy(selected = conversation.id.toString() == selectedId)
                }
            )
        }
    }

    fun onSaveConversations(selectedConversations: List<FilterConversationUi>) {
        val selectedId = selectedConversations.firstOrNull { it.selected }?.id?.toString()
        applySelectedConversations(selectedId)
    }

    fun onRemoveConversations() {
        _uiState.update { state ->
            state.copy(availableConversations = state.availableConversations.map { it.copy(selected = false) })
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

    fun onRemoveAllFilters() = _uiState.update {
        onRemoveAllTags()
        onRemoveOwners()
        onRemoveTypeFilter()
        onRemoveConversations()
        it.copy(filesWithPublicLink = false)
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

fun defaultCriteriaFor(by: SortBy): SortingCriteria = when (by) {
    SortBy.Modified -> SortingCriteria.Modified.NewestFirst
    SortBy.Name -> SortingCriteria.Name.AtoZ
    SortBy.Size -> SortingCriteria.Size.SmallestFirst
}

fun SortingCriteria.toKaliumCriteria(): com.wire.kalium.cells.data.SortingCriteria =
    when (by) {
        SortBy.Modified ->
            com.wire.kalium.cells.data.SortingCriteria.MODIFICATION_TIME

        SortBy.Name ->
            com.wire.kalium.cells.data.SortingCriteria.NAME_CASE_INSENSITIVE

        SortBy.Size ->
            com.wire.kalium.cells.data.SortingCriteria.SIZE
    }
