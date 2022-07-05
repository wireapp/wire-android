package com.wire.android.ui.userprofile.other

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.CopyButton
import com.wire.android.ui.common.MoreOptionIcon
import com.wire.android.ui.common.RowItemTemplate
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.common.EditableState
import com.wire.android.ui.userprofile.common.UserProfileInfo

@Composable
fun OtherUserProfileScreen(viewModel: OtherUserProfileScreenViewModel = hiltViewModel()) {
    OtherProfileScreenContent(
        state = viewModel.state,
        operationState = viewModel.connectionOperationState,
        onSendConnectionRequest = viewModel::sendConnectionRequest,
        onOpenConversation = viewModel::openConversation,
        onCancelConnectionRequest = viewModel::cancelConnectionRequest,
        ignoreConnectionRequest = viewModel::ignoreConnectionRequest,
        acceptConnectionRequest = viewModel::acceptConnectionRequest,
        onNavigateBack = viewModel::navigateBack
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OtherProfileScreenContent(
    state: OtherUserProfileState,
    operationState: ConnectionOperationState?,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
    onNavigateBack: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }
    val otherUserProfileScreenState = rememberOtherUserProfileScreenState(snackbarHostState)

    handleOperationMessages(snackbarHostState, operationState)

    Scaffold(
        topBar = {
            OtherUserProfileTopBar(
                onNavigateBack = onNavigateBack,
                connectionStatus = state.connectionStatus
            )
        },
        snackbarHost = {
            SwipeDismissSnackbarHost(
                hostState = otherUserProfileScreenState.snackbarHostState,
                modifier = Modifier.fillMaxWidth()
            )
        }
    ) { internalPadding ->
        with(state) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .fillMaxHeight()
                    .padding(internalPadding)
                    .background(MaterialTheme.colorScheme.background)
            ) {
                LazyColumn(modifier = Modifier.weight(1f)) {
                    item {
                        UserProfileInfo(
                            isLoading = state.isAvatarLoading,
                            avatarAsset = state.userAvatarAsset,
                            fullName = fullName,
                            userName = userName,
                            teamName = teamName,
                            membership = membership,
                            editableState = EditableState.NotEditable
                        )
                    }

                    if (connectionStatus is ConnectionStatus.Connected) {
                        item {
                            if (state.email.isNotEmpty()) {
                                UserDetailInformation(
                                    title = stringResource(R.string.email_label),
                                    value = state.email,
                                    onCopy = { otherUserProfileScreenState.copy(it) }
                                )
                            }
                        }

                        if (state.phone.isNotEmpty()) {
                            item {
                                UserDetailInformation(
                                    title = stringResource(R.string.phone_label),
                                    value = state.phone,
                                    onCopy = { otherUserProfileScreenState.copy(it) }
                                )
                            }
                        }
                    } else {
                        item {
                            ConnectionStatusInformation(state.connectionStatus)
                        }
                    }
                }
                Divider()
                Box(
                    modifier = Modifier
                        .padding(all = dimensions().spacing16x)
                ) {
                    ConnectionActionButton(
                        connectionStatus,
                        onSendConnectionRequest,
                        onOpenConversation,
                        onCancelConnectionRequest,
                        acceptConnectionRequest,
                        ignoreConnectionRequest
                    )
                }
            }
        }
    }
}

