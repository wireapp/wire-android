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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
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

    val isLoading = MutableStateFlow(false)

    val tagsTextState = TextFieldState()

    private val allTags: MutableStateFlow<Set<String>> = MutableStateFlow(emptySet())

    val initialTags: Set<String> = navArgs.tags.toSet()
    private val _addedTags: MutableStateFlow<Set<String>> = MutableStateFlow(navArgs.tags.toSet())
    internal val addedTags = _addedTags.asStateFlow()

    val disallowedChars = listOf(",", ";", "/", "\\", "\"", "\'", "<", ">")

    @Suppress("MagicNumber")
    val allowedLength = 1..30

    internal val suggestedTags =
        allTags.combine(addedTags) { all, added ->
            all.filter { it !in added }.toSet()
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptySet())

    init {
        viewModelScope.launch {
            getAllTagsUseCase().onSuccess { tags ->
                allTags.update { tags }
            }
        }
    }

    fun isValidTag(): Boolean = disallowedChars.none {
        it in tagsTextState.text
    } && tagsTextState.text.length in allowedLength

    fun addTag(tag: String) {
        tag.trim().let { newTag ->
            if (newTag.isNotBlank() && newTag !in _addedTags.value) {
                _addedTags.update { it + newTag }
                tagsTextState.clearText()
            }
        }
    }

    fun removeTag(tag: String) {
        _addedTags.update { it - tag }
    }

    fun removeLastTag() {
        _addedTags.value.lastOrNull()?.let { lastTag ->
            removeTag(lastTag)
        }
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
