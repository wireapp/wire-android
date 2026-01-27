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
package com.wire.android.ui.home.conversations.search.messages

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun SearchConversationMessagesEmptyScreen(
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val searchUrl = stringResource(R.string.url_learn_about_conversation_search)

    Box(
        modifier = modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = stringResource(R.string.label_search_messages_empty_title),
                style = MaterialTheme.wireTypography.body01.copy(
                    color = MaterialTheme.wireColorScheme.secondaryText
                ),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(dimensions().spacing8x))

            Text(
                text = stringResource(R.string.label_learn_more),
                style = MaterialTheme.wireTypography.body02.copy(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.clickable(
                    onClickLabel = stringResource(
                        R.string.content_description_open_link_label
                    )
                ) {
                    CustomTabsHelper.launchUrl(context, searchUrl)
                }
            )
        }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewSearchConversationMessagesEmptyScreen() {
    WireTheme {
        SearchConversationMessagesEmptyScreen()
    }
}
