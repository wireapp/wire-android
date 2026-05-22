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

package com.wire.android.ui.common.topappbar.search

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.common.R
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.onEscapeOrBackKey
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun SearchTopBar(
    isSearchActive: Boolean,
    searchBarHint: String,
    searchQueryTextState: TextFieldState,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    shouldClearTextOnClearFocus: Boolean = true,
    keepBackButtonVisible: Boolean = false,
    backIconContentDescription: String? = null,
    searchBarDescription: String? = null,
    onCloseSearchClicked: (() -> Unit)? = null,
    onActiveChanged: (isActive: Boolean) -> Unit = {},
    externalFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    bottomContent: @Composable ColumnScope.() -> Unit = {},
    textFieldState: WireTextFieldState = WireTextFieldState.Default,
    onTap: (() -> Unit)? = null,
) {
    val interactionSource = remember { MutableInteractionSource() }
    val keyboardController = LocalSoftwareKeyboardController.current
    val localFocusRequester = remember { FocusRequester() }
    val focusRequester = resolveFocusRequester(externalFocusRequester, localFocusRequester)
    val backButtonFocusRequester = remember { FocusRequester() }
    val clearButtonFocusRequester = remember { FocusRequester() }
    val hasSearchQuery by remember { derivedStateOf { searchQueryTextState.text.isNotBlank() } }
    val resolvedBackIconContentDescription = resolveBackIconContentDescription(backIconContentDescription)

    fun showKeyboard() {
        keyboardController?.show()
    }

    fun clearSearchState() {
        if (shouldClearTextOnClearFocus) {
            searchQueryTextState.clearText()
        }
    }

    fun closeSearchInput() {
        keyboardController?.hide()
        clearSearchState()
        onCloseSearchClicked?.invoke() ?: onActiveChanged(false)
    }

    SearchFocusEffect(
        isSearchActive = isSearchActive,
        focusRequester = focusRequester,
        onShowKeyboard = ::showKeyboard,
        onClearSearchState = ::clearSearchState
    )

    val placeholderAlignment by animateHorizontalAlignmentAsState(
        targetAlignment = if (isSearchActive) Alignment.CenterStart else Alignment.Center
    )

    Column(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .background(MaterialTheme.wireColorScheme.background)
    ) {
        if (isSearchActive) {
            ActiveSearchBarInput(
                placeholderText = searchBarHint,
                semanticDescription = searchBarDescription,
                textState = searchQueryTextState,
                isLoading = isLoading,
                textFieldState = textFieldState,
                placeholderAlignment = placeholderAlignment,
                interactionSource = interactionSource,
                focusRequester = focusRequester,
                backButtonFocusRequester = backButtonFocusRequester,
                clearButtonFocusRequester = clearButtonFocusRequester,
                nextFocusRequester = nextFocusRequester,
                hasSearchQuery = hasSearchQuery,
                backIconContentDescription = resolvedBackIconContentDescription,
                onCloseSearchInput = ::closeSearchInput,
                onActiveChanged = onActiveChanged,
            )
        } else {
            InactiveSearchBarInput(
                placeholderText = searchBarHint,
                semanticDescription = searchBarDescription,
                textState = searchQueryTextState,
                isLoading = isLoading,
                textFieldState = textFieldState,
                placeholderAlignment = placeholderAlignment,
                interactionSource = interactionSource,
                focusRequester = focusRequester,
                keepBackButtonVisible = keepBackButtonVisible,
                backIconContentDescription = resolvedBackIconContentDescription,
                onCloseSearchInput = ::closeSearchInput,
                onTap = onTap,
                onActiveChanged = onActiveChanged,
                onShowKeyboard = ::showKeyboard
            )
        }
        bottomContent()
    }
}

private fun resolveFocusRequester(
    externalFocusRequester: FocusRequester?,
    localFocusRequester: FocusRequester,
): FocusRequester = externalFocusRequester ?: localFocusRequester

@Composable
private fun resolveBackIconContentDescription(backIconContentDescription: String?): String =
    backIconContentDescription ?: stringResource(R.string.content_description_back_button)

@Composable
private fun SearchFocusEffect(
    isSearchActive: Boolean,
    focusRequester: FocusRequester,
    onShowKeyboard: () -> Unit,
    onClearSearchState: () -> Unit,
) {
    LaunchedEffect(isSearchActive) {
        if (isSearchActive) {
            withFrameNanos { }
            focusRequester.requestFocus()
            onShowKeyboard()
        } else {
            onClearSearchState()
        }
    }
}

