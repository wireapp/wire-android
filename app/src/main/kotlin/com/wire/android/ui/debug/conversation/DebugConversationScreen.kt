/*
 * Wire
 * Copyright (C) 2025 Wire Swiss GmbH
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
package com.wire.android.ui.debug.conversation

import com.wire.android.navigation.annotation.app.WireRootDestination
import android.widget.Toast
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.BuildConfig
import com.wire.android.R
import com.wire.android.model.Clickable
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.HandleActions
import com.wire.android.ui.common.WireDialog
import com.wire.android.ui.common.WireDialogButtonProperties
import com.wire.android.ui.common.WireDialogButtonType
import com.wire.android.ui.common.rowitem.RowItemTemplate
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSwitch
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.topappbar.WireTopAppBarTitle
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.home.settings.SettingsItem
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.kalium.logic.data.conversation.Conversation
import com.wire.kalium.logic.data.id.ConversationId

@WireRootDestination(
    navArgs = DebugConversationScreenNavArgs::class,
)
@Composable
fun DebugConversationScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    viewModel: DebugConversationViewModel = hiltViewModel(),
) {

    val context = LocalContext.current
    val scrollState = rememberScrollState()

    WireScaffold(
        topBar = {
            WireCenterAlignedTopAppBar(
                titleContent = {
                    WireTopAppBarTitle(
                        title = "Conversation Debug Menu",
                        style = MaterialTheme.wireTypography.title01,
                        maxLines = 2
                    )
                },
                navigationIconType = NavigationIconType.Close(R.string.content_description_conversation_details_close_btn),
                onNavigationPressed = {
                    navigator.navigateBack()
                }
            )
        },
        content = { paddingValues ->

            val state by viewModel.state.collectAsState()

            Column(
                modifier = modifier
                    .fillMaxSize()
                    .verticalScroll(scrollState)
                    .padding(paddingValues),
            ) {
                SectionHeader("Conversation details")
                ConversationDetailsView(state)

                state.mlsProtocolInfo?.let {
                    SectionHeader("MLS")
                    MlsDetailsView(it)
                }

                SectionHeader("Actions")
                ConversationActionsView(
                    state = state,
                    onUpdate = { viewModel.updateConversation() },
                    onReset = { viewModel.resetMLSConversation() },
                )
                if (BuildConfig.CONVERSATION_FEEDER_ENABLED) {
                    SectionHeader("Feeders / performance config")
                    ConversationFeedConfigView(
                        config = state.feedConfig,
                        onMessagesToggle = { enabled -> viewModel.onMessagesFeederToggle(enabled) },
                        onReactionsToggle = { enabled -> viewModel.onReactionsFeederToggle(enabled) },
                        onUnreadEventsToggle = { enabled -> viewModel.onUnreadEventsFeederToggle(enabled) },
                        onMentionsToggle = { enabled -> viewModel.onMentionsFeederToggle(enabled) },
                        onShowDialog = { show -> viewModel.showFeedersDialog(show) },
                        onRunFeeders = { viewModel.runFeedersForConversation() }
                    )
                }
            }
        }
    )

    HandleActions(viewModel.actions) { action ->
        when (action) {
            is ShowMessage -> {
                Toast.makeText(context, action.message, Toast.LENGTH_SHORT).show()
            }
        }
    }
}

@Composable
private fun ConversationActionsView(
    state: DebugConversationViewState,
    onUpdate: () -> Unit,
    onReset: () -> Unit,
) {
    RowItemTemplate(
        title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = "Fetch conversation",
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                onClick = onUpdate,
                text = "Fetch now",
                fillMaxWidth = false
            )
        }
    )
    if (state.mlsProtocolInfo != null) {
        RowItemTemplate(modifier = Modifier.wrapContentWidth(), title = {
            Text(
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                text = "Reset MLS Conversation",
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        }, actions = {
            WirePrimaryButton(
                onClick = onReset,
                text = "Reset now",
                fillMaxWidth = false,
            )
        })
    }
}

@Composable
private fun ConversationDetailsView(state: DebugConversationViewState) {

    val clipboard = LocalClipboardManager.current

    state.conversationName?.let {
        SettingsItem(
            title = "Conversation Name",
            text = it,
            trailingIcon = R.drawable.ic_copy,
            onRowPressed = Clickable(
                enabled = true,
                onClick = {
                    clipboard.setText(AnnotatedString(it))
                }
            ),
        )
    }
    state.conversationId?.let {
        SettingsItem(
            title = "Conversation Id",
            wrapTitleContentWidth = false,
            text = it,
            trailingIcon = R.drawable.ic_copy,
            onRowPressed = Clickable(
                enabled = true,
                onClick = {
                    clipboard.setText(AnnotatedString(it))
                }
            ),
        )
        SettingsItem(
            title = "Team Id",
            text = state.teamId ?: "-",
            wrapTitleContentWidth = false,
            trailingIcon = state.teamId?.let { R.drawable.ic_copy },
            onRowPressed = Clickable(
                enabled = true,
                onClick = {
                    state.teamId?.let { teamId ->
                        clipboard.setText(AnnotatedString(teamId))
                    }
                }
            ),
        )
    }
}

@Composable
private fun MlsDetailsView(mlsProtocolInfo: Conversation.ProtocolInfo.MLS) {

    val clipboard = LocalClipboardManager.current

    SettingsItem(
        title = "Group ID",
        text = mlsProtocolInfo.groupId.value,
        maxTitleLines = 2,
        wrapTitleContentWidth = false,
        trailingIcon = R.drawable.ic_copy,
        onRowPressed = Clickable(
            enabled = true,
            onClick = {
                clipboard.setText(AnnotatedString(mlsProtocolInfo.groupId.value))
            }
        ),
    )
    SettingsItem(
        title = "Group state",
        text = mlsProtocolInfo.groupState.toString(),
    )
    SettingsItem(
        title = "Epoch",
        text = mlsProtocolInfo.epoch.toString(),
    )
}

@Composable
private fun ConversationFeedConfigView(
    config: DebugFeedConfigUiState,
    onMessagesToggle: (Boolean) -> Unit,
    onReactionsToggle: (Boolean) -> Unit,
    onUnreadEventsToggle: (Boolean) -> Unit,
    onMentionsToggle: (Boolean) -> Unit,
    onShowDialog: (Boolean) -> Unit,
    onRunFeeders: () -> Unit,
) {
    if (config.showDialog) {
        WireDialog(
            title = "Warning: Irreversible Debug Operation",
            text = "Using feeders will permanently modify this conversation’s data.\n" +
                    "Synthetic messages, mentions, reactions or unread events cannot be undone.\n" +
                    "\n" +
                    "This action is for internal testing only. Do NOT use on real conversations.",
            onDismiss = {
                onShowDialog(false)
            },
            optionButton1Properties = WireDialogButtonProperties(
                onClick = {
                    onShowDialog(false)
                },
                text = stringResource(R.string.label_cancel),
                type = WireDialogButtonType.Secondary,
            ),
            optionButton2Properties = WireDialogButtonProperties(
                onClick = {
                    onRunFeeders()
                },
                text = "Run feeders",
                loading = config.isProcessing,
                type = WireDialogButtonType.Primary,
            )
        )
    }

    RowItemTemplate(
        title = {
            Text(
                text = "Feed messages",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = config.messagesEnabled,
                onCheckedChange = onMessagesToggle
            )
        }
    )

    RowItemTemplate(
        title = {
            Text(
                text = "Feed reactions",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = config.reactionsEnabled,
                onCheckedChange = onReactionsToggle
            )
        }
    )

    RowItemTemplate(
        title = {
            Text(
                text = "Feed unread events",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = config.unreadEventsEnabled,
                onCheckedChange = onUnreadEventsToggle
            )
        }
    )

    RowItemTemplate(
        title = {
            Text(
                text = "Feed mentions",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WireSwitch(
                checked = config.mentionsEnabled,
                onCheckedChange = onMentionsToggle
            )
        }
    )

    RowItemTemplate(
        title = {
            Text(
                text = "Run selected feeders",
                style = MaterialTheme.wireTypography.body01,
                color = MaterialTheme.wireColorScheme.onBackground,
                modifier = Modifier.padding(start = dimensions().spacing8x)
            )
        },
        actions = {
            WirePrimaryButton(
                loading = config.isProcessing,
                onClick = { onShowDialog(true) },
                text = "Run feeders",
                fillMaxWidth = false
            )
        }
    )
}

data class DebugConversationScreenNavArgs(
    val conversationId: ConversationId,
)
