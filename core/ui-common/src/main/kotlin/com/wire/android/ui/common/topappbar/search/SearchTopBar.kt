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
import androidx.compose.foundation.interaction.FocusInteraction
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
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusProperties
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEvent
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.isAltPressed
import androidx.compose.ui.input.key.isCtrlPressed
import androidx.compose.ui.input.key.isMetaPressed
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.input.key.utf16CodePoint
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
import kotlinx.coroutines.launch

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
    previousFocusRequester: FocusRequester? = null,
    nextFocusRequester: FocusRequester? = null,
    activateSearchOnFocus: Boolean = true,
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
                previousFocusRequester = previousFocusRequester,
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
                previousFocusRequester = previousFocusRequester,
                nextFocusRequester = nextFocusRequester,
                keepBackButtonVisible = keepBackButtonVisible,
                backIconContentDescription = resolvedBackIconContentDescription,
                onCloseSearchInput = ::closeSearchInput,
                onTap = onTap,
                onActiveChanged = onActiveChanged,
                onShowKeyboard = ::showKeyboard,
                activateSearchOnFocus = activateSearchOnFocus
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
    previousFocusRequester: FocusRequester?,
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
                previousFocusRequester = previousFocusRequester,
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
    previousFocusRequester: FocusRequester?,
    nextFocusRequester: FocusRequester?,
    keepBackButtonVisible: Boolean,
    backIconContentDescription: String,
    onCloseSearchInput: () -> Unit,
    onTap: (() -> Unit)?,
    onActiveChanged: (Boolean) -> Unit,
    onShowKeyboard: () -> Unit,
    activateSearchOnFocus: Boolean,
) {
    var isSearchBarFocused by remember { mutableStateOf(false) }
    val keyboardFocusInteraction = remember { KeyboardFocusInteractionState() }

    InactiveSearchFocusInteraction(
        activateSearchOnFocus = activateSearchOnFocus,
        isSearchBarFocused = isSearchBarFocused,
        keyboardFocusInteraction = keyboardFocusInteraction,
        interactionSource = interactionSource
    )

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
                    previousFocusRequester = previousFocusRequester,
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
            .focusProperties {
                previousFocusRequester?.let { previous = it }
                nextFocusRequester?.let { next = it }
            }
            .onPreviewKeyEvent { event ->
                when {
                    event.isSearchActivationKey -> {
                        activateSearch()
                        true
                    }

                    event.printableCharacter != null -> {
                        textState.edit {
                            append(event.printableCharacter.orEmpty())
                        }
                        activateSearch()
                        true
                    }

                    else -> false
                }
            }
            .onFocusChanged {
                isSearchBarFocused = it.isFocused
                if (activateSearchOnFocus && it.isFocused) {
                    activateSearch()
                }
            }
            .semantics {
                semanticDescription?.let { contentDescription = it }
            }
            .focusable(),
        inputModifier = Modifier.onFocusEvent {
            if (activateSearchOnFocus && it.isFocused) {
                activateSearch()
            }
        }
    )
}

@Composable
private fun InactiveSearchFocusInteraction(
    activateSearchOnFocus: Boolean,
    isSearchBarFocused: Boolean,
    keyboardFocusInteraction: KeyboardFocusInteractionState,
    interactionSource: MutableInteractionSource,
) {
    val currentInteractionSource = rememberUpdatedState(interactionSource)
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(activateSearchOnFocus, isSearchBarFocused) {
        val interaction = keyboardFocusInteraction.value
        when {
            !activateSearchOnFocus && isSearchBarFocused && interaction == null -> {
                FocusInteraction.Focus().also {
                    keyboardFocusInteraction.value = it
                    currentInteractionSource.value.emit(it)
                }
            }

            (activateSearchOnFocus || !isSearchBarFocused) && interaction != null -> {
                currentInteractionSource.value.emit(FocusInteraction.Unfocus(interaction))
                keyboardFocusInteraction.value = null
            }
        }
    }

    DisposableEffect(currentInteractionSource) {
        onDispose {
            keyboardFocusInteraction.value?.let {
                coroutineScope.launch {
                    currentInteractionSource.value.emit(FocusInteraction.Unfocus(it))
                }
            }
        }
    }
}

private class KeyboardFocusInteractionState {
    var value: FocusInteraction.Focus? = null
}

@Composable
private fun SearchBackButton(
    focusRequester: FocusRequester?,
    previousFocusRequester: FocusRequester?,
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
                previousFocusRequester?.let { previous = it }
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

private val navigationKeys = setOf(
    Key.Tab,
    Key.DirectionUp,
    Key.DirectionDown,
    Key.DirectionLeft,
    Key.DirectionRight,
    Key.Back,
    Key.Escape,
    Key.Backspace,
    Key.Delete,
    Key.Enter,
    Key.NumPadEnter,
    Key.MoveHome,
    Key.MoveEnd,
    Key.PageUp,
    Key.PageDown,
    Key.Spacebar,
)

private val KeyEvent.isSearchActivationKey: Boolean
    get() = type == KeyEventType.KeyDown && (key == Key.Enter || key == Key.Spacebar)

private val KeyEvent.printableCharacter: String?
    get() {
        if (type != KeyEventType.KeyDown || isSystemOrNavigationKey) {
            return null
        }
        val unicodeChar = utf16CodePoint
        return unicodeChar.takeIf { it != 0 }?.let { String(Character.toChars(it)) }
    }

private val KeyEvent.isSystemOrNavigationKey: Boolean
    get() = key in navigationKeys || isCtrlPressed || isMetaPressed || isAltPressed

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
