@file:Suppress("MatchingDeclarationName")

package com.wire.android.ui.joinConversation

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.stringWithStyledArgs
import com.wire.kalium.logic.feature.conversation.CheckConversationInviteCodeUseCase

sealed interface JoinConversationViaCodeState {
    data class Show(
        val conversationName: String?,
        val code: String,
        val key: String,
        val domain: String?
    ) : JoinConversationViaCodeState

    data class Error(val error: CheckConversationInviteCodeUseCase.Result.Failure) : JoinConversationViaCodeState
}

@Composable
fun JoinConversationViaDeepLinkDialog(
    state: JoinConversationViaCodeState.Show,
    isLoading: Boolean,
    onJoinClick: (String, String, String?) -> Unit,
    onCancel: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.join_conversation_dialog_title),
        text = LocalContext.current.resources.stringWithStyledArgs(
            R.string.join_conversation_dialog_message,
            MaterialTheme.wireTypography.body01,
            MaterialTheme.wireTypography.body02,
            colorsScheme().onBackground,
            colorsScheme().onBackground,
            state.conversationName.orEmpty()
        ),

        buttonsHorizontalAlignment = true,
        onDismiss = onCancel,
        dismissButtonProperties = WireDialogButtonProperties(
            onClick = onCancel,
            text = stringResource(id = R.string.label_cancel),
            state = WireButtonState.Default
        ),
        optionButton1Properties = WireDialogButtonProperties(
            onClick = { onJoinClick(state.code, state.key, state.domain) },
            text = stringResource(R.string.join_conversation_dialog_button),
            type = WireDialogButtonType.Primary,
            state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
            loading = isLoading
        )
    )
}

@Composable
fun JoinConversationViaDeepLinkErrorDialog(
    errorMessage: String,
    onCancel: () -> Unit
) {
    WireDialog(
        title = stringResource(R.string.join_conversation_via_deeplink_error_title),
        text = errorMessage,
        onDismiss = onCancel,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onCancel,
            text = stringResource(id = R.string.label_ok),
            type = WireDialogButtonType.Primary
        )
    )
}

@Composable
fun JoinConversationViaInviteLinkError(
    errorState: JoinConversationViaCodeState.Error,
    onCancel: () -> Unit
) {
    when (errorState.error) {
        CheckConversationInviteCodeUseCase.Result.Failure.GuestLinksDisabled ->
            JoinConversationViaDeepLinkErrorDialog(
                stringResource(id = R.string.join_conversation_via_deeplink_error_link_expired),
                onCancel
            )
        CheckConversationInviteCodeUseCase.Result.Failure.AccessDenied ->
            JoinConversationViaDeepLinkErrorDialog(
                stringResource(id = R.string.join_conversation_via_deeplink_error_max_number_of_participent),
                onCancel
            )

        CheckConversationInviteCodeUseCase.Result.Failure.ConversationNotFound,
        CheckConversationInviteCodeUseCase.Result.Failure.InvalidCodeOrKey,
        CheckConversationInviteCodeUseCase.Result.Failure.RequestingUserIsNotATeamMember,
        is CheckConversationInviteCodeUseCase.Result.Failure.Generic ->
            JoinConversationViaDeepLinkErrorDialog(
                stringResource(id = R.string.join_conversation_via_deeplink_error_general),
                onCancel
            )
    }
}

@Preview
@Composable
fun JoinConversationViaDeepLinkDialogPreview() {
    JoinConversationViaDeepLinkDialog(
        isLoading = false,
        onCancel = {},
        onJoinClick = { _, _, _ -> },
        state = JoinConversationViaCodeState.Show("Conversation Name", "code", "key", "domain")
    )
}
