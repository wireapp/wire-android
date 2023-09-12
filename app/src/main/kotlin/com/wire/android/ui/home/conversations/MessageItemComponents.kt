package com.wire.android.ui.home.conversations

import android.content.Context
import android.content.res.Resources
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
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
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.mock.mockHeader
import com.wire.android.ui.home.conversations.model.DeliveryStatusContent
import com.wire.android.ui.home.conversations.model.MessageFlowStatus
import com.wire.android.ui.home.conversations.model.MessageHeader
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.CustomTabsHelper
import com.wire.android.util.EMPTY
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Composable
internal fun MessageSendFailureWarning(
    messageStatus: MessageFlowStatus.Failure.Send,
    onRetryClick: () -> Unit,
    onCancelClick: () -> Unit
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Column {
            VerticalSpace.x4()
            Text(
                text = messageStatus.errorText.asString(),
                style = LocalTextStyle.current,
                color = MaterialTheme.colorScheme.error
            )
            if (messageStatus is MessageFlowStatus.Failure.Send.Remotely) {
                OfflineBackendsLearnMoreLink()
            }
            Row {
                WireSecondaryButton(
                    text = stringResource(R.string.label_retry),
                    onClick = onRetryClick,
                    minSize = dimensions().buttonSmallMinSize,
                    minClickableSize = dimensions().buttonSmallMinClickableSize,
                    fillMaxWidth = false
                )
                HorizontalSpace.x8()
                WireSecondaryButton(
                    text = stringResource(R.string.label_cancel),
                    onClick = onCancelClick,
                    minSize = dimensions().buttonSmallMinSize,
                    minClickableSize = dimensions().buttonSmallMinClickableSize,
                    fillMaxWidth = false
                )
            }
        }
    }
}

@Composable
internal fun MessageSentPartialDeliveryFailures(partialDeliveryFailureContent: DeliveryStatusContent.PartialDelivery) {
    val resources = LocalContext.current.resources
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall.copy(color = MaterialTheme.wireColorScheme.error)
    ) {
        if (partialDeliveryFailureContent.isSingleUserFailure) {
            SingleUserDeliveryFailure(partialDeliveryFailureContent, resources)
        } else {
            MultiUserDeliveryFailure(partialDeliveryFailureContent, resources)
        }
    }
}

@Composable
private fun MultiUserDeliveryFailure(
    partialDeliveryFailureContent: DeliveryStatusContent.PartialDelivery,
    resources: Resources
) {
    var expanded: Boolean by remember { mutableStateOf(false) }
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
            if (partialDeliveryFailureContent.filteredRecipientsFailure.isNotEmpty()) {
                Text(
                    text = stringResource(
                        id = R.string.label_message_partial_delivery_participants_many_deliver_later,
                        partialDeliveryFailureContent.filteredRecipientsFailure
                            .filter {
                                !it.asString(resources).contentEquals(resources.getString(R.string.username_unavailable_label))
                            }
                            .joinToString(", ") { it.asString(resources) }
                    ),
                    textAlign = TextAlign.Start
                )
            }
            if (partialDeliveryFailureContent.noClients.isNotEmpty()) {
                Text(
                    text = partialDeliveryFailureContent.noClients.entries.map {
                        pluralStringResource(
                            R.plurals.label_message_partial_delivery_x_participants_from_backend,
                            it.value.size,
                            it.value.size,
                            it.key
                        )
                    }.joinToString(", ") + stringResource(
                        R.string.label_message_partial_delivery_participants_wont_deliver,
                        String.EMPTY
                    ),
                    textAlign = TextAlign.Start
                )
            }
            OfflineBackendsLearnMoreLink()
        }
        VerticalSpace.x4()
        if (partialDeliveryFailureContent.expandable) {
            WireSecondaryButton(
                onClick = { expanded = !expanded },
                text = stringResource(if (expanded) R.string.label_hide_details else R.string.label_show_details),
                fillMaxWidth = false,
                minSize = dimensions().buttonSmallMinSize,
                minClickableSize = dimensions().buttonSmallMinClickableSize,
                shape = RoundedCornerShape(size = dimensions().corner12x),
                contentPadding = PaddingValues(horizontal = dimensions().spacing12x, vertical = dimensions().spacing8x),
            )
        }
    }
}

@Composable
private fun SingleUserDeliveryFailure(
    partialDeliveryFailureContent: DeliveryStatusContent.PartialDelivery,
    resources: Resources
) {
    Column {
        if (partialDeliveryFailureContent.failedRecipients.isNotEmpty()) {
            Text(
                text = stringResource(
                    id = R.string.label_message_partial_delivery_participants_one_deliver_later,
                    partialDeliveryFailureContent.failedRecipients.joinToString(", ") {
                        it.asString(resources).ifEmpty { resources.getString(R.string.username_unavailable_label) }
                    }
                ),
                textAlign = TextAlign.Start
            )
        }
        if (partialDeliveryFailureContent.noClients.isNotEmpty()) {
            Text(
                text = partialDeliveryFailureContent.noClients.entries.map {
                    pluralStringResource(
                        R.plurals.label_message_partial_delivery_x_participants_from_backend,
                        it.value.size,
                        it.value.size,
                        it.key
                    )
                }.joinToString(", ") + stringResource(
                    R.string.label_message_partial_delivery_participants_wont_deliver,
                    String.EMPTY
                ),
                textAlign = TextAlign.Start
            )
        }
        OfflineBackendsLearnMoreLink()
        VerticalSpace.x4()
    }
}

@Composable
internal fun MessageDecryptionFailure(
    messageHeader: MessageHeader,
    decryptionStatus: MessageFlowStatus.Failure.Decryption,
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
                style = LocalTextStyle.current,
                color = MaterialTheme.colorScheme.error
            )
            Text(
                modifier = Modifier
                    .clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
                style = LocalTextStyle.current,
                textDecoration = TextDecoration.Underline,
                color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
                text = stringResource(R.string.label_learn_more)
            )
            VerticalSpace.x4()
            Text(
                text = stringResource(R.string.label_message_decryption_failure_informative_message),
                style = LocalTextStyle.current,
                color = MaterialTheme.colorScheme.error
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
                        minSize = dimensions().buttonSmallMinSize,
                        minClickableSize = dimensions().buttonSmallMinClickableSize,
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

@Composable
internal fun OfflineBackendsLearnMoreLink(context: Context = LocalContext.current) {
    val learnMoreUrl = stringResource(R.string.url_message_details_offline_backends_learn_more)
    VerticalSpace.x4()
    Text(
        modifier = Modifier.clickable { CustomTabsHelper.launchUrl(context, learnMoreUrl) },
        style = LocalTextStyle.current.copy(
            color = MaterialTheme.wireColorScheme.onTertiaryButtonSelected,
            textDecoration = TextDecoration.Underline
        ),
        text = stringResource(R.string.label_learn_more)
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageSendFailureWarning() {
    WireTheme {
        MessageSendFailureWarning(MessageFlowStatus.Failure.Send.Locally(false), {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageDecryptionFailure() {
    WireTheme {
        MessageDecryptionFailure(mockHeader, MessageFlowStatus.Failure.Decryption(false)) { _, _ -> }
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMultiUserDeliveryFailure() {
    WireTheme {
        MultiUserDeliveryFailure(
            DeliveryStatusContent.PartialDelivery(
                failedRecipients = persistentListOf(UIText.DynamicString("username")),
                noClients = persistentMapOf(
                    "iOS" to listOf(UIText.DynamicString("ios")),
                    "Android" to listOf(UIText.DynamicString("android"))
                ),
            ),
            LocalContext.current.resources
        )
    }
}
