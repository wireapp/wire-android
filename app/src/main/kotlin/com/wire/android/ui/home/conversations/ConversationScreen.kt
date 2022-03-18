package com.wire.android.ui.home.conversations

import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ModalBottomSheetState
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.home.conversations.mock.mockMessages
import com.wire.android.ui.home.conversations.model.Message
import com.wire.android.ui.home.conversations.model.MessageSource
import com.wire.android.ui.home.messagecomposer.MessageComposer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConversationScreen(
    conversationViewModel: ConversationViewModel,
) {
    val uiState = conversationViewModel.conversationViewState

    ConversationScreen(
        conversationViewState = uiState,
        onMessageChanged = { message -> conversationViewModel.onMessageChanged(message) },
        onSendButtonClicked = { conversationViewModel.sendMessage() },
        onBackButtonClick = { conversationViewModel.navigateBack() },
    )
}

@OptIn(ExperimentalMaterial3Api::class, ExperimentalMaterialApi::class)
@Composable
private fun ConversationScreen(
    conversationViewState: ConversationViewState,
    onMessageChanged: (TextFieldValue) -> Unit,
    onSendButtonClicked: () -> Unit,
    onBackButtonClick: () -> Unit
) {
    val conversationScreenState = rememberConversationScreenState()

    with(conversationViewState) {
        MenuModalSheetLayout(
            sheetState = conversationScreenState.modalBottomSheetState,
            menuItems = EditMessageMenuItems(conversationScreenState.editMessageSource),
            content = {
                Scaffold(
                    topBar = {
                        ConversationScreenTopAppBar(
                            title = conversationName,
                            onBackButtonClick = onBackButtonClick,
                            onDropDownClick = {},
                            onSearchButtonClick = {},
                            onVideoButtonClick = {}
                        )
                    },
                    content = {
                        ConversationScreenContent(
                            messages = messages,
                            onMessageChanged = onMessageChanged,
                            messageText = conversationViewState.messageText,
                            onSendButtonClicked = onSendButtonClicked,
                            onShowContextMenu = { message -> conversationScreenState.showEditContextMenu(message) }
                        )
                    }
                )
            }
        )
    }
}

@Composable
private fun EditMessageMenuItems(editMessageSource: MessageSource?): List<@Composable () -> Unit> {
    return buildList {
        add {
            MenuBottomSheetItem(
                icon = {
                    MenuItemIcon(
                        id = R.drawable.ic_copy,
                        contentDescription = stringResource(R.string.content_description_block_the_user),
                    )
                },
                title = stringResource(R.string.label_copy),
            )
        }
        if (editMessageSource == MessageSource.CurrentUser)
            add {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_edit,
                            contentDescription = stringResource(R.string.content_description_block_the_user),
                        )
                    },
                    title = stringResource(R.string.label_edit),
                )
            }
        add {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.content_description_block_the_user),
                        )
                    },
                    title = stringResource(R.string.label_delete),
                )
            }
        }
    }
}

@Composable
private fun ConversationScreenContent(
    messages: List<Message>,
    onMessageChanged: (TextFieldValue) -> Unit,
    messageText: TextFieldValue,
    onSendButtonClicked: () -> Unit,
    onShowContextMenu: (Message) -> Unit,
) {
    MessageComposer(
        content = {
            LazyColumn(
                reverseLayout = true,
                modifier = Modifier
                    .fillMaxHeight()
                    .fillMaxWidth()
            ) {
                items(messages) { message ->
                    MessageItem(
                        message = message,
                        onLongClicked = { onShowContextMenu(message) }
                    )
                }
            }
        },
        messageText = messageText,
        onMessageChanged = onMessageChanged,
        onSendButtonClicked = onSendButtonClicked
    )
}

@Preview
@Composable
fun ConversationScreenPreview() {
    ConversationScreen(
        ConversationViewState(
            conversationName = "Some test conversation",
            messages = mockMessages,
        ), {}, {}, {})
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
fun rememberConversationScreenState(
    bottomSheetState: ModalBottomSheetState = rememberModalBottomSheetState(initialValue = ModalBottomSheetValue.Hidden),
    coroutineScope: CoroutineScope = rememberCoroutineScope()
): ConversationScreenState {
    return remember {
        ConversationScreenState(
            modalBottomSheetState = bottomSheetState,
            coroutineScope = coroutineScope
        )
    }
}

@OptIn(ExperimentalMaterialApi::class)
class ConversationScreenState(
    val modalBottomSheetState: ModalBottomSheetState,
    val coroutineScope: CoroutineScope
) {

    private var editMessage by mutableStateOf<Message?>(null)

    val editMessageSource by derivedStateOf {
        editMessage?.messageSource
    }

    fun showEditContextMenu(message: Message) {
        editMessage = message
        coroutineScope.launch { modalBottomSheetState.animateTo(ModalBottomSheetValue.Expanded) }
    }

}

