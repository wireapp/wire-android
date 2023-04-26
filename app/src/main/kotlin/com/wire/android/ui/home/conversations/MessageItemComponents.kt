package com.wire.android.ui.home.conversations

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
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
internal fun MessageSentPartialDeliveryFailures(partialDeliveryFailureContent: DeliveryStatusContent.PartialDelivery) {
    val resources = LocalContext.current.resources
    var expanded: Boolean by remember { mutableStateOf(false) }
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.error)
    ) {
        Column {
            Text(
                text = stringResource(
                    id = R.string.label_message_partial_delivery_participants_count,
                    partialDeliveryFailureContent.totalUsersWithFailures
                ),
                textAlign = TextAlign.Start
            )
            VerticalSpace.x4()
            if (expanded) {
                if (partialDeliveryFailureContent.noClients.isNotEmpty()) {
                    // map to domain count
                    Text(
                        text = stringResource(
                            id = R.string.label_message_partial_delivery_participants_wont_deliver,
                            partialDeliveryFailureContent.noClients.joinToString(", ") { it.asString(resources) }
                        ),
                        textAlign = TextAlign.Start
                    )
                }
                if (partialDeliveryFailureContent.failedRecipients.isNotEmpty()) {
                    // ignore users without metadata
                    Text(
                        text = stringResource(
                            id = R.string.label_message_partial_delivery_participants_deliver_later,
                            partialDeliveryFailureContent.failedRecipients.joinToString(", ") { it.asString(resources) }
                        ),
                        textAlign = TextAlign.Start
                    )
                }
            }
            VerticalSpace.x4()
            if (partialDeliveryFailureContent.expandable) {
                WireSecondaryButton(
                    onClick = { expanded = !expanded },
                    text = stringResource(if (expanded) R.string.label_hide_details else R.string.label_show_details),
                    fillMaxWidth = false,
                    minHeight = dimensions().spacing32x,
                    minWidth = dimensions().spacing40x,
                    shape = RoundedCornerShape(size = dimensions().corner12x),
                    contentPadding = PaddingValues(horizontal = dimensions().spacing12x, vertical = dimensions().spacing8x),
                    modifier = Modifier
                        .padding(top = dimensions().spacing4x)
                        .height(height = dimensions().spacing32x)
                )
            }
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
