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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wire.kalium.cells.domain.usecase.GetAllTagsUseCase
import com.wire.kalium.common.functional.getOrElse
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AddRemoveTagsViewModel @Inject constructor(
    val savedStateHandle: SavedStateHandle,
    private val getAllTagsUseCase: GetAllTagsUseCase,
) : ViewModel() {

    val tagsTextState = TextFieldState()

    private val _addedTags: MutableStateFlow<List<String>> = MutableStateFlow(listOf("Android", "Kotlin", "Compose"))
    internal val addedTags = _addedTags.asStateFlow()

    private val _suggestedTags: MutableStateFlow<List<String>> = MutableStateFlow(listOf())
    internal val suggestedTags = _suggestedTags.asStateFlow()

    init {
        viewModelScope.launch {
            _suggestedTags.value = getAllTagsUseCase().getOrElse(emptyList())
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
}
