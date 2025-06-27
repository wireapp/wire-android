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
package com.wire.android.ui.userprofile.teammigration.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import com.wire.android.R
import com.wire.android.ui.authentication.login.WireAuthBackgroundLayout
import com.wire.android.ui.common.VisibilityState
import com.wire.android.ui.common.bottomsheet.WireBottomSheetDefaults
import com.wire.android.ui.common.colorsScheme
import com.wire.android.ui.common.preview.EdgeToEdgePreview
import com.wire.android.ui.common.rememberBottomBarElevationState
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun TeamMigrationContainer(
    onClose: () -> Unit = {},
    closeIconContentDescription: String = "",
    showConfirmationDialogWhenClosing: Boolean = true,
    bottomBar: @Composable (() -> Unit) = {},
    content: @Composable () -> Unit,
) {
    val confirmMigrationLeaveDialogState = rememberVisibilityState<Unit>()
    val scrollState = rememberScrollState()
    NavigationBarBackground()
    WireScaffold(
        containerColor = Color.Transparent,
        bottomBar = {
            Column(
                verticalArrangement = Arrangement.Bottom,
                modifier = Modifier
                    .clip(WireBottomSheetDefaults.WireBottomSheetShape)
                    .background(WireBottomSheetDefaults.WireSheetContainerColor)
                    .fillMaxSize()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f, fill = true)
                ) {
                    Surface(
                        color = WireBottomSheetDefaults.WireSheetContainerColor,
                        shadowElevation = scrollState.rememberTopBarElevationState().value,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column {
                            IconButton(
                                modifier = Modifier.align(alignment = Alignment.End),
                                onClick = {
                                    if (showConfirmationDialogWhenClosing) {
                                        confirmMigrationLeaveDialogState.show(Unit)
                                    } else {
                                        onClose()
                                    }
                                },
                            ) {
                                Icon(
                                    painter = painterResource(id = R.drawable.ic_close),
                                    contentDescription = closeIconContentDescription
                                )
                            }
                        }
                    }
                    Column(
                        modifier = Modifier.verticalScroll(scrollState)
                    ) {
                        content()
                    }
                }
                Surface(
                    color = WireBottomSheetDefaults.WireSheetContainerColor,
                    shadowElevation = scrollState.rememberBottomBarElevationState().value,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    bottomBar()
                }
            }
        },
    ) { _ -> }

    VisibilityState(confirmMigrationLeaveDialogState) {
        ConfirmMigrationLeaveDialog(
            onContinue = {
                confirmMigrationLeaveDialogState.dismiss()
            },
            onLeave = {
                confirmMigrationLeaveDialogState.dismiss()
                onClose()
            }
        )
    }
}

@Composable
private fun NavigationBarBackground() = Box(
    contentAlignment = Alignment.BottomCenter,
    modifier = Modifier.fillMaxSize()
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(colorsScheme().background)
            .navigationBarsPadding()
    )
}

@PreviewMultipleThemes
@Composable
private fun PreviewTeamMigrationContainer() = WireTheme {
    EdgeToEdgePreview(useDarkIcons = false) {
        WireAuthBackgroundLayout {
            TeamMigrationContainer(
                bottomBar = {
                    BottomLineButtons(isContinueButtonEnabled = true)
                },
                content = {
                    Text(text = "EMPTY")
                }
            )
        }
    }
}
