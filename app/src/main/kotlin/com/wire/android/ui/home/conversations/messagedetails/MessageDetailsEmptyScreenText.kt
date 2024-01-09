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

package com.wire.android.ui.home.conversations.messagedetails

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun MessageDetailsEmptyScreenText(
    onClick: () -> Unit,
    modifier: Modifier,
    text: String,
    learnMoreText: String
) {
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = dimensions().spacing48x),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                text = text,
                style = MaterialTheme.wireTypography.body01.copy(color = MaterialTheme.wireColorScheme.secondaryText),
                textAlign = TextAlign.Center,
            )
            Spacer(modifier = Modifier.height(dimensions().spacing16x))
            val learnMore = buildAnnotatedString {
                append(learnMoreText)
                addStyle(
                    style = SpanStyle(
                        color = MaterialTheme.colorScheme.primary,
                        textDecoration = TextDecoration.Underline
                    ),
                    start = 0,
                    end = learnMoreText.length
                )
            }
            ClickableText(
                text = learnMore,
                onClick = { onClick() },
                style = MaterialTheme.wireTypography.body02,
            )
        }
    }
}

@Preview
@Composable
fun PreviewMessageDetailsEmptyScreenText() {
    MessageDetailsEmptyScreenText(
        onClick = { },
        modifier = Modifier
            .fillMaxHeight()
            .fillMaxWidth(),
        text = "Learn More Text",
        learnMoreText = "Learn More URL Label"
    )
}
