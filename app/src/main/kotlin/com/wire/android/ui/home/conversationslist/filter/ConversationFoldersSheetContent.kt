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
package com.wire.android.ui.home.conversationslist.filter

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.ArrowLeftIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.RichMenuItemState
import com.wire.android.ui.common.bottomsheet.SelectableMenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.data.conversation.Filter

@Composable
fun ConversationFoldersSheetContent(
    sheetData: ConversationFilterSheetData,
    onChangeFolder: (Filter.Conversation.Folder) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    WireMenuModalSheetContent(
        modifier = modifier
            .verticalScroll(rememberScrollState()),
        header = MenuModalSheetHeader.Visible(
            title = stringResource(R.string.label_folders),
            customVerticalPadding = dimensions().spacing8x,
            leadingIcon = {
                IconButton(onClick = onBackClick) {
                    ArrowLeftIcon()
                }
            },
            includeDivider = sheetData.folders.isNotEmpty()
        ),
        menuItems = buildList<@Composable () -> Unit> {
            if (sheetData.folders.isEmpty()) {
                add {
                    EmptyFolders()
                }
            } else {
                sheetData.folders.forEach { folder ->
                    add {
                        val state = if (sheetData.currentFilter is Filter.Conversation.Folder) {
                            val currentFolder = sheetData.currentFilter
                            if (currentFolder.folderId == folder.id) {
                                RichMenuItemState.SELECTED
                            } else {
                                RichMenuItemState.DEFAULT
                            }
                        } else {
                            RichMenuItemState.DEFAULT
                        }
                        SelectableMenuBottomSheetItem(
                            title = folder.name,
                            onItemClick = Clickable(
                                enabled = state == RichMenuItemState.DEFAULT,
                                onClickDescription = stringResource(id = R.string.content_description_select_label),
                                onClick = { onChangeFolder(Filter.Conversation.Folder(folder.name, folder.id)) }
                            ),
                            state = state
                        )
                    }
                }
            }
        }
    )
}

@Composable
private fun EmptyFolders() {
    val context = LocalContext.current
    Box(
        modifier = Modifier
            .height(dimensions().spacing300x)
            .fillMaxWidth(),
    ) {
        Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                painter = painterResource(id = R.drawable.ic_folders_outline),
                contentDescription = "",
                tint = colorsScheme().secondaryText,
                modifier = Modifier
                    .size(MaterialTheme.wireDimensions.spacing56x)
            )
            VerticalSpace.x16()
            Text(
                text = stringResource(R.string.folders_empty_list_description),
                style = typography().body01,
            )
            VerticalSpace.x16()
            val supportUrl = stringResource(id = R.string.url_how_to_add_folders)
            Text(
                text = stringResource(R.string.folders_empty_list_how_to_add),
                style = MaterialTheme.wireTypography.body02.copy(
                    textDecoration = TextDecoration.Underline,
                    color = MaterialTheme.colorScheme.onBackground
                ),
                modifier = Modifier.clickable {
                    CustomTabsHelper.launchUrl(context, supportUrl)
                }
            )
            VerticalSpace.x16()
        }
    }
}
