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
package com.wire.android.feature.cells.ui.tags

import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.UpdateNodeTagsUseCase
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddRemoveTagsViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getAllTagsUseCase: GetAllTagsUseCase,
    private val updateNodeTagsUseCase: UpdateNodeTagsUseCase,
    private val removeNodeTagsUseCase: RemoveNodeTagsUseCase,
) : ActionsViewModel<AddRemoveTagsViewModelAction>() {

    private val navArgs: AddRemoveTagsNavArgs = savedStateHandle.navArgs()
    private val initialTags: Set<String> = navArgs.tags.toSet()
    private val disallowedChars = listOf(",", ";", "/", "\\", "\"", "\'", "<", ">")

    private val _state = MutableStateFlow(TagsViewState(addedTags = initialTags))
    val state = _state.asStateFlow()

    val tagsTextState = TextFieldState()

    init {
        viewModelScope.launch {
            getAllTagsUseCase().onSuccess { tags ->
                _state.update { it.copy(allTags = tags) }
            }
            launch {
                snapshotFlow { tagsTextState.text.toString() }
                    .debounce(TYPING_DEBOUNCE_TIME)
                    .collectLatest { updateViewState() }
            }
        }
    }

    fun isValidTag(): Boolean = with(tagsTextState) {
        disallowedChars.none { it in text } && text.length in ALLOWED_LENGTH
    }

    fun addTag(tag: String) {
        tag.trim().let { newTag ->
            val addedTags = state.value.addedTags
            if (newTag.isNotBlank() && newTag !in addedTags) {
                updateViewState(addedTags + tag)
                tagsTextState.clearText()
            }
        }
    }

    fun removeTag(tag: String) {
        updateViewState(state.value.addedTags - tag)
    }

    fun removeLastTag() {
        state.value.addedTags.lastOrNull()?.let { removeTag(it) }
    }

    fun updateTags() = viewModelScope.launch {
        _state.update { it.copy(isLoading = true) }

        if (state.value.addedTags.isEmpty()) {
            removeNodeTagsUseCase(navArgs.uuid)
        } else {
            updateNodeTagsUseCase(navArgs.uuid, state.value.addedTags)
        }
            .onSuccess { sendAction(AddRemoveTagsViewModelAction.Success) }
            .onFailure { sendAction(AddRemoveTagsViewModelAction.Failure) }
            .also { _state.update { it.copy(isLoading = false) } }
    }

    fun updateViewState() {
        updateViewState(state.value.addedTags)
    }

    fun updateViewState(addedTags: Set<String>) {
        _state.update { current ->
            current.copy(
                addedTags = addedTags,
                suggestedTags = current.allTags
                    .filter { it !in addedTags }
                    .filter { it.contains(tagsTextState.text.toString(), ignoreCase = true) }
                    .toSet(),
                tagsUpdated = addedTags != initialTags
            )
        }
    }

    private companion object {
        val ALLOWED_LENGTH = 1..30
        const val TYPING_DEBOUNCE_TIME = 200L
    }
}

data class TagsViewState(
    val isLoading: Boolean = false,
    val tagsUpdated: Boolean = false,
    val addedTags: Set<String> = emptySet(),
    val suggestedTags: Set<String> = emptySet(),
    val allTags: Set<String> = emptySet(),
)

sealed interface AddRemoveTagsViewModelAction {
    data object Success : AddRemoveTagsViewModelAction
    data object Failure : AddRemoveTagsViewModelAction
}
