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

@file:Suppress("TooManyFunctions")

package com.wire.android.ui.home.conversations.details.options

import androidx.annotation.StringRes
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyListState
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.times
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.collectAsStateLifecycleAware
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.home.conversations.details.GroupConversationDetailsViewModel
import com.wire.android.ui.home.conversations.selfdeletion.SelfDeletionMapper.toSelfDeletionDuration
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.ui.home.newconversation.channelaccess.ChannelAccessType
import com.wire.android.ui.home.settings.SwitchState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.util.ui.PreviewMultipleThemes
import com.wire.android.util.ui.UIText
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId
import com.wire.kalium.logic.data.id.GroupID
import com.wire.kalium.logic.data.message.SelfDeletionTimer
import com.wire.kalium.logic.data.mls.CipherSuite
import kotlinx.datetime.Instant
import kotlin.time.Duration.Companion.days

@Composable
fun GroupConversationOptions(
    lazyListState: LazyListState,
    onEditGuestAccess: () -> Unit,
    onChannelAccessItemClicked: () -> Unit,
    onEditSelfDeletingMessages: () -> Unit,
    viewModel: GroupConversationDetailsViewModel = hiltViewModel(),
    onEditGroupName: () -> Unit
) {
    val state by viewModel.groupOptionsState.collectAsStateLifecycleAware()

    GroupConversationSettings(
        state = state,
        onGuestItemClicked = onEditGuestAccess,
        onSelfDeletingClicked = onEditSelfDeletingMessages,
        onChannelAccessItemClicked = onChannelAccessItemClicked,
        onServiceSwitchClicked = viewModel::onServicesUpdate,
        onReadReceiptSwitchClicked = viewModel::onReadReceiptUpdate,
        lazyListState = lazyListState,
        onEditGroupName = onEditGroupName,
        onWireCellSwitchClicked = viewModel::onWireCellStateChange,
    )

    if (state.changeServiceOptionConfirmationRequired) {
        DisableServicesConfirmationDialog(
            onConfirm = viewModel::onServiceDialogConfirm,
            onDialogDismiss = viewModel::onServiceDialogDismiss
        )
    }
}

