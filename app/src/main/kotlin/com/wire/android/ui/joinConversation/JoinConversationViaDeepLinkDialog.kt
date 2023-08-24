@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.joinConversation

import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.tooling.preview.Preview
import com.sebaslogen.resaca.hilt.hiltViewModelScoped
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.textfield.WirePasswordTextField
import com.wire.android.ui.common.textfield.WireTextFieldState
import com.wire.android.ui.common.wireDialogPropertiesBuilder
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.capitalizeFirstLetter
import com.wire.android.util.toTitleCase
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase

sealed interface JoinConversationViaCodeState {
    data class Show(
        val conversationName: String?,
        val code: String,
        val key: String,
        val domain: String?,
        val passwordProtected: Boolean
    ) : JoinConversationViaCodeState

    data class Error(val error: CheckConversationInviteCodeUseCase.Result.Failure) : JoinConversationViaCodeState
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun JoinConversationViaDeepLinkDialog(
    name: String?,
    code: String,
    key: String,
    domain: String?,
    requirePassword: Boolean,
    onFlowCompleted: (conversationId: ConversationId?) -> Unit
) {
    val viewModel = hiltViewModelScoped<JoinConversationViaCodeViewModel>()

    val isLoading: Boolean by remember {
        derivedStateOf { viewModel.state is JoinViaDeepLinkDialogState.Loading }
    }

    val joinButtonState = when {
        isLoading -> WireButtonState.Disabled
        viewModel.state is JoinViaDeepLinkDialogState.WrongPassword -> WireButtonState.Disabled
        viewModel.state is JoinViaDeepLinkDialogState.UnknownError -> WireButtonState.Disabled
        requirePassword && viewModel.password.text.isBlank() -> WireButtonState.Disabled
        else -> WireButtonState.Default
    }
    val onJoinClick: () -> Unit = remember {
        {
            viewModel.joinConversationViaCode(
                code = code,
                key = key,
                domain = domain
            )
        }
    }

    val onCancel: () -> Unit = remember(viewModel.state is JoinViaDeepLinkDialogState.Success) {
        {
            if (viewModel.state !is JoinViaDeepLinkDialogState.Success) {
                onFlowCompleted(null)
            } else {
                onFlowCompleted((viewModel.state as JoinViaDeepLinkDialogState.Success).convId)
            }
        }
    }
    if (viewModel.state is JoinViaDeepLinkDialogState.Success) {
        onCancel()
    }

    var keyboardController: SoftwareKeyboardController?

    WireDialog(
        title = stringResource(R.string.join_conversation_dialog_title),
        text = LocalContext.current.resources.stringWithStyledArgs(
            R.string.join_conversation_dialog_message,
            MaterialTheme.wireTypography.body01,
            MaterialTheme.wireTypography.body02,
            colorsScheme().onBackground,
            colorsScheme().onBackground,
            name.orEmpty()
        ),

        buttonsHorizontalAlignment = true,
        onDismiss = onCancel,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onCancel,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onJoinClick,
            text = stringResource(R.string.join_conversation_dialog_button),
            type = WireDialogButtonType.Primary,
            state = joinButtonState,
            loading = isLoading
        ),
        content = {
            if (requirePassword) {
                // keyboard controller from outside the Dialog doesn't work inside its content so we have to pass the state
                // to the dialog's content and use keyboard controller from there
                keyboardController = LocalSoftwareKeyboardController.current
                val focusRequester = remember { FocusRequester() }
                WirePasswordTextField(
                    labelText = stringResource(id = R.string.join_conversation_dialog_password_label).toTitleCase(),
                    placeholderText = stringResource(id = R.string.join_conversation_dialog_password_placeholder).capitalizeFirstLetter(),
                    value = viewModel.password,
                    onValueChange = viewModel::onPasswordUpdated,
                    state = when {
                        isLoading -> WireTextFieldState.Disabled
                        viewModel.state is JoinViaDeepLinkDialogState.WrongPassword ->
                            WireTextFieldState.Error(stringResource(id = R.string.remove_device_invalid_password))

                        else -> WireTextFieldState.Default
                    },
                    imeAction = ImeAction.Done,
                    keyboardActions = KeyboardActions(onDone = { keyboardController?.hide() }),
                    autofillTypes = emptyList(),
                    modifier = Modifier
                        .focusRequester(focusRequester)
                        .padding(bottom = MaterialTheme.wireDimensions.spacing8x)
                        .testTag("remove device password field")
                )
                LaunchedEffect(Unit) { // executed only once when showing the dialog
                    focusRequester.requestFocus()
                }
            }
        },
        properties = wireDialogPropertiesBuilder(
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    )
}

@Preview
@Composable
fun JoinConversationViaDeepLinkDialogPreview() {
    JoinConversationViaDeepLinkDialog(
        onFlowCompleted = { _ -> },
        requirePassword = true,
        name = "Test",
        code = "123",
        key = "123",
        domain = "test.com"
    )
}
