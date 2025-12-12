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

package com.wire.android.ui.common

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.input.TextFieldLineLimits
import androidx.compose.foundation.text.input.TextFieldState
import androidx.compose.foundation.text.input.clearText
import androidx.compose.foundation.text.input.rememberTextFieldState
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.sp
import com.wire.android.ui.common.progress.WireCircularProgressIndicator
import com.wire.android.ui.common.textfield.WireTextField
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.PreviewMultipleThemes

@Composable
fun SearchBarInput(
    placeholderText: String,
    leadingIcon: @Composable () -> Unit,
    textState: TextFieldState,
    modifier: Modifier = Modifier,
    placeholderTextStyle: TextStyle = LocalTextStyle.current,
    placeholderAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    interactionSource: MutableInteractionSource = remember { MutableInteractionSource() },
    textStyle: TextStyle = LocalTextStyle.current,
    isLoading: Boolean = false,
    semanticDescription: String? = null
) {

    WireTextField(
        modifier = modifier,
        textState = textState,
        leadingIcon = {
            leadingIcon()
        },
        trailingIcon = {
            Box(
                modifier = Modifier
                    .width(dimensions().spacing64x)
                    .height(dimensions().spacing40x),
                contentAlignment = Alignment.CenterEnd
            ) {
                AnimatedVisibility(
                    visible = textState.text.isNotBlank(),
                    enter = fadeIn(),
                    exit = fadeOut()
                ) {
                    if (isLoading) {
                        WireCircularProgressIndicator(
                            modifier = Modifier.padding(
                                top = dimensions().spacing12x,
                                bottom = dimensions().spacing12x,
                                end = dimensions().spacing32x
                            ),
                            progressColor = MaterialTheme.wireColorScheme.onSurface
                        )
                    }
                    IconButton(
                        modifier = Modifier.padding(start = dimensions().spacing12x),
                        onClick = textState::clearText,
                    ) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_clear_search),
                            contentDescription = stringResource(R.string.content_description_clear_content)
                        )
                    }
                }
            }
        },
        interactionSource = interactionSource,
        textStyle = textStyle.copy(fontSize = 14.sp),
        placeholderTextStyle = placeholderTextStyle.copy(fontSize = 14.sp),
        placeholderAlignment = placeholderAlignment,
        placeholderText = placeholderText,
        lineLimits = TextFieldLineLimits.SingleLine,
        semanticDescription = semanticDescription
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchBarInput() {
    WireTheme {
        SearchBarInput(
            placeholderText = "placeholder",
            textState = rememberTextFieldState(),
            leadingIcon = {
                IconButton(onClick = { }) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_search),
                        contentDescription = stringResource(R.string.content_description_conversation_search_icon),
                        tint = MaterialTheme.wireColorScheme.onBackground
                    )
                }
            },
        )
    }
}
