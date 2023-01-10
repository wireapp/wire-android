@file:Suppress("MatchingDeclarationName")
package com.wire.android.ui.joinConversation

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.button.WireButtonState
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
        text = stringResource(R.string.join_conversation_dialog_message, state.conversationName.orEmpty()),
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
fun JoinConversationViaInviteLinkError(
    errorState: JoinConversationViaCodeState.Error
) {
    val context = LocalContext.current
    // TODO: checku[ with design about the error message copy
    Toast.makeText(context, "Failed to join conversation via deep link", Toast.LENGTH_LONG).show()
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
