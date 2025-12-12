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

import androidx.compose.animation.AnimatedContent
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.BiasAlignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusEvent
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import com.wire.android.ui.common.R
import com.wire.android.ui.common.SearchBarInput
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun SearchTopBar(
    isSearchActive: Boolean,
    searchBarHint: String,
    backIconContentDescription: String? = null,
    searchQueryTextState: TextFieldState,
    modifier: Modifier = Modifier,
    isLoading: Boolean = false,
    searchBarDescription: String? = null,
    onCloseSearchClicked: (() -> Unit)? = null,
    onActiveChanged: (isActive: Boolean) -> Unit = {},
    bottomContent: @Composable ColumnScope.() -> Unit = {}
) {
    Column(
        modifier = modifier
            .wrapContentHeight()
            .fillMaxWidth()
            .background(MaterialTheme.wireColorScheme.background)
    ) {
        val interactionSource = remember { MutableInteractionSource() }
        val focusRequester = remember { FocusRequester() }
        val keyboardController = LocalSoftwareKeyboardController.current
        val focusManager = LocalFocusManager.current

        fun setActive(isActive: Boolean) {
            if (isActive) {
                focusRequester.requestFocus()
                keyboardController?.show()
            } else {
                focusManager.clearFocus()
                keyboardController?.hide()
                searchQueryTextState.clearText()
            }
        }

        LaunchedEffect(isSearchActive) {
            setActive(isSearchActive)
        }

        val placeholderAlignment by animateHorizontalAlignmentAsState(
            targetAlignment = if (isSearchActive) Alignment.CenterStart else Alignment.Center
        )

        SearchBarInput(
            placeholderText = searchBarHint,
            semanticDescription = searchBarDescription,
            textState = searchQueryTextState,
            isLoading = isLoading,
            leadingIcon = {
                AnimatedContent(!isSearchActive, label = "") { isVisible ->
                    if (isVisible) {
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
                    } else {
                        IconButton(
                            onClick = { onCloseSearchClicked?.invoke() ?: setActive(false) },
                            modifier = Modifier.size(dimensions().buttonCircleMinSize)
                        ) {
                            Icon(
                                painter = painterResource(R.drawable.ic_arrow_back),
                                contentDescription = backIconContentDescription,
                                tint = MaterialTheme.wireColorScheme.onBackground,
                            )
                        }
                    }
                }
            },
            placeholderTextStyle = LocalTextStyle.current.copy(textAlign = if (!isSearchActive) TextAlign.Center else TextAlign.Start),
            placeholderAlignment = placeholderAlignment,
            textStyle = LocalTextStyle.current.copy(textAlign = TextAlign.Start),
            interactionSource = interactionSource,
            modifier = Modifier
                .padding(dimensions().spacing8x)
                .focusable(true)
                .focusRequester(focusRequester)
                .onFocusEvent { onActiveChanged(it.isFocused) }
        )
        bottomContent()
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
