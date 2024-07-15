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

import android.content.Context
import android.net.Uri
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.RootNavGraph
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.wire.android.R
import com.wire.android.navigation.Navigator
import com.wire.android.navigation.WireDestination
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetHeader
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WirePrimaryButton
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dialogs.PermissionPermanentlyDeniedDialog
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.imagepreview.BulletHoleImagePreview
import com.wire.android.ui.common.scaffold.WireScaffold
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.common.visbility.rememberVisibilityState
import com.wire.android.ui.home.conversations.PermissionPermanentlyDeniedDialogState
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel.PictureState
import com.wire.android.util.ImageUtil
import com.wire.android.util.resampleImageAndCopyToTempPath
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okio.Path

@RootNavGraph
@WireDestination
@Composable
fun AvatarPickerScreen(
    navigator: Navigator,
    resultNavigator: ResultBackNavigator<String?>,
    viewModel: AvatarPickerViewModel = hiltViewModel()
) {
    val permissionPermanentlyDeniedDialogState =
        rememberVisibilityState<PermissionPermanentlyDeniedDialogState>()

    val context = LocalContext.current

    val targetAvatarPath = viewModel.defaultAvatarPath
    val targetAvatarUri = viewModel.temporaryAvatarUri

    val scope = rememberCoroutineScope()
    val state = rememberAvatarPickerState(
        onImageSelected = { originalUri ->
            onNewAvatarPicked(originalUri, targetAvatarPath, scope, context, viewModel)
        },
        onPictureTaken = {
            onNewAvatarPicked(targetAvatarUri, targetAvatarPath, scope, context, viewModel)
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

    AvatarPickerContent(
        pictureState = viewModel.pictureState,
        state = state,
        onCloseClick = navigator::navigateBack,
        onCancelClick = viewModel::loadInitialAvatarState,
        onSaveClick = {
            viewModel.uploadNewPickedAvatar { avatarAssetId ->
                resultNavigator.setResult(avatarAssetId)
                resultNavigator.navigateBack()
            }
        }
    )

    PermissionPermanentlyDeniedDialog(
        dialogState = permissionPermanentlyDeniedDialogState,
        hideDialog = permissionPermanentlyDeniedDialogState::dismiss
    )
}

// TODO: Mateusz: I think we should refactor this, it takes some values from the ViewModel, part of the logic is executed inside 
// the UI, part of the logic is exectued inside the ViewModel, I see no reasons to handle the logic inside the UI
// personally it was a confusing part for me to read when investing the bugs, unless there is a valid reason to move the logic to the UI
// that I am not aware of ?
fun onNewAvatarPicked(originalUri: Uri, targetAvatarPath: Path, scope: CoroutineScope, context: Context, viewModel: AvatarPickerViewModel) {
    scope.launch {
        sanitizeAvatarImage(originalUri, targetAvatarPath, context)
        withContext(Dispatchers.Main) {
            viewModel.updatePickedAvatarUri(targetAvatarPath.toFile().toUri())
        }
    }
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
        Box(
            Modifier
                .fillMaxSize()
                .padding(internalPadding)
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .background(MaterialTheme.wireColorScheme.background)
            ) {
                Box(Modifier.weight(1f)) {
                    Box(Modifier.align(Alignment.Center)) {
                        AvatarPreview(pictureState)
                    }
                }
                HorizontalDivider()
                Spacer(Modifier.height(4.dp))
                AvatarPickerActionButtons(
                    pictureState = pictureState,
                    onSaveClick = onSaveClick,
                    onCancelClick = onCancelClick,
                    onChangeImage = state::showModalBottomSheet,
                )
            }
        }
    }

    MenuModalSheetLayout(
        sheetState = state.modalBottomSheetState,
        coroutineScope = rememberCoroutineScope(),
        header = MenuModalSheetHeader.Visible(title = stringResource(R.string.profile_image_modal_sheet_header_title)),
        menuItems = listOf(
            {
                MenuBottomSheetItem(
                    title = stringResource(R.string.profile_image_choose_from_gallery_menu_item),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_gallery,
                            contentDescription = stringResource(R.string.content_description_choose_from_gallery)
                        )
                    },
                    action = { ArrowRightIcon() },
                    onItemClick = state::openGallery
                )
            }, {
                MenuBottomSheetItem(
                    title = stringResource(R.string.profile_image_take_a_picture_menu_item),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_camera,
                            contentDescription = stringResource(R.string.content_description_take_a_picture)
                        )
                    },
                    action = { ArrowRightIcon() },
                    onItemClick = state::openCamera
                )
            }
        )
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
    when (pictureState) {
        is PictureState.Uploading, is PictureState.Picked -> {
            val isUploading = pictureState is PictureState.Uploading

            Row(Modifier.fillMaxWidth()) {
                WireSecondaryButton(
                    modifier = Modifier
                        .padding(dimensions().spacing16x)
                        .weight(1f),
                    text = stringResource(R.string.label_cancel),
                    onClick = onCancelClick
                )
                WirePrimaryButton(
                    modifier = Modifier
                        .padding(dimensions().spacing16x)
                        .weight(1f),
                    text = stringResource(R.string.label_confirm),
                    onClick = onSaveClick,
                    loading = isUploading,
                    state = if (isUploading) WireButtonState.Disabled else WireButtonState.Default
                )
            }
        }

        else -> {
            WirePrimaryButton(
                modifier = Modifier.padding(dimensions().spacing16x),
                text = stringResource(R.string.profile_image_change_image_button_label),
                onClick = onChangeImage
            )
        }
    }
}

@Composable
private fun AvatarPickerTopBar(onCloseClick: () -> Unit) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        title = stringResource(R.string.profile_image_top_bar_label),
    )
}

private suspend fun sanitizeAvatarImage(originalAvatarUri: Uri, avatarPath: Path, appContext: Context) {
    originalAvatarUri.resampleImageAndCopyToTempPath(appContext, avatarPath, ImageUtil.ImageSizeClass.Small)
}
