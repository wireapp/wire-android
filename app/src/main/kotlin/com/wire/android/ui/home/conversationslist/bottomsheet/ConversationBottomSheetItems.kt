package com.wire.android.ui.home.conversationslist.bottomsheet

import androidx.compose.material.TabRowDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.res.stringResource
import com.wire.android.R

@Suppress("LongParameterList")
fun commonConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit
) = listOf<@Composable () -> Unit>(
    {
        ModalBottomSheetItem(
            icon = {
                ItemIcon(
                    id = R.drawable.ic_mute,
                    contentDescription = stringResource(R.string.content_description_mute),
                )
            },
            title = { ItemTitle(stringResource(R.string.label_mute)) },
            onMuteClick
        )
    },
    {
        ModalBottomSheetItem(
            icon = {
                ItemIcon(
                    id = R.drawable.ic_favourite,
                    contentDescription = stringResource(R.string.content_description_add_to_favourite),
                )
            },
            title = { ItemTitle(stringResource(R.string.label_add_to_favourites)) },
            onAddToFavouritesClick
        )
    },
    {
        ModalBottomSheetItem(
            icon = {
                ItemIcon(
                    id = R.drawable.ic_folder,
                    contentDescription = stringResource(R.string.content_description_move_to_folder),
                )
            },
            title = { ItemTitle(stringResource(R.string.label_move_to_folder)) },
            onMoveToFolderClick
        )
    },
    {
        ModalBottomSheetItem(
            icon = {
                ItemIcon(
                    id = R.drawable.ic_archive,
                    contentDescription = stringResource(R.string.content_description_move_to_archive),
                )
            },
            title = { ItemTitle(stringResource(R.string.label_move_to_archive)) },
            onMoveToArchiveClick
        )
    }, {
        ModalBottomSheetItem(
            icon = {
                ItemIcon(
                    id = R.drawable.ic_erase,
                    contentDescription = stringResource(R.string.content_description_clear_content),
                )
            },
            title = { ItemTitle(stringResource(R.string.label_clear_content)) },
            onClearContentClick
        )
    }
)

@Composable
fun GroupConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onLeaveClick: () -> Unit
) {
    buildItems(
        items = groupConversationItems(
            onMuteClick = onMuteClick,
            onAddToFavouritesClick = onAddToFavouritesClick,
            onMoveToFolderClick = onMoveToFolderClick,
            onMoveToArchiveClick = onMoveToArchiveClick,
            onClearContentClick = onClearContentClick,
            onLeaveClick = onLeaveClick,
        )
    )
}


@Suppress("LongParameterList")
fun groupConversationItems(
    onMuteClick: () -> Unit,
    onAddToFavouritesClick: () -> Unit,
    onMoveToFolderClick: () -> Unit,
    onMoveToArchiveClick: () -> Unit,
    onClearContentClick: () -> Unit,
    onLeaveClick: () -> Unit
): List<@Composable () -> Unit> {
    return commonConversationItems(
        onMuteClick = onMuteClick,
        onAddToFavouritesClick = onAddToFavouritesClick,
        onMoveToFolderClick = onMoveToFolderClick,
        onMoveToArchiveClick = onMoveToArchiveClick,
        onClearContentClick = onClearContentClick
    ) + {
        CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_leave,
                        contentDescription = stringResource(R.string.content_description_leave_the_group),
                    )
                },
                title = { ItemTitle(stringResource(R.string.label_leave_group)) },
                onLeaveClick
            )
        }
    }
}

@Composable
fun buildItems(items: List<@Composable () -> Unit>) {
    TabRowDefaults.Divider()
    items.forEachIndexed { index, sheetItem ->
        sheetItem()
        if (index != items.size) {
            TabRowDefaults.Divider()
        }
    }
}
