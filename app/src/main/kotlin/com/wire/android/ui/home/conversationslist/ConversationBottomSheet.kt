package com.wire.android.ui.home.conversationslist

import androidx.annotation.DrawableRes
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetLayout
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.TabRowDefaults.Divider
import androidx.compose.material3.Icon
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.CircularProgressIndicator
import com.wire.android.ui.common.UserProfileAvatar
import com.wire.android.ui.home.conversations.common.GroupConversationAvatar
import com.wire.android.ui.home.conversationslist.model.ConversationType.GroupConversation
import com.wire.android.ui.home.conversationslist.model.ConversationType.PrivateConversation
import com.wire.android.ui.theme.wireTypography


@ExperimentalMaterialApi
@Composable
fun ConversationModalBottomSheet(
    modalBottomSheetState: ModalBottomSheetState,
    modalSheetContentState: ModalSheetContentState,
    content: @Composable () -> Unit
) {
    ModalBottomSheetLayout(
        sheetState = modalBottomSheetState,
        //TODO: create a shape object inside the materialtheme 3 component
        sheetShape = androidx.compose.material.MaterialTheme.shapes.large.copy(topStart = CornerSize(12.dp), topEnd = CornerSize(12.dp)),
        sheetContent = {
            Column {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(
                    modifier = Modifier
                        .width(width = 48.dp)
                        .align(alignment = Alignment.CenterHorizontally),
                    thickness = 4.dp
                )
                ModalBottomSheetHeader(modalSheetContentState)
                Divider()
                ModalBottomSheetItems()
            }
        }
    ) {
        content()
    }
}

@Composable
fun ModalBottomSheetItems() {
    LazyColumn {
        item {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_mute,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Mute") }
            )
        }
        item { Divider() }
        item {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_favourite,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Add to Favourites") }
            )
        }
        item { Divider() }
        item {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_folder,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Move to Folder") }
            )
        }
        item { Divider() }
        item {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_archive,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Move to Archive") }
            )
        }
        item { Divider() }
        item {
            ModalBottomSheetItem(
                icon = {
                    ItemIcon(
                        id = R.drawable.ic_erase,
                        contentDescription = "",
                    )
                },
                title = { ItemTitle("Clear Content...") }
            )
        }
        item { Divider() }
        item {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                ModalBottomSheetItem(
                    icon = {
                        ItemIcon(
                            id = R.drawable.ic_leave,
                            contentDescription = "",
                        )
                    },
                    title = { ItemTitle("Leave Group") }
                )
            }
        }
    }
}

@Composable
fun ModalBottomSheetHeader(modalSheetContentState: ModalSheetContentState) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.padding(
            start = 8.dp,
            top = 16.dp,
            bottom = 8.dp
        )
    ) {
        when (val avatar = modalSheetContentState.avatar.value) {
            is ModalSheetAvatar.GroupAvatar -> GroupConversationAvatar(colorValue = avatar.groupColor)
            is ModalSheetAvatar.UserAvatar -> UserProfileAvatar()
            ModalSheetAvatar.None -> CircularProgressIndicator(progressColor = Color.Blue)
        }
        Spacer(modifier = Modifier.width(8.dp))
        Text(
            text = modalSheetContentState.title.value,
            style = MaterialTheme.wireTypography.body02
        )
    }
}


//TODO: split components into two types
@Composable
private fun PrivateConversationModalBottomSheet(privateConversation: PrivateConversation) {

}

//TODO: split components into two types
@Composable
private fun GroupConversationModalBottomSheet(groupConversation: GroupConversation) {

}

@Composable
private fun ItemIcon(
    @DrawableRes id: Int,
    contentDescription: String,
    size: Dp = 16.dp,
    modifier: Modifier = Modifier
) {
    Icon(
        painter = painterResource(id = id),
        contentDescription = contentDescription,
        modifier = Modifier
            .size(size)
            .then(modifier)
    )
}

@Composable
private fun ItemTitle(
    title: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = title,
        style = MaterialTheme.wireTypography.body01,
        modifier = modifier
    )
}

@Composable
private fun ModalBottomSheetItem(
    icon: @Composable () -> Unit,
    title: @Composable () -> Unit,
    onItemClick: () -> Unit = {}
) {
    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier
        .height(48.dp)
        .fillMaxWidth()
        .clickable { onItemClick() }
        .padding(16.dp)
    ) {
        icon()
        Spacer(modifier = Modifier.width(12.dp))
        title()
    }
}

class ModalSheetContentState {
    val title: MutableState<String> = mutableStateOf("")
    val avatar: MutableState<ModalSheetAvatar> = mutableStateOf(ModalSheetAvatar.None)
}

sealed class ModalSheetAvatar {
    data class UserAvatar(val avatarUrl: String) : ModalSheetAvatar()
    data class GroupAvatar(val groupColor: Long) : ModalSheetAvatar()
    object None : ModalSheetAvatar()
}

@Composable
fun rememberModalSheetContentState(): ModalSheetContentState {
    return remember {
        ModalSheetContentState()
    }
}
