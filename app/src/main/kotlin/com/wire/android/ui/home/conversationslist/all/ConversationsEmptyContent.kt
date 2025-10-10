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

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rowitem.EmptyListArrowFooter
import com.wire.android.ui.common.rowitem.EmptyListContent
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.destinations.BrowseChannelsScreenDestination
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.kalium.logic.data.conversation.ConversationFilter

@Composable
fun ConversationsEmptyContent(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    filter: ConversationFilter = ConversationFilter.All,
    domain: String = "wire.com"
) {
    EmptyListContent(
        title = if (filter == ConversationFilter.All) stringResource(R.string.conversation_empty_list_title) else null,
        text = filter.emptyDescription(domain),
        modifier = modifier,
        footer = {
            EmptyContentFooter(currentFilter = filter, navigator = navigator)
        }
    )
}

@Composable
private fun EmptyContentFooter(currentFilter: ConversationFilter, navigator: Navigator) {
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
            VerticalSpace.x8()
            WirePrimaryButton(
                modifier = Modifier
                    .height(dimensions().buttonSmallMinSize.height)
                    .wrapContentWidth(),
                fillMaxWidth = false,
                text = stringResource(R.string.label_browse_public_channels),
                state = if (BuildConfig.PUBLIC_CHANNELS_ENABLED) {
                    WireButtonState.Default
                } else {
                    WireButtonState.Disabled
                },
                onClick = { navigator.navigate(NavigationCommand(BrowseChannelsScreenDestination)) }
            )
        }

        else -> EmptyListArrowFooter()
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
    ConversationsEmptyContent(filter = ConversationFilter.All, navigator = rememberNavigator {})
}

@PreviewMultipleThemes
@Composable
fun PreviewChannelsConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.Channels, navigator = rememberNavigator {})
}

@PreviewMultipleThemes
@Composable
fun PreviewFavoritesConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.Favorites, navigator = rememberNavigator {})
}

@PreviewMultipleThemes
@Composable
fun PreviewGroupConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.Groups, navigator = rememberNavigator {})
}

@PreviewMultipleThemes
@Composable
fun PreviewOneOnOneConversationsEmptyContent() = WireTheme {
    ConversationsEmptyContent(filter = ConversationFilter.OneOnOne, domain = "wire.com", navigator = rememberNavigator {})
}
