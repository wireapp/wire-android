/*
 * Wire
 * Copyright (C) 2026 Wire Swiss GmbH
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
package com.wire.android.ui.home.conversations.privacy

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.layout.Row
import com.wire.android.R
import com.wire.android.feature.privacy.model.AutoLockTimeout
import com.wire.android.feature.privacy.model.ConversationPrivacyLevel
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.annotation.app.WireRootDestination
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.ui.common.WireRadioButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.settings.conversationPrivacyViewModel
import com.wire.android.ui.theme.wireTypography

@WireRootDestination(
    navArgs = ConversationPrivacyNavArgs::class,
    style = SlideNavigationAnimation::class,
)
@Composable
fun ConversationPrivacyScreen(
    navigator: Navigator,
    viewModel: ConversationPrivacyViewModel = conversationPrivacyViewModel(),
) {
    ConversationPrivacyContent(
        level = viewModel.state.level,
        autoLock = viewModel.state.autoLock,
        needsChatPinSetup = viewModel.state.needsChatPinSetup,
        needsChatPinConfirmation = viewModel.state.needsChatPinConfirmation,
        pinError = viewModel.state.pinError,
        onLevelSelected = viewModel::onLevelSelected,
        onAutoLockSelected = viewModel::onAutoLockSelected,
        onChatPinCreated = viewModel::onChatPinCreated,
        onChatPinConfirmed = viewModel::onChatPinConfirmed,
        onPinErrorCleared = viewModel::clearPinError,
        onChatPinDialogDismissed = viewModel::onChatPinDialogDismissed,
        onBackPressed = navigator::navigateBack,
    )
}

@Composable
private fun ConversationPrivacyContent(
    level: ConversationPrivacyLevel,
    autoLock: AutoLockTimeout,
    needsChatPinSetup: Boolean,
    needsChatPinConfirmation: Boolean,
    pinError: Boolean,
    onLevelSelected: (ConversationPrivacyLevel) -> Unit,
    onAutoLockSelected: (AutoLockTimeout) -> Unit,
    onChatPinCreated: (String) -> Unit,
    onChatPinConfirmed: (String) -> Unit,
    onPinErrorCleared: () -> Unit,
    onChatPinDialogDismissed: () -> Unit,
    onBackPressed: () -> Unit,
    modifier: Modifier = Modifier,
) {
    WireScaffold(
        modifier = modifier,
        topBar = {
            WireCenterAlignedTopAppBar(
                onNavigationPressed = onBackPressed,
                elevation = dimensions().spacing0x,
                title = stringResource(id = R.string.privacy_level_title),
            )
        }
    ) { internalPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
                .verticalScroll(rememberScrollState())
        ) {
            ConversationPrivacyLevel.entries.forEach { item ->
                SelectableRow(
                    title = stringResource(item.titleRes()),
                    subtitle = stringResource(item.descriptionRes()),
                    selected = level == item,
                    onClick = { onLevelSelected(item) },
                )
                WireDivider(color = colorsScheme().divider)
            }

            if (level == ConversationPrivacyLevel.HIGHLY_SENSITIVE) {
                Text(
                    text = stringResource(R.string.privacy_auto_lock_title),
                    style = MaterialTheme.wireTypography.title03,
                    color = colorsScheme().secondaryText,
                    modifier = Modifier.padding(dimensions().spacing16x),
                )
                AutoLockTimeout.entries.forEach { item ->
                    SelectableRow(
                        title = stringResource(item.labelRes()),
                        subtitle = null,
                        selected = autoLock == item,
                        onClick = { onAutoLockSelected(item) },
                    )
                    WireDivider(color = colorsScheme().divider)
                }
            }
        }
    }

    if (needsChatPinSetup) {
        ChatPinSetupDialog(onConfirm = onChatPinCreated, onDismiss = onChatPinDialogDismissed)
    }

    if (needsChatPinConfirmation) {
        ChatPinConfirmDialog(
            pinError = pinError,
            onConfirm = onChatPinConfirmed,
            onPinChange = onPinErrorCleared,
            onDismiss = onChatPinDialogDismissed,
        )
    }
}

@Composable
private fun SelectableRow(
    title: String,
    subtitle: String?,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(dimensions().spacing16x)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, style = MaterialTheme.wireTypography.body02, color = colorsScheme().onSurface)
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.wireTypography.subline01,
                    color = colorsScheme().secondaryText,
                    modifier = Modifier.padding(top = dimensions().spacing4x),
                )
            }
        }
        WireRadioButton(checked = selected, onButtonChecked = onClick)
    }
}

@Composable
private fun ChatPinSetupDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_pin_create_title)) },
        text = {
            Column {
                Text(stringResource(R.string.chat_pin_create_subtitle))
                OutlinedTextField(
                    value = pin,
                    onValueChange = { pin = it.filter(Char::isDigit).take(MAX_PIN_LENGTH) },
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions().spacing8x),
                )
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length >= MIN_PIN_LENGTH, onClick = { onConfirm(pin) }) {
                Text(stringResource(R.string.chat_pin_create_title))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

@Composable
private fun ChatPinConfirmDialog(
    pinError: Boolean,
    onConfirm: (String) -> Unit,
    onPinChange: () -> Unit,
    onDismiss: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.chat_pin_confirm_title)) },
        text = {
            Column {
                Text(stringResource(R.string.chat_pin_confirm_subtitle))
                OutlinedTextField(
                    value = pin,
                    onValueChange = {
                        pin = it.filter(Char::isDigit).take(MAX_PIN_LENGTH)
                        onPinChange()
                    },
                    isError = pinError,
                    singleLine = true,
                    visualTransformation = PasswordVisualTransformation(),
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = dimensions().spacing8x),
                )
                if (pinError) {
                    Text(
                        text = stringResource(R.string.chat_pin_incorrect),
                        style = MaterialTheme.wireTypography.subline01,
                        color = colorsScheme().error,
                        modifier = Modifier.padding(top = dimensions().spacing4x),
                    )
                }
            }
        },
        confirmButton = {
            TextButton(enabled = pin.length >= MIN_PIN_LENGTH, onClick = { onConfirm(pin) }) {
                Text(stringResource(R.string.conversation_locked_unlock))
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(android.R.string.cancel)) }
        },
    )
}

private const val MIN_PIN_LENGTH = 4
private const val MAX_PIN_LENGTH = 8

private fun ConversationPrivacyLevel.titleRes(): Int = when (this) {
    ConversationPrivacyLevel.NORMAL -> R.string.privacy_level_normal
    ConversationPrivacyLevel.SENSITIVE -> R.string.privacy_level_sensitive
    ConversationPrivacyLevel.HIGHLY_SENSITIVE -> R.string.privacy_level_highly_sensitive
}

private fun ConversationPrivacyLevel.descriptionRes(): Int = when (this) {
    ConversationPrivacyLevel.NORMAL -> R.string.privacy_level_normal_description
    ConversationPrivacyLevel.SENSITIVE -> R.string.privacy_level_sensitive_description
    ConversationPrivacyLevel.HIGHLY_SENSITIVE -> R.string.privacy_level_highly_sensitive_description
}

private fun AutoLockTimeout.labelRes(): Int = when (this) {
    AutoLockTimeout.IMMEDIATELY -> R.string.privacy_auto_lock_immediately
    AutoLockTimeout.THIRTY_SECONDS -> R.string.privacy_auto_lock_30_seconds
    AutoLockTimeout.ONE_MINUTE -> R.string.privacy_auto_lock_1_minute
    AutoLockTimeout.FIVE_MINUTES -> R.string.privacy_auto_lock_5_minutes
}
