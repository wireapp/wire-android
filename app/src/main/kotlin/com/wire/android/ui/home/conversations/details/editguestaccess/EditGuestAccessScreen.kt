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

package com.wire.android.ui.home.conversations.details.editguestaccess

import com.wire.android.navigation.annotation.app.WireRootDestination
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.navigation.style.SlideNavigationAnimation
import com.wire.android.R
import com.wire.android.navigation.NavigationCommand
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.rememberNavigator
import com.wire.android.ui.common.bottomsheet.rememberWireModalSheetState
import com.wire.android.ui.common.bottomsheet.show
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.rememberTopBarElevationState
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.snackbar.LocalSnackbarHostState
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.ramcosta.composedestinations.generated.app.destinations.CreatePasswordProtectedGuestLinkScreenDestination
import com.wire.android.ui.home.conversations.details.editguestaccess.createPasswordProtectedGuestLink.CreatePasswordGuestLinkNavArgs
import com.wire.android.ui.common.rowitem.SectionHeader
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.copyLinkToClipboard
import com.wire.android.util.shareViaIntent

@Suppress("ComplexMethod")
@WireRootDestination(
    navArgs = EditGuestAccessNavArgs::class,
    style = SlideNavigationAnimation::class, // default should be SlideNavigationAnimation
)
@Composable
fun EditGuestAccessScreen(
    navigator: Navigator,
    modifier: Modifier = Modifier,
    editGuestAccessViewModel: EditGuestAccessViewModel = hiltViewModel()
) {
    val scrollState = rememberScrollState()
    val snackbarHostState = LocalSnackbarHostState.current
    val sheetState = rememberWireModalSheetState<Unit>()
    val onSheetItemClick: (Boolean) -> Unit = { isPasswordProtected ->
        sheetState.hide()
        if (isPasswordProtected) {
            navigator.navigate(
                NavigationCommand(
                    CreatePasswordProtectedGuestLinkScreenDestination(
                        CreatePasswordGuestLinkNavArgs(
                            conversationId = editGuestAccessViewModel.conversationId
                        )
                    )
                )
            )
        } else {
            editGuestAccessViewModel.onRequestGuestRoomLink()
        }
    }
    CreateGuestLinkBottomSheet(
        sheetState = sheetState,
        onItemClick = remember { onSheetItemClick },
        isPasswordInviteLinksAllowed = editGuestAccessViewModel.editGuestAccessState.isPasswordProtectedLinksAllowed
    )

    WireScaffold(
        modifier = modifier,
        topBar = {
            val title = stringResource(id = R.string.conversation_options_guests_label)
            WireCenterAlignedTopAppBar(
                elevation = scrollState.rememberTopBarElevationState().value,
                navigationIconType = NavigationIconType.Back(R.string.content_description_edit_guests_option_back_btn),
                onNavigationPressed = navigator::navigateBack,
                title = title
            )
        }
    ) { internalPadding ->
        Column {
            LazyColumn(
                modifier = Modifier
                    .background(MaterialTheme.wireColorScheme.surface)
                    .padding(internalPadding)
                    .weight(1F)
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
                item {
                    SectionHeader(
                        name = stringResource(id = R.string.folder_label_guest_link),
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(MaterialTheme.wireColorScheme.background)
                    )
                }
                item {
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(MaterialTheme.wireColorScheme.surface)
                            .padding(
                                start = dimensions().spacing16x,
                                end = dimensions().spacing16x,
                                bottom = dimensions().spacing8x,
                                top = dimensions().spacing8x,
                            )
                    ) {
                        with(editGuestAccessViewModel) {
                            Text(
                                text = stringResource(id = R.string.guest_link_description),
                                style = MaterialTheme.wireTypography.body01,
                                color = MaterialTheme.wireColorScheme.secondaryText,
                                modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing2x)
                            )
                            Spacer(modifier = Modifier.height(dimensions().spacing16x))

                            editGuestAccessState.link?.also {
                                if (editGuestAccessState.isLinkPasswordProtected) {
                                    PasswordProtectedLinkBanner()
                                }
                                Text(
                                    text = it,
                                    style = MaterialTheme.wireTypography.body01,
                                    modifier = Modifier.padding(top = MaterialTheme.wireDimensions.spacing4x)
                                )
                            }
                        }
                    }
                }
                item {
                    val clipboardManager = LocalClipboardManager.current
                    val context = LocalContext.current

                    with(editGuestAccessViewModel) {
                        GuestLinkActionButtons(
                            shouldDisableGenerateGuestLinkButton = shouldDisableGenerateGuestLinkButton(),
                            isGeneratingLink = editGuestAccessState.isGeneratingGuestRoomLink,
                            isRevokingLink = editGuestAccessState.isRevokingLink,
                            link = editGuestAccessState.link,
                            onCreateLink = sheetState::show,
                            onRevokeLink = ::onRevokeGuestRoomLink,
                            onCopyLink = {
                                editGuestAccessState = editGuestAccessState.copy(isLinkCopied = true)
                                editGuestAccessState.link?.let {
                                    clipboardManager.copyLinkToClipboard(it)
                                }
                            },
                            onShareLink = {
                                editGuestAccessState.link?.let {
                                    context.shareViaIntent(it)
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    with(editGuestAccessViewModel) {
        if (editGuestAccessState.shouldShowGuestAccessChangeConfirmationDialog) {
            DisableGuestConfirmationDialog(
                onConfirm = ::onGuestDialogConfirm,
                onDialogDismiss = ::onGuestDialogDismiss
            )
        }
        if (editGuestAccessState.shouldShowRevokeLinkConfirmationDialog) {
            RevokeGuestConfirmationDialog(
                onConfirm = ::removeGuestLink,
                onDialogDismiss = ::onRevokeDialogDismiss
            )
        }
        if (editGuestAccessState.isFailedToGenerateGuestRoomLink) {
            GenerateGuestRoomLinkFailureDialog(
                onDismiss = ::onGenerateGuestRoomFailureDialogDismiss,
            )
        }
        if (editGuestAccessState.isLinkCopied) {
            val message = stringResource(id = R.string.guest_room_link_copied)
            LaunchedEffect(true) {
                if (!editGuestAccessState.link.isNullOrEmpty()) {
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

@Preview
@Composable
fun PreviewEditGuestAccessScreen() {
    EditGuestAccessScreen(rememberNavigator {})
}
