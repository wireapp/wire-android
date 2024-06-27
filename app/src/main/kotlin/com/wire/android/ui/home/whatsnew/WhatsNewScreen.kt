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

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.HomeNavGraph
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.handleNavigation
import com.wire.android.ui.home.HomeStateHolder
import com.wire.android.util.extension.folderWithElements
import com.wire.android.util.ui.UIText

@HomeNavGraph
@Destination
@Composable
fun WhatsNewScreen(
    homeStateHolder: HomeStateHolder,
    whatsNewViewModel: WhatsNewViewModel = hiltViewModel()
) {
    val lazyListState: LazyListState = rememberLazyListState()
    val context = LocalContext.current
    WhatsNewScreenContent(
        state = whatsNewViewModel.state,
        lazyListState = lazyListState,
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
    lazyListState: LazyListState = rememberLazyListState(),
    onItemClicked: (WhatsNewItem) -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        state = lazyListState,
        modifier = Modifier.fillMaxSize()
    ) {
        folderWithElements(
            items = buildList {
                add(WhatsNewItem.WelcomeToNewAndroidApp)
            },
            onItemClicked = onItemClicked,
            isLoading = false,
        )

        folderWithElements(
            header = context.getString(R.string.whats_new_release_notes_group_title),
            items = buildList {
                if (state.isLoading) {
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
                } else {
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
                add(WhatsNewItem.AllAndroidReleaseNotes)
            },
            onItemClicked = onItemClicked,
            isLoading = state.isLoading,
        )
    }
}

private fun LazyListScope.folderWithElements(
    header: String? = null,
    items: List<WhatsNewItem>,
    onItemClicked: (WhatsNewItem) -> Unit,
    isLoading: Boolean,
) {
    folderWithElements(
        header = header?.uppercase(),
        items = items.associateBy { it.id }
    ) { item ->
        WhatsNewItem(
            title = item.title.asString(),
            boldTitle = item.boldTitle,
            text = item.text?.asString(),
            onRowPressed = remember { Clickable(enabled = !isLoading) { onItemClicked(item) } },
            trailingIcon = R.drawable.ic_arrow_right,
            isLoading = isLoading,
        )
    }
}

@Preview(showBackground = false)
@Composable
fun PreviewWhatsNewScreen() {
    WhatsNewScreenContent(WhatsNewState(isLoading = false)) {}
}

@Preview(showBackground = false)
@Composable
fun PreviewWhatsNewScreenLoading() {
    WhatsNewScreenContent(WhatsNewState(isLoading = true)) {}
}
