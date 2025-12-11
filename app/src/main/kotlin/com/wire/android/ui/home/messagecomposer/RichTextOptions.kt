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
package com.wire.android.ui.home.messagecomposer

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryIconButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.common.R as commonR

@Composable
fun RichTextOptions(
    onRichTextHeaderButtonClicked: () -> Unit,
    onRichTextBoldButtonClicked: () -> Unit,
    onRichTextItalicButtonClicked: () -> Unit,
    onCloseRichTextEditingButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier.wrapContentSize()) {
        HorizontalDivider(color = MaterialTheme.wireColorScheme.outline)
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Absolute.SpaceEvenly,
            modifier = Modifier.wrapContentSize()
                .height(dimensions().spacing56x)
        ) {
            val iconModifier = Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = dimensions().spacing0x)

            HeaderButton(
                modifier = iconModifier,
                onRichTextHeaderButtonClicked = onRichTextHeaderButtonClicked
            )
            BoldButton(
                modifier = iconModifier,
                onRichTextBoldButtonClicked = onRichTextBoldButtonClicked
            )
            ItalicButton(
                modifier = iconModifier,
                onRichTextItalicButtonClicked = onRichTextItalicButtonClicked,
            )
            CloseButton(
                onCloseRichTextEditingButtonClicked = onCloseRichTextEditingButtonClicked
            )
        }
    }
}

@Composable
private fun HeaderButton(
    modifier: Modifier = Modifier,
    onRichTextHeaderButtonClicked: () -> Unit
) {
    WireSecondaryIconButton(
        onButtonClicked = onRichTextHeaderButtonClicked,
        iconResource = R.drawable.ic_rich_text_header,
        contentDescription = R.string.content_description_conversation_rich_text_header,
        modifier = modifier
            .padding(start = dimensions().spacing8x),
        fillMaxWidth = true,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.wireDimensions.buttonCornerSize,
            bottomStart = MaterialTheme.wireDimensions.buttonCornerSize,
            topEnd = MaterialTheme.wireDimensions.spacing0x,
            bottomEnd = MaterialTheme.wireDimensions.spacing0x
        )
    )
}

@Composable
private fun BoldButton(
    modifier: Modifier = Modifier,
    onRichTextBoldButtonClicked: () -> Unit
) {
    WireSecondaryIconButton(
        onButtonClicked = onRichTextBoldButtonClicked,
        iconResource = R.drawable.ic_rich_text_bold,
        contentDescription = R.string.content_description_conversation_rich_text_bold,
        modifier = modifier,
        fillMaxWidth = true,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.wireDimensions.spacing0x,
            bottomStart = MaterialTheme.wireDimensions.spacing0x,
            topEnd = MaterialTheme.wireDimensions.spacing0x,
            bottomEnd = MaterialTheme.wireDimensions.spacing0x
        )
    )
}

@Composable
private fun ItalicButton(
    onRichTextItalicButtonClicked: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireSecondaryIconButton(
        onButtonClicked = onRichTextItalicButtonClicked,
        iconResource = R.drawable.ic_rich_text_italic,
        contentDescription = R.string.content_description_conversation_rich_text_italic,
        modifier = modifier,
        fillMaxWidth = true,
        shape = RoundedCornerShape(
            topStart = MaterialTheme.wireDimensions.spacing0x,
            bottomStart = MaterialTheme.wireDimensions.spacing0x,
            topEnd = MaterialTheme.wireDimensions.buttonCornerSize,
            bottomEnd = MaterialTheme.wireDimensions.buttonCornerSize
        )
    )
}

@Composable
private fun CloseButton(
    onCloseRichTextEditingButtonClicked: () -> Unit
) {
    IconButton(
        onClick = onCloseRichTextEditingButtonClicked,
        modifier = Modifier
            .padding(end = dimensions().spacing8x)
    ) {
        Icon(
            painter = painterResource(commonR.drawable.ic_close),
            contentDescription = stringResource(R.string.content_description_close_button)
        )
    }
}
