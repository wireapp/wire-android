package com.wire.android.ui.home.newconversation

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
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.wire.android.R
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.home.newconversation.common.Screen
import com.wire.android.ui.home.newconversation.groupOptions.GroupOptionScreen
import com.wire.android.ui.home.newconversation.newgroup.NewGroupScreen
import com.wire.android.ui.home.conversations.search.SearchPeopleRouter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NewConversationRouter() {
    val newConversationViewModel: NewConversationViewModel = hiltViewModel()
    val newConversationNavController = rememberNavController()
    val snackbarHostState = remember { SnackbarHostState() }

    handleSnackBarMessage(
        snackbarHostState,
        newConversationViewModel.snackbarMessageState
    ) { newConversationViewModel.clearSnackbarMessage() }

    Scaffold(
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        }) { internalPadding ->
        NavHost(
            navController = newConversationNavController,
            startDestination = Screen.SearchListNavHostScreens.route,
            modifier = Modifier.padding(internalPadding)
        ) {
            composable(
                route = Screen.SearchListNavHostScreens.route,
                content = {
                    SearchPeopleRouter(
                        searchBarTitle = stringResource(id = R.string.label_new_conversation),
                    searchPeopleViewModel = newConversationViewModel,
                        onPeoplePicked = { newConversationNavController.navigate(Screen.NewGroupNameScreen.route) },

                    )
                }
            )
            composable(
                route = Screen.NewGroupNameScreen.route,
                content = {
                    NewGroupScreen(
                        onBackPressed = newConversationNavController::popBackStack,
                        newGroupState = newConversationViewModel.groupNameState,
                        onGroupNameChange = newConversationViewModel::onGroupNameChange,
                        onContinuePressed = { newConversationNavController.navigate(Screen.GroupOptionsScreen.route) },
                        onGroupNameErrorAnimated = newConversationViewModel::onGroupNameErrorAnimated
                    )
                }
            )

            composable(
                route = Screen.GroupOptionsScreen.route,
                content = {
                    GroupOptionScreen(
                        onBackPressed = newConversationNavController::popBackStack,
                        onCreateGroup = newConversationViewModel::createGroupConversation,
                        groupOptionState = newConversationViewModel.groupOptionsState,
                        onAllowGuestChanged = newConversationViewModel::onAllowGuestStatusChanged,
                        onAllowServicesChanged = newConversationViewModel::onAllowServicesStatusChanged,
                        onReadReceiptChanged = newConversationViewModel::onReadReceiptStatusChanged,
                        onAllowGuestsDialogDismissed = newConversationViewModel::onAllowGuestsDialogDismissed,
                        onAllowGuestsClicked = newConversationViewModel::onAllowGuestsClicked,
                        onNotAllowGuestsClicked = newConversationViewModel::onNotAllowGuestClicked
                    )
                }
            )
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
