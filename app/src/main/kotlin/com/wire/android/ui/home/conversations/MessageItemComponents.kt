package com.wire.android.ui.home.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.home.conversations.model.MessageStatus
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.CustomTabsHelper
import com.wire.kalium.logic.data.user.UserId

@Composable
internal fun MessageSendFailureWarning(
    messageStatus: MessageStatus.MessageSendFailureStatus
    /* TODO: add onRetryClick handler */
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_message_details_offline_backends_learn_more)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Column {
            Text(
                text = messageStatus.errorText.asString(),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            if (messageStatus is MessageStatus.SendRemotelyFailure) {
                Text(
                    modifier = Modifier
                        .clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
                    style = LocalTextStyle.current.copy(
                        color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                        textDecoration = TextDecoration.Underline
                    ),
                    text = stringResource(R.string.label_learn_more)
                )
            }
            // TODO: re-enable when we have a retry mechanism
//            VerticalSpace.x4()
//            Row {
//                WireSecondaryButton(
//                    text = stringResource(R.string.label_retry),
//                    onClick = { /* TODO */ },
//                    minHeight = dimensions().spacing32x,
//                    fillMaxWidth = false
//                )
//            }
        }
    }
}

@Composable
internal fun MessageDecryptionFailure(
    messageHeader: MessageHeader,
    decryptionStatus: MessageStatus.DecryptionFailure,
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit
) {
    val context = LocalContext.current
    val learnMoreUrl = stringResource(R.string.url_decryption_failure_learn_more)
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Column {
            VerticalSpace.x4()
            Text(
                text = decryptionStatus.errorText.asString(),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            Text(
                modifier = Modifier
                    .clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
                style = LocalTextStyle.current.copy(
                    color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                    textDecoration = TextDecoration.Underline
                ),
                text = stringResource(R.string.label_learn_more)
            )
            VerticalSpace.x4()
            Text(
                text = stringResource(R.string.label_message_decryption_failure_informative_message),
                style = LocalTextStyle.current.copy(color = MaterialTheme.colorScheme.error)
            )
            if (!decryptionStatus.isDecryptionResolved) {
                Row {
                    WireSecondaryButton(
                        text = stringResource(R.string.label_reset_session),
                        onClick = {
                            messageHeader.userId?.let { userId ->
                                onResetSessionClicked(
                                    userId,
                                    messageHeader.clientId?.value
                                )
                            }
                        },
                        minHeight = dimensions().spacing32x,
                        fillMaxWidth = false
                    )
                }
            } else {
                VerticalSpace.x8()
            }
        }
    }
}

@Composable
internal fun Modifier.customizeMessageBackground(
    sendingFailed: Boolean,
    receivingFailed: Boolean
) = run {
    if (sendingFailed || receivingFailed) {
        background(MaterialTheme.wireColorScheme.messageErrorBackgroundColor)
    } else {
        this
    }
}
