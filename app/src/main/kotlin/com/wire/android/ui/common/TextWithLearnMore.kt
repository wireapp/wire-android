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
package com.wire.android.ui.common

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.withStyle
import com.wire.android.R
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.toSpanStyle

@Composable
fun TextWithLearnMore(
    textAnnotatedString: AnnotatedString,
    learnMoreLink: String,
    modifier: Modifier = Modifier,
    onTextLayout: (TextLayoutResult) -> Unit = {}
) {
    val context = LocalContext.current
    val learnMoreText = stringResource(id = R.string.label_learn_more).replace(" ", "\u00A0") // non-breaking space
    val learnMoreAnnotatedString = buildAnnotatedString {
        append(learnMoreText)
        addStyle(
            style = SpanStyle(
                color = MaterialTheme.colorScheme.onBackground,
                textDecoration = TextDecoration.Underline
            ),
            start = 0,
            end = learnMoreText.length
        )
        addStringAnnotation(
            tag = TAG_LEARN_MORE,
            annotation = learnMoreLink,
            start = 0,
            end = learnMoreText.length
        )
    }
    val fullAnnotatedString = textAnnotatedString + AnnotatedString(" ") + learnMoreAnnotatedString
    androidx.compose.foundation.text.ClickableText(
        modifier = modifier,
        text = fullAnnotatedString,
        onTextLayout = onTextLayout,
        onClick = { offset ->
            fullAnnotatedString.getStringAnnotations(TAG_LEARN_MORE, offset, offset)
                .firstOrNull()?.let { result -> CustomTabsHelper.launchUrl(context, result.item) }
        },
    )
}

private const val TAG_LEARN_MORE = "tag_learn_more"

@PreviewMultipleThemes
@Composable
fun PreviewTextWithLearnMore() = WireTheme {
    TextWithLearnMore(
        textAnnotatedString = buildAnnotatedString {
            withStyle(toSpanStyle(typography().body01, colorsScheme().onBackground)) {
                append("This is text with a learn more link")
            }
        },
        learnMoreLink = "https://www.wire.com",
    )
}
