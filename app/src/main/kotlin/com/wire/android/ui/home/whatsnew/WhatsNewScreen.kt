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

package com.wire.android.ui.home.whatsnew
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.HomeDestination
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.util.ui.sectionWithElements
import com.wire.android.util.ui.UIText

@HomeNavGraph
@Destination<WireRootNavGraph>
@Composable
fun WhatsNewScreen(
    homeStateHolder: HomeStateHolder,
    whatsNewViewModel: WhatsNewViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    WhatsNewScreenContent(
        state = whatsNewViewModel.state,
        lazyListState = homeStateHolder.lazyListStateFor(HomeDestination.WhatsNew),
        onItemClicked = remember {
            {
                it.direction.handleNavigation(
                    context = context,
                    handleOtherDirection = { homeStateHolder.navigator.navigate(NavigationCommand(it)) }
                )
            }
        }
    )
}

@Composable
fun WhatsNewScreenContent(
    state: WhatsNewState,
    onItemClicked: (WhatsNewItem) -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState()
) {
    val context = LocalContext.current
    val openLinkLabel = stringResource(R.string.content_description_open_link_label)
    LazyColumn(
        state = lazyListState,
        modifier = modifier.fillMaxSize()
    ) {
        if (BuildConfig.SHOULD_DISPLAY_RELEASE_NOTES) {
            sectionWithElements(
                items = buildList {
                    add(WhatsNewItem.WelcomeToNewAndroidApp)
                },
                onItemClicked = onItemClicked,
                onItemClickedDescription = openLinkLabel,
                isLoading = false,
            )
        }

        sectionWithElements(
            header = context.getString(R.string.whats_new_release_notes_group_title),
            items = buildList {
                if (state.isLoading) {
                    // placeholders with shimmer effect
                    for (i in 0..3) {
                        add(
                            WhatsNewItem.AndroidReleaseNotes(
                                id = "placeholder_$i",
                                title = UIText.DynamicString("Android X.X.X"), // this text won't be displayed
                                boldTitle = true,
                                text = UIText.DynamicString("01 Jan 2024"), // this text won't be displayed
                                url = "",
                            )
                        )
                    }
                    add(WhatsNewItem.AllAndroidReleaseNotes(id = "placeholder_all"))
                } else {
                    if (BuildConfig.SHOULD_DISPLAY_RELEASE_NOTES) {
                        state.releaseNotesItems.forEach {
                            add(
                                WhatsNewItem.AndroidReleaseNotes(
                                    id = it.id,
                                    title = UIText.DynamicString(it.title),
                                    boldTitle = true,
                                    text = UIText.DynamicString(it.publishDate),
                                    url = it.link,
                                )
                            )
                        }
                    }
                    add(WhatsNewItem.AllAndroidReleaseNotes())
                }
            },
            onItemClicked = onItemClicked,
            onItemClickedDescription = openLinkLabel,
            isLoading = state.isLoading,
        )
    }
}

private fun LazyListScope.sectionWithElements(
    header: String? = null,
    items: List<WhatsNewItem>,
    onItemClicked: (WhatsNewItem) -> Unit,
    onItemClickedDescription: String,
    isLoading: Boolean,
) {
    sectionWithElements(
        header = header?.uppercase(),
        items = items.associateBy { it.id }
    ) { item ->
        val contentDescription = when (item) {
            WhatsNewItem.WelcomeToNewAndroidApp -> stringResource(R.string.content_description_whats_new_welcome_item)
            is WhatsNewItem.AllAndroidReleaseNotes -> stringResource(R.string.content_description_whats_new_all_releases_item)
            is WhatsNewItem.AndroidReleaseNotes ->
                stringResource(R.string.content_description_whats_new_release_item, item.title.asString(), item.text?.asString() ?: "")
        }
        WhatsNewItem(
            title = item.title.asString(),
            boldTitle = item.boldTitle,
            text = item.text?.asString(),
            onRowPressed = remember(isLoading) {
                Clickable(
                    enabled = !isLoading,
                    onClickDescription = onItemClickedDescription
                ) { onItemClicked(item) }
            },
            contentDescription = contentDescription,
            trailingIcon = R.drawable.ic_arrow_right,
            isLoading = isLoading,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewWhatsNewScreen() {
    WhatsNewScreenContent(
        state = WhatsNewState(
            isLoading = false,
            releaseNotesItems = buildList {
                for (i in 0..3) {
                    add(ReleaseNotesItem(i.toString(), "Title $i", "https://www.example.com", "01 Jan 2024"))
                }
            }
        ),
        onItemClicked = {}
    )
}

@Preview(showBackground = false)
@Composable
fun PreviewWhatsNewScreenLoading() {
    WhatsNewScreenContent(
        state = WhatsNewState(isLoading = true),
        onItemClicked = {}
    )
}