@Composable
fun GroupConversationSettings(
    state: GroupConversationOptionsState,
    onChannelAccessItemClicked: () -> Unit,
    onGuestItemClicked: () -> Unit,
    onSelfDeletingClicked: () -> Unit,
    onServiceSwitchClicked: (Boolean) -> Unit,
    onReadReceiptSwitchClicked: (Boolean) -> Unit,
    onWireCellSwitchClicked: (Boolean) -> Unit,
    onEditGroupName: () -> Unit,
    modifier: Modifier = Modifier,
    lazyListState: LazyListState = rememberLazyListState(),
    mlsReadReceiptsEnabled: Boolean = BuildConfig.MLS_READ_RECEIPTS_ENABLED,
) {
    LazyColumn(
        state = lazyListState,
        verticalArrangement = Arrangement.spacedBy(DividerDefaults.Thickness),
        modifier = modifier.fillMaxSize()
    ) {
        item {
            GroupNameItem(
                groupName = state.groupName,
                canBeChanged = state.isUpdatingNameAllowed,
                isChannel = state.isChannel,
                onClick = onEditGroupName,
            )
        }
        if (state.areAccessOptionsAvailable) {
            folderWithItems(
                folderTitleResId = R.string.folder_label_access,
                showFolder = !state.isChannel,
                items = buildList {
                    addIf(state.isChannel) {
                        GroupConversationOptionsItem(
                            title = stringResource(R.string.channel_access_label),
                            subtitle = stringResource(id = R.string.channel_access_short_description),
                            arrowType = if (state.isUpdatingChannelAccessAllowed) ArrowType.TITLE_ALIGNED else ArrowType.NONE,
                            arrowLabel = stringResource(state.channelAccessType!!.labelResId),
                            arrowLabelColor = colorsScheme().onBackground,
                            clickable = Clickable(
                                enabled = state.isUpdatingChannelAccessAllowed,
                                onClick = onChannelAccessItemClicked,
                            ),
                        )
                    }
                    add {
                        GroupConversationOptionsItem(
                            title = stringResource(id = R.string.conversation_options_guests_label),
                            subtitle = stringResource(id = R.string.conversation_details_guest_description),
                            switchState = SwitchState.TextOnly(value = state.isGuestAllowed),
                            arrowType = if (state.isUpdatingGuestAllowed) ArrowType.TITLE_ALIGNED else ArrowType.NONE,
                            clickable = Clickable(
                                enabled = state.isUpdatingGuestAllowed,
                                onClick = onGuestItemClicked,
                                onClickDescription = stringResource(id = R.string.content_description_conversation_details_guests_action)
                            ),
                        )
                    }
                    add {
                        ServicesOption(
                            isSwitchEnabledAndVisible = state.isUpdatingServicesAllowed,
                            switchState = state.isServicesAllowed,
                            isLoading = state.loadingServicesOption,
                            onCheckedChange = onServiceSwitchClicked
                        )
                    }
                }
            )
        }

        folderWithItems(
            folderTitleResId = R.string.folder_label_messaging,
            items = buildList {
                addIf(!state.selfDeletionTimer.isDisabled) {
                    GroupConversationOptionsItem(
                        title = stringResource(id = R.string.conversation_options_self_deleting_messages_label),
                        subtitle = stringResource(id = R.string.conversation_options_self_deleting_messages_description),
                        trailingOnText = if (state.selfDeletionTimer.isEnforced) {
                            "(${state.selfDeletionTimer.duration.toSelfDeletionDuration().shortLabel.asString()})"
                        } else {
                            null
                        },
                        switchState = SwitchState.TextOnly(value = state.selfDeletionTimer.isEnforced),
                        arrowType = if (state.isUpdatingSelfDeletingAllowed && !state.selfDeletionTimer.isEnforcedByTeam) {
                            ArrowType.TITLE_ALIGNED
                        } else {
                            ArrowType.NONE
                        },
                        clickable = Clickable(
                            enabled = state.isUpdatingSelfDeletingAllowed && !state.selfDeletionTimer.isEnforcedByTeam,
                            onClick = onSelfDeletingClicked,
                            onClickDescription = stringResource(id = R.string.content_description_conversation_details_self_deleting_action)
                        )
                    )
                }
                addIf(state.protocolInfo !is Conversation.ProtocolInfo.MLS || mlsReadReceiptsEnabled) {
                    ReadReceiptOption(
                        isSwitchEnabled = state.isUpdatingReadReceiptAllowed,
                        switchState = state.isReadReceiptAllowed,
                        isLoading = state.loadingReadReceiptOption,
                        onCheckedChange = onReadReceiptSwitchClicked
                    )
                }
            }
        )

        folderWithItems(
            folderTitleResId = R.string.folder_label_protocol_details,
            items = conversationProtocolDetailsItems(protocolInfo = state.protocolInfo),
        )

        if (state.isWireCellFeatureEnabled) {
            folderWithItems(
                folderTitleResId = R.string.folder_label_wire_cell,
                items = listOf {
                    ConversationCellDetails(
                        isWireCellEnabled = state.isWireCellEnabled,
                        isLoading = state.loadingWireCellState,
                        onCheckedChange = onWireCellSwitchClicked
                    )
                }
            )
        }
    }
}

/**
 * Adds a new section to the LazyListScope with a header and items.
 * @param folderTitleResId The resource ID for the folder title.
 * @param showFolder Whether to show the folder header. Defaults to true.
 * @param items A list of composable functions representing the items to be displayed in the section.
 *              If the list is empty, the whole section will be skipped, so that there will be no header with empty section.
 */
private fun LazyListScope.folderWithItems(
    @StringRes folderTitleResId: Int,
    showFolder: Boolean = true,
    items: List<@Composable () -> Unit> = emptyList(),
) {
    if (items.isNotEmpty() && showFolder) {
        item {
            FolderHeader(
                name = stringResource(folderTitleResId),
                padding = PaddingValues(
                    horizontal = dimensions().spacing16x,
                    vertical = dimensions().spacing8x - (2 * DividerDefaults.Thickness),
                )
            )
        }
    }
    items(count = items.size) { index ->
        items[index].invoke()
    }
}

private fun <E> MutableList<E>.addIf(condition: Boolean, element: E) {
    if (condition) add(element)
}

private fun conversationProtocolDetailsItems(
    protocolInfo: Conversation.ProtocolInfo,
): List<@Composable () -> Unit> = buildList {
    add {
        ProtocolDetails(
            label = UIText.StringResource(R.string.protocol),
            text = UIText.DynamicString(protocolInfo.name())
        )
    }

    if (protocolInfo is Conversation.ProtocolInfo.MLS) {
        add {
            ProtocolDetails(
                label = UIText.StringResource(R.string.cipher_suite),
                text = UIText.DynamicString(protocolInfo.cipherSuite.toString())
            )
        }

        if (BuildConfig.PRIVATE_BUILD) {
            add {
                ProtocolDetails(
                    label = UIText.StringResource(R.string.last_key_material_update_label),
                    text = UIText.DynamicString(protocolInfo.keyingMaterialLastUpdate.toString())
                )
            }

            add {
                ProtocolDetails(
                    label = UIText.StringResource(R.string.group_state_label),
                    text = UIText.DynamicString(protocolInfo.groupState.name)
                )
            }
        }
    }
}

