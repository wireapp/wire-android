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
import androidx.compose.foundation.text.input.delete
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.viewModelScope
import com.wire.android.feature.cells.ui.navArgs
import com.wire.android.ui.common.ActionsViewModel
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.cells.domain.usecase.RemoveNodeTagsUseCase
import com.wire.kalium.cells.domain.usecase.UpdateNodeTagsUseCase
import com.wire.kalium.common.functional.getOrElse
import com.wire.kalium.common.functional.onFailure
import com.wire.kalium.common.functional.onSuccess
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
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

    val isLoading = MutableStateFlow(false)

    val tagsTextState = TextFieldState()

    private val _addedTags: MutableStateFlow<List<String>> = MutableStateFlow(navArgs.tags)
    internal val addedTags = _addedTags.asStateFlow()

    private val _suggestedTags: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    internal val suggestedTags = _suggestedTags.asStateFlow()

    init {
        viewModelScope.launch {
            val allTags = getAllTagsUseCase().getOrElse(emptyList())
            _suggestedTags.value = allTags.filterNot { it in _addedTags.value }
        }
    }

    fun addTag(tag: String) {
        if (tag.isNotBlank() && tag !in _addedTags.value) {
            _addedTags.value += tag
            _suggestedTags.value = _suggestedTags.value.filter { it != tag }
            tagsTextState.edit {
                delete(0, length)
            }
        }
    }

    fun removeTag(tag: String) {
        _addedTags.value = _addedTags.value.filter { it != tag }
    }

    fun updateTags() {
        viewModelScope.launch {
            isLoading.value = true
            val result = if (_addedTags.value.isEmpty()) {
                removeNodeTagsUseCase(navArgs.uuid)
            } else {
                updateNodeTagsUseCase(navArgs.uuid, _addedTags.value)
            }

            result
                .onSuccess { sendAction(AddRemoveTagsViewModelAction.Success) }
                .onFailure { sendAction(AddRemoveTagsViewModelAction.Failure) }
                .also { isLoading.value = false }
        }
    }
}

sealed interface AddRemoveTagsViewModelAction {
    data object Success : AddRemoveTagsViewModelAction
    data object Failure : AddRemoveTagsViewModelAction
}