@Suppress("LongParameterList")
@Composable
private fun ActiveSearchBarInput(
    placeholderText: String,
    semanticDescription: String?,
    textState: TextFieldState,
    isLoading: Boolean,
    textFieldState: WireTextFieldState,
    placeholderAlignment: Alignment.Horizontal,
    interactionSource: MutableInteractionSource,
    focusRequester: FocusRequester,
    backButtonFocusRequester: FocusRequester,
    clearButtonFocusRequester: FocusRequester,
    nextFocusRequester: FocusRequester?,
    hasSearchQuery: Boolean,
    backIconContentDescription: String,
    onCloseSearchInput: () -> Unit,
    onActiveChanged: (Boolean) -> Unit,
) {
    SearchBarInput(
        placeholderText = placeholderText,
        semanticDescription = semanticDescription,
        textState = textState,
        isLoading = isLoading,
        textFieldState = textFieldState,
        leadingIcon = {
            SearchBackButton(
                focusRequester = backButtonFocusRequester,
                nextFocusRequester = focusRequester,
                contentDescription = backIconContentDescription,
                onClick = onCloseSearchInput
            )
        },
        placeholderTextStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
        placeholderAlignment = placeholderAlignment,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
        interactionSource = interactionSource,
        inputEnabled = true,
        clearButtonModifier = Modifier
            .focusRequester(clearButtonFocusRequester)
            .focusProperties {
                previous = focusRequester
                nextFocusRequester?.let { next = it }
            },
        onTap = null,
        modifier = Modifier
            .onEscapeOrBackKey(
                enabled = true,
                onKeyPressed = onCloseSearchInput
            )
            .padding(dimensions().spacing8x),
        inputModifier = Modifier
            .focusRequester(focusRequester)
            .focusProperties {
                previous = backButtonFocusRequester
                if (hasSearchQuery) {
                    next = clearButtonFocusRequester
                } else {
                    nextFocusRequester?.let { next = it }
                }
            }
            .onEscapeOrBackKey(
                enabled = true,
                onKeyPressed = onCloseSearchInput
            )
            .onFocusEvent {
                if (it.isFocused) {
                    onActiveChanged(true)
                }
            }
    )
}

@Composable
private fun InactiveSearchBarInput(
    placeholderText: String,
    semanticDescription: String?,
    textState: TextFieldState,
    isLoading: Boolean,
    textFieldState: WireTextFieldState,
    placeholderAlignment: Alignment.Horizontal,
    interactionSource: MutableInteractionSource,
    focusRequester: FocusRequester,
    keepBackButtonVisible: Boolean,
    backIconContentDescription: String,
    onCloseSearchInput: () -> Unit,
    onTap: (() -> Unit)?,
    onActiveChanged: (Boolean) -> Unit,
    onShowKeyboard: () -> Unit,
) {
    fun activateSearch() {
        onTap?.invoke()
        onActiveChanged(true)
        onShowKeyboard()
    }

    SearchBarInput(
        placeholderText = placeholderText,
        semanticDescription = semanticDescription,
        textState = textState,
        isLoading = isLoading,
        textFieldState = textFieldState,
        leadingIcon = {
            if (keepBackButtonVisible) {
                SearchBackButton(
                    focusRequester = null,
                    nextFocusRequester = null,
                    contentDescription = backIconContentDescription,
                    onClick = onCloseSearchInput
                )
            } else {
                SearchIcon()
            }
        },
        placeholderTextStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Center),
        placeholderAlignment = placeholderAlignment,
        textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
        interactionSource = interactionSource,
        inputEnabled = true,
        onTap = ::activateSearch,
        modifier = Modifier
            .padding(dimensions().spacing8x)
            .focusRequester(focusRequester)
            .onFocusChanged {
                if (it.isFocused) {
                    activateSearch()
                }
            }
            .semantics {
                semanticDescription?.let { contentDescription = it }
            }
            .focusable(),
        inputModifier = Modifier.onFocusEvent {
            if (it.isFocused) {
                activateSearch()
            }
        }
    )
}

@Composable
private fun SearchBackButton(
    focusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester?,
    contentDescription: String,
    onClick: () -> Unit,
) {
    IconButton(
        onClick = onClick,
        modifier = Modifier
            .size(dimensions().buttonCircleMinSize)
            .then(focusRequester?.let { Modifier.focusRequester(it) } ?: Modifier)
            .focusProperties {
                nextFocusRequester?.let { next = it }
            }
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_arrow_back),
            contentDescription = contentDescription,
            tint = MaterialTheme.wireColorScheme.onBackground,
        )
    }
}

@Composable
private fun SearchIcon() {
    Box(
        contentAlignment = Alignment.Center,
        modifier = Modifier.size(dimensions().buttonCircleMinSize)
    ) {
        Icon(
            painter = painterResource(R.drawable.ic_search),
            contentDescription = null,
            tint = MaterialTheme.wireColorScheme.onBackground,
        )
    }
}

@Composable
private fun animateHorizontalAlignmentAsState(
    targetAlignment: Alignment,
): State<BiasAlignment.Horizontal> {
    val biased = targetAlignment as BiasAlignment
    val bias by animateFloatAsState(biased.horizontalBias, label = "AnimateHorizontalAlignment")
    return remember { derivedStateOf { BiasAlignment.Horizontal(bias) } }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchTopBarActive() {
    WireTheme {
        SearchTopBar(
            isSearchActive = true,
            searchBarHint = "Search",
            searchQueryTextState = rememberTextFieldState(),
            onActiveChanged = {},
        )
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchTopBarInactive() {
    WireTheme {
        SearchTopBar(
            isSearchActive = false,
            searchBarHint = "Search",
            searchQueryTextState = rememberTextFieldState(),
            onActiveChanged = {},
        )
    }
}
