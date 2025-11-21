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

package com.wire.android.ui.userprofile.avatarpicker
import com.ramcosta.composedestinations.annotation.Destination
import com.wire.android.navigation.WireRootNavGraph

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.ramcosta.composedestinations.spec.DestinationStyle
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.WireMenuModalSheetContent
import com.wire.android.ui.common.bottomsheet.WireModalSheetLayout
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.imagepreview.BulletHoleImagePreview
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireDimensions
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel.PictureState
import com.wire.android.util.ui.PreviewMultipleThemesForLandscape
import com.wire.android.util.ui.PreviewMultipleThemesForPortrait
import com.wire.android.util.ui.PreviewMultipleThemesForSquare

@Destination<WireRootNavGraph>(
    style = DestinationStyle.Runtime::class, // default should be SlideNavigationAnimation
)
@Composable
fun AvatarPickerScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<String?>,
    viewModel: AvatarPickerViewModel = hiltViewModel()
) {
    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val targetAvatarPath = viewModel.defaultAvatarPath
    val targetAvatarUri = viewModel.temporaryAvatarUri

    val state = rememberAvatarPickerState(
        onImageSelected = { originalUri ->
            viewModel.updatePickedAvatarUri(originalUri, targetAvatarPath.toFile().toUri())
        },
        onPictureTaken = {
            viewModel.updatePickedAvatarUri(targetAvatarUri, targetAvatarPath.toFile().toUri())
        },
        targetPictureFileUri = targetAvatarUri,
        onGalleryPermissionPermanentlyDenied = {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = R.string.open_gallery_permission_dialog_description
                )
            )
        },
        onCameraPermissionPermanentlyDenied = {
            permissionPermanentlyDeniedDialogState.show(
                PermissionPermanentlyDeniedDialogState.Visible(
                    title = R.string.app_permission_dialog_title,
                    description = R.string.take_picture_permission_dialog_description
                )
            )
        },
    )

    LaunchedEffect(Unit) {
        viewModel.infoMessage.collect {
            state.showSnackbar(it)
        }
    }

    LaunchedEffect(viewModel.pictureState) {
        (viewModel.pictureState as? PictureState.Completed)?.let {
            resultNavigator.setResult(it.assetId)
            resultNavigator.navigateBack()
        }
    }

    AvatarPickerContent(
        pictureState = viewModel.pictureState,
        state = state,
        onCloseClick = navigator::navigateBack,
        onCancelClick = viewModel::loadInitialAvatarState,
        onSaveClick = viewModel::uploadNewPickedAvatar
    )

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )
}

@Composable
private fun AvatarPickerContent(
    pictureState: PictureState,
    state: AvatarPickerState,
    onCancelClick: () -> Unit,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    WireScaffold(
        topBar = { AvatarPickerTopBar(onCloseClick = onCloseClick) }
    ) { internalPadding ->
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier
                .fillMaxSize()
                .padding(internalPadding)
                .background(MaterialTheme.wireColorScheme.background)
        ) {
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.weight(1f)
            ) {
                AvatarPreview(pictureState)
            }
            AvatarPickerActionButtons(
                pictureState = pictureState,
                onSaveClick = onSaveClick,
                onCancelClick = onCancelClick,
                onChangeImage = state::showModalBottomSheet,
            )
        }
    }

    WireModalSheetLayout(
        sheetState = state.modalBottomSheetState,
        sheetContent = {
            WireMenuModalSheetContent(
                header = MenuModalSheetHeader.Visible(title = stringResource(R.string.profile_image_modal_sheet_header_title)),
                menuItems = listOf(
                    {
                        MenuBottomSheetItem(
                            title = stringResource(R.string.profile_image_choose_from_gallery_menu_item),
                            leading = {
                                MenuItemIcon(
                                    id = R.drawable.ic_gallery,
                                    contentDescription = stringResource(R.string.content_description_choose_from_gallery)
                                )
                            },
                            trailing = { ArrowRightIcon() },
                            onItemClick = state::openGallery
                        )
                    },
                    {
                        MenuBottomSheetItem(
                            title = stringResource(R.string.profile_image_take_a_picture_menu_item),
                            leading = {
                                MenuItemIcon(
                                    id = R.drawable.ic_camera,
                                    contentDescription = stringResource(R.string.content_description_take_a_picture)
                                )
                            },
                            trailing = { ArrowRightIcon() },
                            onItemClick = state::openCamera
                        )
                    }
                )
            )
        }
    )
}

@Composable
fun AvatarPreview(pictureState: PictureState) {
    BulletHoleImagePreview(
        imageUri = pictureState.avatarUri,
        contentDescription = stringResource(R.string.content_description_avatar_preview)
    )
}

@Composable
private fun AvatarPickerActionButtons(
    pictureState: PictureState,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onChangeImage: () -> Unit
) {
    Surface(
        shadowElevation = dimensions().bottomNavigationShadowElevation,
        color = MaterialTheme.wireColorScheme.background
    ) {
        Box(modifier = Modifier.padding(MaterialTheme.wireDimensions.spacing16x)) {
            when (pictureState) {
                is PictureState.Uploading, is PictureState.Picked -> {
                    val isUploading = pictureState is PictureState.Uploading

                    Row(
                        horizontalArrangement = Arrangement.spacedBy(MaterialTheme.wireDimensions.spacing16x),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        WireSecondaryButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.label_cancel),
                            onClick = onCancelClick
                        )
                        WirePrimaryButton(
                            modifier = Modifier.weight(1f),
                            text = stringResource(R.string.label_confirm),
                            onClick = onSaveClick,
                            loading = isUploading,
                            state = if (isUploading) WireButtonState.Disabled else WireButtonState.Default
                        )
                    }
                }

                else -> {
                    WirePrimaryButton(
                        text = stringResource(R.string.profile_image_change_image_button_label),
                        onClick = onChangeImage
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarPickerTopBar(onCloseClick: () -> Unit) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        navigationIconType = NavigationIconType.Back(R.string.content_description_change_picture_back_btn),
        title = stringResource(R.string.profile_image_top_bar_label),
    )
}

@PreviewMultipleThemesForPortrait
@PreviewMultipleThemesForLandscape
@PreviewMultipleThemesForSquare
@Composable
fun AvatarPickerPreview() = WireTheme {
    AvatarPickerContent(
        pictureState = PictureState.Picked("https://example.com/avatar.jpg".toUri()),
        state = rememberAvatarPickerState(
            onImageSelected = {},
            onCameraPermissionPermanentlyDenied = {},
            onGalleryPermissionPermanentlyDenied = {},
            onPictureTaken = {},
            targetPictureFileUri = "https://example.com/avatar.jpg".toUri(),
        ),
        onCloseClick = {},
        onCancelClick = {},
        onSaveClick = {}
    )
}
