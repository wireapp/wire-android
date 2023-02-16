/*
 * Wire
 * Copyright (C) 2023 Wire Swiss GmbH
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
 *
 *
 */

package com.wire.android.ui.home.conversations.details.editguestaccess

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.ClipboardManager
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.home.conversationslist.common.FolderHeader
import com.wire.android.util.copyLinkToClipboard
import com.wire.android.util.shareViaIntent

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditGuestAccessScreen(
    editGuestAccessViewModel: EditGuestAccessViewModel = hiltViewModel(),
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    Scaffold(topBar = {
        WireCenterAlignedTopAppBar(
            elevation = scrollState.rememberTopBarElevationState().value,
            onNavigationPressed = editGuestAccessViewModel::navigateBack,
            title = stringResource(id = R.string.conversation_options_guests_label)
        )
    }, snackbarHost = {
        SwipeDismissSnackbarHost(
            hostState = snackbarHostState, modifier = Modifier.fillMaxWidth()
        )
    }) { internalPadding ->
        LazyColumn(
            modifier = Modifier
                .padding(internalPadding)
                .fillMaxSize()
        ) {
            item {
                with(editGuestAccessViewModel) {
                    GuestOption(
                        isSwitchEnabled = editGuestAccessState.isUpdatingGuestAccessAllowed,
                        isSwitchVisible = true,
                        switchState = editGuestAccessState.isGuestAccessAllowed,
                        isLoading = editGuestAccessState.isUpdatingGuestAccess,
                        onCheckedChange = ::updateGuestAccess
                    )
                }
            }
            item { FolderHeader(name = stringResource(id = R.string.folder_label_guest_link)) }
            item {
                val clipboardManager = LocalClipboardManager.current
                val context = LocalContext.current

                with(editGuestAccessViewModel) {
                    LinkSection(isGeneratingLink = editGuestAccessState.isGeneratingGuestRoomLink,
                        isRevokingLink = editGuestAccessState.isRevokingLink,
                        link = editGuestAccessState.link,
                        onCreateLink = ::onGenerateGuestRoomLink,
                        onRevokeLink = ::onRevokeGuestRoomLink,
                        onCopyLink = {
                            editGuestAccessState = editGuestAccessState.copy(isLinkCopied = true)
                            clipboardManager.copyLinkToClipboard(editGuestAccessState.link)
                        },
                        onShareLink = {
                            context.shareViaIntent(editGuestAccessState.link)
                        }
                    )
                }
            }
        }
    }

    with(editGuestAccessViewModel) {
        if (editGuestAccessState.changeGuestOptionConfirmationRequired) {
            DisableGuestConfirmationDialog(
                onConfirm = ::onGuestDialogConfirm, onDialogDismiss = ::onGuestDialogDismiss
            )
        }
        if (editGuestAccessState.shouldShowRevokeLinkConfirmationDialog) {
            RevokeGuestConfirmationDialog(
                onConfirm = ::onRevokeDialogConfirm, onDialogDismiss = ::onRevokeDialogDismiss
            )
        }
        if (editGuestAccessState.isFailedToGenerateGuestRoomLink) {
            GenerateGuestRoomLinkFailureDialog(
                onDismiss = ::onGenerateGuestRoomFailureDialogDismiss,
            )
        }
        if (editGuestAccessState.isLinkCopied) {
            val message = stringResource(id = R.string.guest__room_link_copied)
            LaunchedEffect(true) {
                if (editGuestAccessState.link.isNotEmpty()) {
                    snackbarHostState.showSnackbar(message)
                    editGuestAccessState = editGuestAccessState.copy(isLinkCopied = false)
                }
            }
        }
        if (editGuestAccessState.isFailedToRevokeGuestRoomLink) {
            RevokeGuestRoomLinkFailureDialog(
                onDismiss = ::onRevokeGuestRoomFailureDialogDismiss,
            )
        }
    }
}

fun copyLinkToClipboard(clipboardManager: ClipboardManager, link: String) {
    clipboardManager.setText(AnnotatedString(link))
}

@Preview
@Composable
fun PreviewEditGuestAccessScreen() {
    EditGuestAccessScreen()
}
