package com.wire.android.ui.home.newconversation

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import cafe.adriel.voyager.navigator.CurrentScreen
import cafe.adriel.voyager.navigator.Navigator
import com.wire.android.R
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.conversations.search.NewConversationSnackbarState
import com.wire.android.ui.home.newconversation.common.NewConversationScreen

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationRouter(newConversationViewModel: NewConversationViewModel) {
    val snackbarHostState = remember { SnackbarHostState() }

    handleSnackBarMessage(
        snackbarHostState,
        newConversationViewModel.snackbarMessageState
    ) { newConversationViewModel.clearSnackbarMessage() }

    Navigator(
        screen = NewConversationScreen.SearchList(
            searchBarTitle = stringResource(id = R.string.label_new_conversation),
            newConversationViewModel = newConversationViewModel
        )
    ) {
        Scaffold(
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.fillMaxWidth()
                )
            }) { internalPadding ->
            Column(modifier = Modifier.padding(internalPadding)) {
                CurrentScreen()
            }
        }
    }
}

@Composable
private fun handleSnackBarMessage(
    snackbarHostState: SnackbarHostState,
    conversationListSnackBarState: NewConversationSnackbarState,
    onMessageShown: () -> Unit
) {
    conversationListSnackBarState.let { messageType ->
        val message = when (messageType) {
            is NewConversationSnackbarState.SuccessSendConnectionRequest ->
                stringResource(id = R.string.connection_request_sent)
            NewConversationSnackbarState.None -> ""
        }
        LaunchedEffect(messageType) {
            if (messageType != NewConversationSnackbarState.None) {
                snackbarHostState.showSnackbar(message)
                onMessageShown()
            }
        }
    }
}
