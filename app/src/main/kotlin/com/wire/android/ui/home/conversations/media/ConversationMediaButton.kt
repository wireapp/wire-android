/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.media

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.preview.MultipleThemePreviews
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireDimensions

@Composable
fun ConversationMediaButton(
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    WireSecondaryButton(
        modifier = modifier,
        text = stringResource(R.string.label_conversation_media),
        onClick = onClick,
        minSize = MaterialTheme.wireDimensions.buttonMinSize,
        fillMaxWidth = true,
        leadingIcon = {
            Icon(
                painter = painterResource(id = R.drawable.ic_gallery),
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onBackground,
                modifier = Modifier.padding(end = dimensions().spacing8x)
            )
        },
        onClickDescription = stringResource(id = R.string.content_description_see_media_in_conversation_btn)
    )
}

@MultipleThemePreviews
@Composable
fun PreviewConversationMediaButton() {
    WireTheme {
        ConversationMediaButton(onClick = {})
    }
}
