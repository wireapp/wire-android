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
package com.wire.android.ui.home.conversationslist.all

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.ConversationFilter

@Composable
fun ConversationsEmptyContent(
    modifier: Modifier = Modifier,
    filter: ConversationFilter = ConversationFilter.All,
    domain: String = "wire.com"
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(
                dimensions().spacing40x
            ),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (filter == ConversationFilter.All) {
            Text(
                modifier = Modifier.padding(
                    bottom = dimensions().spacing24x,
                    top = dimensions().spacing100x
                ),
                text = stringResource(R.string.conversation_empty_list_title),
                style = MaterialTheme.wireTypography.title01,
                color = MaterialTheme.wireColorScheme.onSurface,
            )
        }
        Text(
            modifier = Modifier.padding(bottom = dimensions().spacing8x),
            text = filter.emptyDescription(domain),
            style = MaterialTheme.wireTypography.body01,
            textAlign = TextAlign.Center,
            color = MaterialTheme.wireColorScheme.onSurface,
        )
        VerticalSpace.x8()
        EmptyContentFooter(currentFilter = filter)
    }
}

@Composable
private fun EmptyContentFooter(currentFilter: ConversationFilter) {
    val context = LocalContext.current
    when (currentFilter) {
        ConversationFilter.Favorites -> {
            val supportUrl = stringResource(id = R.string.url_how_to_add_favorites)
            Text(
                text = stringResource(R.string.favorites_empty_list_how_to_label),
                style = MaterialTheme.wireTypography.body02.copy(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.clickable {
                    CustomTabsHelper.launchUrl(context, supportUrl)
                }
            )
        }

        ConversationFilter.Channels -> {
            val supportUrl = stringResource(id = R.string.url_support) // todo. change to url for channels
            Text(
                text = stringResource(R.string.channels_empty_list_learn_more),
                style = MaterialTheme.wireTypography.body02.copy(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.clickable {
                    CustomTabsHelper.launchUrl(context, supportUrl)
                }
            )
        }

        else -> {
            Image(
                modifier = Modifier.padding(start = dimensions().spacing100x),
                painter = painterResource(
                    id = R.drawable.ic_empty_conversation_arrow
                ),
                contentDescription = ""
            )
        }
    }
}

@Composable
private fun ConversationFilter.emptyDescription(backendName: String): String = when (this) {
    ConversationFilter.All -> stringResource(R.string.conversation_empty_list_description)
    ConversationFilter.Favorites -> stringResource(R.string.favorites_empty_list_description)
    ConversationFilter.Groups -> stringResource(R.string.group_empty_list_description)
    ConversationFilter.Channels -> stringResource(R.string.channels_empty_list_description)
    ConversationFilter.OneOnOne -> stringResource(R.string.one_on_one_empty_list_description, backendName)
    // currently not used, because empty folders are removed from filters
    is ConversationFilter.Folder -> ""
}

@PreviewMultipleThemes
@Composable
fun PreviewAllConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.All)
}

@PreviewMultipleThemes
@Composable
fun PreviewChannelsConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.Channels)
}

@PreviewMultipleThemes
@Composable
fun PreviewFavoritesConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.Favorites)
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.Groups)
}

@PreviewMultipleThemes
@Composable
fun PreviewOneOnOneConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.OneOnOne, domain = "wire.com")
}