@Composable
private fun ConnectionActionButton(
    connectionStatus: ConnectionStatus,
    onSendConnectionRequest: () -> Unit,
    onOpenConversation: () -> Unit,
    onCancelConnectionRequest: () -> Unit,
    acceptConnectionRequest: () -> Unit,
    ignoreConnectionRequest: () -> Unit,
) {
    when (connectionStatus) {
        is ConnectionStatus.Sent -> WireSecondaryButton(
            text = stringResource(R.string.connection_label_cancel_request),
            onClick = onCancelConnectionRequest
        )
        is ConnectionStatus.Connected -> WirePrimaryButton(
            text = stringResource(R.string.label_open_conversation),
            onClick = onOpenConversation,
        )
        is ConnectionStatus.Pending -> Column {
            WirePrimaryButton(
                text = stringResource(R.string.connection_label_accept),
                onClick = acceptConnectionRequest,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_check_tick),
                        contentDescription = stringResource(R.string.content_description_right_arrow),
                        modifier = Modifier.padding(dimensions().spacing8x)
                    )
                }
            )
            Spacer(modifier = Modifier.height(8.dp))
            WirePrimaryButton(
                text = stringResource(R.string.connection_label_ignore),
                state = WireButtonState.Error,
                onClick = ignoreConnectionRequest,
                leadingIcon = {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_close),
                        contentDescription = stringResource(R.string.content_description_right_arrow),
                    )
                }
            )
        }
        else -> WirePrimaryButton(
            text = stringResource(R.string.connection_label_connect),
            onClick = onSendConnectionRequest,
            leadingIcon = {
                Icon(
                    painter = painterResource(id = R.drawable.ic_add_contact),
                    contentDescription = stringResource(R.string.content_description_right_arrow),
                    modifier = Modifier.padding(dimensions().spacing8x)
                )
            }
        )

    }
}

@Composable
private fun ConnectionStatusInformation(connectionStatus: ConnectionStatus) {
    Box(
        Modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .padding(dimensions().spacing32x)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            if (connectionStatus is ConnectionStatus.Pending)
                Text(
                    text = stringResource(R.string.connection_label_user_wants_to_conect),
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.wireColorScheme.onSurface,
                    style = MaterialTheme.wireTypography.title02
                )
            Spacer(modifier = Modifier.height(24.dp))
            val descriptionResource = when (connectionStatus) {
                ConnectionStatus.Pending -> R.string.connection_label_accepting_request_description
                ConnectionStatus.Connected -> throw IllegalStateException("Unhandled Connected ConnectionStatus")
                else -> R.string.connection_label_member_not_belongs_to_team
            }
            Text(
                text = stringResource(descriptionResource),
                textAlign = TextAlign.Center,
                color = MaterialTheme.wireColorScheme.labelText,
                style = MaterialTheme.wireTypography.body01
            )
        }
    }
}

@Composable
private fun UserDetailInformation(
    title: String,
    value: String,
    onCopy: (String) -> Unit
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.subline01,
                color = MaterialTheme.wireColorScheme.labelText,
                text = title.uppercase()
            )
        },
        subtitle = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = value
            )
        },
        actions = { CopyButton(onCopyClicked = { onCopy("$value copied") }) },
        clickable = Clickable(enabled = false) {}
    )
}

@Composable
fun OtherUserProfileTopBar(
    onNavigateBack: () -> Unit,
    connectionStatus: ConnectionStatus
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onNavigateBack,
        title = stringResource(id = R.string.user_profile_title),
        elevation = 0.dp,
        actions = {
            if (connectionStatus is ConnectionStatus.Connected) {
                MoreOptionIcon({ })
            }
        }
    )
}

@Composable
private fun handleOperationMessages(
    snackbarHostState: SnackbarHostState,
    operationState: ConnectionOperationState?
) {
    operationState?.let { errorType ->
        val message = when (errorType) {
            is ConnectionOperationState.ConnectionRequestError -> stringResource(id = R.string.connection_request_sent_error)
            is ConnectionOperationState.SuccessConnectionSentRequest -> stringResource(id = R.string.connection_request_sent)
            is ConnectionOperationState.LoadUserInformationError -> stringResource(id = R.string.error_unknown_message)
            is ConnectionOperationState.SuccessConnectionAcceptRequest -> stringResource(id = R.string.connection_request_accepted)
            is ConnectionOperationState.SuccessConnectionCancelRequest -> stringResource(id = R.string.connection_request_canceled)
        }
        LaunchedEffect(errorType) {
            snackbarHostState.showSnackbar(message)
        }
    }
}
