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

package com.wire.android.ui.home.conversations.banner

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.QueryMatchExtractor
import com.wire.android.util.ui.UIText

@Composable
fun ConversationBanner(
    bannerMessage: UIText?,
    modifier: Modifier = Modifier,
    spannedTexts: List<String> = listOf()
) {
    bannerMessage?.let { uiText ->
        Column(modifier = modifier.fillMaxWidth()) {
            HorizontalDivider(color = MaterialTheme.wireColorScheme.divider)

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(color = MaterialTheme.colorScheme.surface)
                    .padding(vertical = dimensions().spacing6x, horizontal = dimensions().spacing16x),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = styleBannerText(uiText, spannedTexts),
                    textAlign = TextAlign.Center
                )
            }

            HorizontalDivider(color = MaterialTheme.wireColorScheme.divider)
        }
    }
}

@Composable
private fun styleBannerText(
    uiText: UIText,
    spannedTexts: List<String>
) = buildAnnotatedString {
    withStyle(
        style = SpanStyle(
            color = MaterialTheme.wireColorScheme.onSurface,
            fontWeight = MaterialTheme.wireTypography.title03.fontWeight,
            fontSize = MaterialTheme.wireTypography.title03.fontSize,
            fontFamily = MaterialTheme.wireTypography.title03.fontFamily,
            fontStyle = MaterialTheme.wireTypography.title03.fontStyle,
            letterSpacing = MaterialTheme.wireTypography.title03.letterSpacing
        )
    ) {
        append(uiText.asString())
    }

    spannedTexts.flatMap { textToSpan ->
        QueryMatchExtractor.extractQueryMatchIndexes(
            matchText = textToSpan,
            text = uiText.asString()
        )
    }.forEach { highLightIndex ->
        if (highLightIndex.endIndex <= this.length) {
            addStyle(
                style = SpanStyle(
                    color = MaterialTheme.wireColorScheme.primary,
                    fontWeight = FontWeight.Bold
                ),
                start = highLightIndex.startIndex,
                end = highLightIndex.endIndex
            )
        }
    }
}

@Preview
@Composable
fun PreviewConversationBanner() {
    ConversationBanner(
        bannerMessage = UIText.DynamicString("Federated users, Externals, guests and services are present"),
        spannedTexts = listOf("Federated users", "Guests")
    )
}