@Composable
private fun GroupNameItem(
    groupName: String,
    canBeChanged: Boolean,
    isChannel: Boolean,
    onClick: () -> Unit = {},
) {
    GroupConversationOptionsItem(
        label = stringResource(
            id = if (isChannel) R.string.channel_name_title else R.string.conversation_details_options_group_name
        ),
        title = groupName,
        clickable = Clickable(
            enabled = canBeChanged,
            onClick = onClick,
            onClickDescription = stringResource(id = R.string.content_description_edit_label)
        ),
        arrowType = if (!canBeChanged) ArrowType.NONE else ArrowType.CENTER_ALIGNED,
    )
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
}

@Composable
private fun ProtocolDetails(label: UIText, text: UIText) {
    GroupConversationOptionsItem(
        label = label.asString(),
        title = text.asString(),
        arrowType = ArrowType.NONE
    )
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
}

@Composable
private fun ServicesOption(
    isSwitchEnabledAndVisible: Boolean,
    switchState: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupOptionWithSwitch(
        switchClickable = isSwitchEnabledAndVisible,
        switchVisible = isSwitchEnabledAndVisible,
        switchState = switchState,
        isLoading = isLoading,
        onClick = onCheckedChange,
        title = R.string.conversation_options_services_label,
        subTitle = if (isSwitchEnabledAndVisible) R.string.conversation_options_services_description else null
    )
}

@Composable
private fun ReadReceiptOption(
    isSwitchEnabled: Boolean,
    switchState: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    GroupOptionWithSwitch(
        switchClickable = isSwitchEnabled,
        switchVisible = isSwitchEnabled,
        switchState = switchState,
        isLoading = isLoading,
        onClick = onCheckedChange,
        title = R.string.conversation_options_read_receipt_label,
        subTitle = R.string.conversation_options_read_receipt_description
    )
}

@Composable
fun GroupOptionWithSwitch(
    switchState: Boolean,
    switchClickable: Boolean,
    switchVisible: Boolean,
    isLoading: Boolean,
    onClick: (Boolean) -> Unit,
    @StringRes title: Int,
    @StringRes subTitle: Int?,
) {
    GroupConversationOptionsItem(
        title = stringResource(id = title),
        subtitle = subTitle?.let { stringResource(id = it) },
        switchState = when {
            !switchVisible -> SwitchState.TextOnly(value = switchState)
            switchClickable && !isLoading -> SwitchState.Enabled(value = switchState, onCheckedChange = onClick)
            else -> SwitchState.Disabled(value = switchState)
        },
        arrowType = ArrowType.NONE
    )
    HorizontalDivider(thickness = Dp.Hairline, color = MaterialTheme.wireColorScheme.divider)
}

@Composable
private fun DisableServicesConfirmationDialog(onConfirm: () -> Unit, onDialogDismiss: () -> Unit) {
    DisableConformationDialog(
        title = R.string.disable_services_dialog_title,
        text = R.string.disable_services_dialog_text,
        onDismiss = onDialogDismiss,
        onConfirm = onConfirm
    )
}

@Composable
fun DisableConformationDialog(@StringRes title: Int, @StringRes text: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    WireDialog(
        title = stringResource(id = title),
        text = stringResource(id = text),
        onDismiss = onDismiss,
        optionButton1Properties = WireDialogButtonProperties(
            onClick = onDismiss,
            text = stringResource(id = R.string.label_cancel),
            type = WireDialogButtonType.Secondary,
        ),
        optionButton2Properties = WireDialogButtonProperties(
            onClick = onConfirm,
            text = stringResource(id = R.string.label_disable),
            type = WireDialogButtonType.Primary,
        )
    )
}

@Composable
private fun ConversationCellDetails(
    isWireCellEnabled: Boolean,
    isLoading: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    GroupOptionWithSwitch(
        switchClickable = true,
        switchVisible = true,
        switchState = isWireCellEnabled,
        isLoading = isLoading,
        onClick = onCheckedChange,
        title = R.string.conversation_options_wire_cell_label,
        subTitle = R.string.conversation_options_wire_cell_description
    )
}

