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
package com.wire.android.feature.meetings.ui.options

import android.content.ClipData
import android.content.ClipboardManager
import android.widget.Toast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontFamily
import com.wire.android.feature.meetings.R
import com.wire.android.feature.meetings.ui.shareMeetingWithGuestsViewModel
import com.wire.android.feature.meetings.ui.util.PreviewMultipleThemes
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.typography
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.common.R as CommonR

@Composable
internal fun ShareMeetingWithGuestsScreen(
    occurrenceId: String,
    onBack: () -> Unit,
    viewModel: ShareMeetingWithGuestsViewModel = shareMeetingWithGuestsViewModel(occurrenceId),
) {
    val context = LocalContext.current
    val title = stringResource(R.string.meeting_share_with_guests)
    val copiedMessage = stringResource(R.string.meeting_share_link_copied)
    when (val state = viewModel.state) {
        ShareMeetingWithGuestsState.Loading -> Unit
        ShareMeetingWithGuestsState.NotAvailable -> LaunchedEffect(state) { onBack() }
        is ShareMeetingWithGuestsState.Ready -> {
            ShareMeetingWithGuestsContent(
                link = state.link,
                hasPassword = state.hasPassword,
                onDismiss = onBack,
                onCopyLink = {
                    val link = state.link
                    if (!link.isNullOrBlank()) {
                        context.getSystemService(ClipboardManager::class.java).setPrimaryClip(ClipData.newPlainText(title, link))
                        Toast.makeText(context, copiedMessage, Toast.LENGTH_SHORT).show()
                    }
                },
            )
        }
    }
}

@Composable
internal fun ShareMeetingWithGuestsContent(
    link: String?,
    hasPassword: Boolean,
    onDismiss: () -> Unit,
    onCopyLink: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val scrollState = rememberScrollState()
    WireScaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            WireCenterAlignedTopAppBar(
                title = stringResource(R.string.meeting_share_with_guests),
                navigationIconType = NavigationIconType.Close(),
                onNavigationPressed = onDismiss,
                elevation = scrollState.rememberTopBarElevationState().value,
            )
        },
        bottomBar = {
            Surface(
                shadowElevation = MaterialTheme.wireDimensions.bottomNavigationShadowElevation,
                color = MaterialTheme.wireColorScheme.background,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(dimensions().spacing16x),
                    horizontalArrangement = Arrangement.spacedBy(dimensions().spacing16x),
                ) {
                    WireSecondaryButton(
                        text = stringResource(CommonR.string.label_cancel),
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                    )
                    WirePrimaryButton(
                        text = stringResource(R.string.meeting_share_copy_link),
                        onClick = onCopyLink,
                        state = if (link.isNullOrBlank()) WireButtonState.Disabled else WireButtonState.Default,
                        modifier = Modifier.weight(1f),
                        leadingIcon = {
                            Icon(
                                painter = painterResource(R.drawable.ic_copy),
                                contentDescription = null,
                                modifier = Modifier.padding(end = dimensions().spacing4x)
                            )
                        },
                    )
                }
            }
        },
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(scrollState)
                .padding(dimensions().spacing16x),
            verticalArrangement = Arrangement.spacedBy(dimensions().spacing24x),
        ) {
            Text(
                text = stringResource(R.string.meeting_share_link_description),
                style = typography().body01
            )

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(dimensions().spacing8x),
                color = colorsScheme().surface,
                border = BorderStroke(dimensions().spacing1x, colorsScheme().outline),
            ) {
                if (!link.isNullOrBlank()) {
                    SelectionContainer {
                        Text(
                            text = link,
                            style = typography().body01.copy(fontFamily = FontFamily.Monospace),
                            modifier = Modifier.padding(dimensions().spacing8x),
                        )
                    }
                } else {
                    Text(
                        text = stringResource(R.string.meeting_share_link_unavailable),
                        style = typography().body01,
                        color = colorsScheme().secondaryText,
                        modifier = Modifier.padding(dimensions().spacing8x),
                    )
                }
            }
            if (!link.isNullOrBlank()) {
                Text(
                    text = stringResource(
                        when {
                            hasPassword -> R.string.meeting_share_link_password_protected
                            else -> R.string.meeting_share_link_not_password_protected
                        }
                    ),
                    style = typography().body01,
                )
            }
        }
    }
}

@PreviewMultipleThemes
@Composable
private fun ShareMeetingWithGuestsPreview() = WireTheme {
    ShareMeetingWithGuestsContent(
        link = "https://account.wire.com/conversation-join/?key=example&code=example&domain=wire.com",
        hasPassword = true,
        onDismiss = {},
        onCopyLink = {},
    )
}
