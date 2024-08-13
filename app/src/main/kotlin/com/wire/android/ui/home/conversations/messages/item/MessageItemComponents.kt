/*
 * Wire
 * Copyright (C) 2024 Wire Swiss GmbH
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see http://www.gnu.org/licenses/.
 */
package com.wire.android.ui.home.conversations.messages.item

import android.content.Context
import android.content.res.Resources
import androidx.compose.animation.Animatable
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import com.wire.android.R
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.home.conversations.SelfDeletionTimerHelper
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
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.user.UserId
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentMapOf

@Composable
internal fun MessageSendFailureWarning(
    messageStatus: MessageFlowStatus.Failure.Send,
    isInteractionAvailable: Boolean,
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
            if (isInteractionAvailable) {
                Row {
                    WireSecondaryButton(
                        text = stringResource(R.string.label_retry),
                        onClick = onRetryClick,
                        minSize = dimensions().buttonSmallMinSize,
                        minClickableSize = dimensions().buttonMinClickableSize,
                        fillMaxWidth = false
                    )
                    HorizontalSpace.x8()
                    WireSecondaryButton(
                        text = stringResource(R.string.label_cancel),
                        onClick = onCancelClick,
                        minSize = dimensions().buttonSmallMinSize,
                        minClickableSize = dimensions().buttonMinClickableSize,
                        fillMaxWidth = false
                    )
                }
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
                minClickableSize = dimensions().buttonMinClickableSize,
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
    onResetSessionClicked: (senderUserId: UserId, clientId: String?) -> Unit,
    conversationProtocol: Conversation.ProtocolInfo?
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

            if (conversationProtocol !is Conversation.ProtocolInfo.Proteus) return@Column

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
                        minClickableSize = dimensions().buttonMinClickableSize,
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
    defaultBackgroundColor: Color,
    sendingFailed: Boolean,
    receivingFailed: Boolean,
    isDeleted: Boolean,
    isSelectedMessage: Boolean,
    selfDeletionTimerState: SelfDeletionTimerHelper.SelfDeletionTimerState,
): Modifier {

    val selectedColorAnimation = remember { Animatable(Color.Transparent) }
    val highlightColor = colorsScheme().primaryVariant
    val transparentColor = colorsScheme().primary.copy(alpha = 0F)
    LaunchedEffect(isSelectedMessage) {
        if (isSelectedMessage) {
            selectedColorAnimation.snapTo(highlightColor)
            selectedColorAnimation.animateTo(
                transparentColor,
                tween(SELECTED_MESSAGE_ANIMATION_DURATION)
            )
        }
    }

    return this
        .then(if (isSelectedMessage) drawBehind { drawRect(selectedColorAnimation.value) } else this)
        .then(
            when {
                sendingFailed || receivingFailed -> {
                    background(MaterialTheme.wireColorScheme.messageErrorBackgroundColor)
                }

                selfDeletionTimerState is SelfDeletionTimerHelper.SelfDeletionTimerState.Expirable && !isDeleted -> {
                    val deletionColor by animateColorAsState(
                        targetValue = colorsScheme().primaryVariant.copy(alpha = selfDeletionTimerState.alphaBackgroundColor()),
                        animationSpec = tween(),
                        label = "message background color"
                    )

                    drawBehind { drawRect(deletionColor) }
                }

                else -> {
                    background(defaultBackgroundColor)
                }
            }
        )
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
        MessageSendFailureWarning(MessageFlowStatus.Failure.Send.Locally(false), true, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageSendFailureWarningWithInteractionDisabled() {
    WireTheme {
        MessageSendFailureWarning(MessageFlowStatus.Failure.Send.Locally(false), false, {}, {})
    }
}

@PreviewMultipleThemes
@Composable
fun PreviewMessageDecryptionFailure() {
    WireTheme {
        MessageDecryptionFailure(
            mockHeader,
            MessageFlowStatus.Failure.Decryption(false),
            { _, _ -> },
            Conversation.ProtocolInfo.Proteus
        )
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

private const val SELECTED_MESSAGE_ANIMATION_DURATION = 2000