private val StateMember = GroupConversationOptionsState(
    conversationId = ConversationId("someValue", "someDomain"),
    groupName = "Conversation Name",
    areAccessOptionsAvailable = true,
    isGuestAllowed = true,
    isServicesAllowed = true,
    isReadReceiptAllowed = true,
)

private val StateAdmin = StateMember.copy(
    isUpdatingNameAllowed = true,
    isUpdatingGuestAllowed = true,
    isUpdatingChannelAccessAllowed = true,
    isUpdatingServicesAllowed = true,
    isUpdatingSelfDeletingAllowed = true,
    isUpdatingReadReceiptAllowed = true,
)

@Suppress("MagicNumber")
private val ProtocolInfoMLS = Conversation.ProtocolInfo.MLS(
    groupId = GroupID("groupId"),
    groupState = Conversation.ProtocolInfo.MLSCapable.GroupState.ESTABLISHED,
    epoch = ULong.MIN_VALUE,
    keyingMaterialLastUpdate = Instant.fromEpochMilliseconds(1648654560000),
    cipherSuite = CipherSuite.MLS_128_DHKEMX25519_AES128GCM_SHA256_Ed25519
)

@Composable
private fun PreviewGroupConversationOptions(state: GroupConversationOptionsState) = WireTheme {
    GroupConversationSettings(
        state = state,
        onChannelAccessItemClicked = {},
        onGuestItemClicked = {},
        onSelfDeletingClicked = {},
        onServiceSwitchClicked = {},
        onReadReceiptSwitchClicked = {},
        onWireCellSwitchClicked = {},
        onEditGroupName = {},
        modifier = Modifier,
        lazyListState = rememberLazyListState(),
        mlsReadReceiptsEnabled = false,
    )
}

@PreviewMultipleThemes
@Composable
fun PreviewAdminTeamGroupConversationOptions() = PreviewGroupConversationOptions(
    state = StateAdmin
)

@PreviewMultipleThemes
@Composable
fun PreviewGuestAdminTeamGroupConversationOptions() = PreviewGroupConversationOptions(
    state = StateAdmin.copy(
        isUpdatingGuestAllowed = false
    )
)

@PreviewMultipleThemes
@Composable
fun PreviewExternalMemberAdminTeamGroupConversationOptions() = PreviewGroupConversationOptions(
    state = StateAdmin.copy(
        isUpdatingNameAllowed = false,
        isUpdatingGuestAllowed = false,
    )
)

@PreviewMultipleThemes
@Composable
fun PreviewMemberTeamGroupConversationOptions() = PreviewGroupConversationOptions(
    state = StateMember
)

@PreviewMultipleThemes
@Composable
fun PreviewNormalGroupConversationOptions() = PreviewGroupConversationOptions(
    state = StateMember.copy(
        areAccessOptionsAvailable = false,
    )
)

@PreviewMultipleThemes
@Composable
fun PreviewNormalGroupConversationOptionsWithSelfDeleting() = PreviewGroupConversationOptions(
    state = StateMember.copy(
        areAccessOptionsAvailable = false,
        selfDeletionTimer = SelfDeletionTimer.Enabled(3.days),
        isUpdatingSelfDeletingAllowed = true,
    )
)

@PreviewMultipleThemes
@Composable
fun PreviewAdminMlsGroup() = PreviewGroupConversationOptions(
    state = StateAdmin.copy(
        protocolInfo = ProtocolInfoMLS,
        mlsEnabled = true,
    ),
)

@PreviewMultipleThemes
@Composable
fun PreviewMemberMlsGroup() = PreviewGroupConversationOptions(
    state = StateMember.copy(
        isChannel = true,
        channelAccessType = ChannelAccessType.PRIVATE,
        protocolInfo = ProtocolInfoMLS,
        mlsEnabled = true,
    ),
)

@PreviewMultipleThemes
@Composable
fun PreviewAdminChannel() = PreviewGroupConversationOptions(
    state = StateAdmin.copy(
        isChannel = true,
        channelAccessType = ChannelAccessType.PRIVATE,
        protocolInfo = ProtocolInfoMLS,
        mlsEnabled = true,
    ),
)

@PreviewMultipleThemes
@Composable
fun PreviewMemberChannel() = PreviewGroupConversationOptions(
    state = StateMember.copy(
        isChannel = true,
        channelAccessType = ChannelAccessType.PRIVATE,
        protocolInfo = ProtocolInfoMLS,
        mlsEnabled = true,
    ),
)
