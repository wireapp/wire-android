/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
package com.wire.android.ui.home.messagecomposer

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Divider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.wire.android.appLogger
import com.wire.android.ui.theme.wireColorScheme

@OptIn(ExperimentalComposeUiApi::class, ExperimentalLayoutApi::class)
@Composable
fun SampleScreen() {
    val keyboardController = LocalSoftwareKeyboardController.current
    val focusRequester = remember { FocusRequester() }

    var isFocused by remember { mutableStateOf(false) }
    var showOptions by remember { mutableStateOf(false) }
    var showSubOptions by remember { mutableStateOf(true) }
    var hideOnlyKeyboard by remember { mutableStateOf(false) }
    var isTextExpanded by remember { mutableStateOf(false) }

    val textFieldValue = TextFieldValue("")
    val isImeVisible = WindowInsets.isImeVisible

    var keyboardHeight by remember { mutableStateOf(250.dp) } // Initial keyboard height
    val density = LocalDensity.current
    var optionsHeight by remember { mutableStateOf(keyboardHeight) }

    val offsetY = WindowInsets.ime.getBottom(density)
    val navBarHeight = BottomNavigationBarHeight()

    val focusManager = LocalFocusManager.current

    LaunchedEffect(isFocused) {
        if (isFocused) focusRequester.requestFocus()
        else focusManager.clearFocus()
    }

    LaunchedEffect(isImeVisible) {
        appLogger.d("KBX isImeVisible $isImeVisible")

        isFocused = isImeVisible
        if (!showSubOptions) {
            showOptions = isImeVisible
        }
    }

    with(density) {
        val offset = offsetY.toDp() - navBarHeight
//        appLogger.d("KBX navbar height $navBarHeight")
//        appLogger.d("KBX offset $offset")
        if (keyboardHeight < offset) {
            appLogger.d("KBX hideOnlyKeyboard false $offset")
            hideOnlyKeyboard = false
            keyboardHeight = offset
        }
        if (!hideOnlyKeyboard) {
            appLogger.d("KBX set optionsHeight to offset $offset")
            optionsHeight = offset
        }

    }

    // Handling back button
    BackHandler(isImeVisible || showOptions) {
        if (isImeVisible || showOptions) {
            appLogger.d("KBX BackHandler isImeVisible $isImeVisible showOptions $showOptions")
            isFocused = false
            showOptions = false
            keyboardController?.hide()
            focusRequester.freeFocus()
        }
    }

    Column(
        modifier = Modifier.fillMaxSize()
    ) {
        // MessagesBox
        val costam =
            if (isTextExpanded) {
                Modifier.height(0.dp)
            } else {
                Modifier.weight(1f)
            }
        Box(
            modifier = costam
                .background(Color.Blue)
        ) {
            LazyColumn {
                items(20) { index ->
                    Text(
                        text = "Item $index",
                        modifier = Modifier.padding(8.dp),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }

        val fillRemainingSpaceOrWrapContent =
            if (!isTextExpanded) {
                Modifier.height(80.dp)
            } else {
                Modifier.weight(1f)
            }
        // Text input box and Plus button
        Column(
            modifier = fillRemainingSpaceOrWrapContent
                .background(Color.White)
                .fillMaxWidth()
        ) {
            Divider(color = MaterialTheme.wireColorScheme.outline)
            CollapseButton(
                onCollapseClick = { isTextExpanded = !isTextExpanded }
            )

            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (!isFocused && !showOptions) {
                    IconButton(
                        onClick = {
                            showOptions = true
                            optionsHeight = keyboardHeight
                            hideOnlyKeyboard = true
                            keyboardController?.hide()
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    ) {
                        Icon(imageVector = Icons.Default.Add, contentDescription = "Plus")
                    }
                }

                BasicTextField(
                    value = textFieldValue,
                    onValueChange = { },
                    keyboardOptions = KeyboardOptions.Default.copy(
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(),
                    textStyle = MaterialTheme.typography.bodyMedium.copy(fontSize = 16.sp),
                    modifier = Modifier
                        .weight(1f)
                        .onFocusChanged { focusState ->
                            isFocused = focusState.isFocused
                            appLogger.d("KBX is focused ${focusState.isFocused} has focus ${focusState.hasFocus}")
                            if (focusState.isFocused) {
                                showOptions = true
                                showSubOptions = true
                            }
                        }
                        .focusRequester(focusRequester)
                )
            }
        }

        // Show SubOptionsBox when TextField is focused or Plus button is clicked
        if (showOptions) {
            SubOptionsBox(
                showSubOptions = showSubOptions,
                optionsHeight = optionsHeight,
                showSubOptionsCallback = {
                    hideOnlyKeyboard = true
                    showSubOptions = true
                    focusManager.clearFocus()
                    optionsHeight = keyboardHeight
                }
            )
        }
    }
}

@Composable
fun SubOptionsBox(
    showSubOptions: Boolean,
    optionsHeight: Dp,
    showSubOptionsCallback: () -> Unit
) {
    val colors = listOf(
        Color.Red,
        Color.Green,
        Color.Blue,
        Color.Yellow,
        Color.Magenta
    )

    val subOptionsModifier = Modifier
        .background(Color.Black)
        .fillMaxWidth()

    Column(
        modifier = subOptionsModifier
    ) {
        // Icons buttons
        Row(
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            colors.forEachIndexed { index, color ->
                IconButton(
                    onClick = {
                        if (index == 0) {
                            showSubOptionsCallback()
                        }
                    },
                    modifier = Modifier
                        .fillMaxHeight()
                        .width(48.dp)
                        .background(color)
                ) {
                    // Your icon here
                }
            }
        }
        if (showSubOptions) {
            // SubOptions content
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(optionsHeight)
                    .border(3.dp, Color.Black)
                    .background(colors[0])
            ) {
                // Your SubOptions content here
            }
        }
    }
}

@Composable
fun BottomNavigationBarHeight(): Dp {
    val insets = ViewCompat.getRootWindowInsets(LocalView.current)
    val bottomInset = insets?.getInsets(WindowInsetsCompat.Type.navigationBars())?.bottom ?: 0
    return with(LocalDensity.current) {
        bottomInset.toDp()
    }
}
