package com.wire.android.ui.userprofile.avatarpicker

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.imagepreview.BulletHoleImagePreview
import com.wire.android.ui.common.snackbar.SwipeDismissSnackbarHost
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.userprofile.avatarpicker.AvatarPickerViewModel.ErrorCodes

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun AvatarPickerScreen(viewModel: AvatarPickerViewModel) {
    val targetAvatarUri = viewModel.getTemporaryTargetAvatarUri()
    val state = rememberAvatarPickerState(
        onImageSelected = { viewModel.pickNewImage(it) },
        onPictureTaken = { viewModel.postProcessAvatarImage(targetAvatarUri) },
        targetPictureFileUri = targetAvatarUri
    )

    AvatarPickerContent(
        viewModel = viewModel,
        state = state,
        onCloseClick = {
            viewModel.navigateBack()
        },
        onSaveClick = {
            viewModel.uploadNewPickedAvatarAndBack()
        }
    )
}

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun AvatarPickerContent(
    viewModel: AvatarPickerViewModel,
    state: AvatarPickerState,
    onCloseClick: () -> Unit,
    onSaveClick: () -> Unit
) {
    val snackbarHostState = remember { SnackbarHostState() }

    viewModel.errorMessageCode?.let { errorCode ->
        val errorMessage = mapErrorCodeToString(errorCode)
        LaunchedEffect(viewModel.errorMessageCode) {
            snackbarHostState.showSnackbar(errorMessage)
            viewModel.clearErrorMessage()
        }
    }

    MenuModalSheetLayout(
        sheetState = state.modalBottomSheetState,
        headerTitle = stringResource(R.string.profile_image_modal_sheet_header_title),
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
                    onItemClick = { state.openImageSource(ImageSource.Gallery) }
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
                onItemClick = { state.openImageSource(ImageSource.Camera) }
            )
        }
        )
    ) {
        Scaffold(
            topBar = { AvatarPickerTopBar(onCloseClick = onCloseClick) },
            snackbarHost = {
                SwipeDismissSnackbarHost(
                    hostState = snackbarHostState,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        ) {
            Box(Modifier.fillMaxSize()) {
                Column(
                    Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.wireColorScheme.background)
                ) {
                    Box(Modifier.weight(1f)) {
                        Box(Modifier.align(Alignment.Center)) {
                            BulletHoleImagePreview(
                                imageUri = viewModel.pictureState.avatarUri,
                                contentDescription = stringResource(R.string.content_description_avatar_preview)
                            )
                        }
                    }
                    Divider()
                    Spacer(Modifier.height(4.dp))
                    AvatarPickerActionButtons(
                        hasPickedImage = hasPickedImage(viewModel.pictureState),
                        onSaveClick = onSaveClick,
                        onCancelClick = { viewModel.loadInitialAvatarState() },
                        onChangeImage = { state.showModalBottomSheet() },
                        isUploadingImage = isUploadingImage(viewModel.pictureState)
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AvatarPickerActionButtons(
    isUploadingImage: Boolean,
    hasPickedImage: Boolean,
    onSaveClick: () -> Unit,
    onCancelClick: () -> Unit,
    onChangeImage: () -> Unit
) {
    if (hasPickedImage || isUploadingImage) {
        Row(Modifier.fillMaxWidth()) {
            WireSecondaryButton(
                modifier = Modifier
                    .padding(dimensions().spacing16x)
                    .weight(1f),
                text = stringResource(R.string.label_cancel),
                onClick = { onCancelClick() }
            )
            WirePrimaryButton(
                modifier = Modifier
                    .padding(dimensions().spacing16x)
                    .weight(1f),
                text = stringResource(R.string.label_confirm),
                onClick = { onSaveClick() },
                loading = isUploadingImage,
                state = if (isUploadingImage) WireButtonState.Disabled else WireButtonState.Default
            )
        }
    } else {
        WirePrimaryButton(
            modifier = Modifier.padding(dimensions().spacing16x),
            text = stringResource(R.string.profile_image_change_image_button_label),
            onClick = { onChangeImage() }
        )
    }
}

@Composable
private fun AvatarPickerTopBar(onCloseClick: () -> Unit) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        title = stringResource(R.string.profile_image_top_bar_label),
    )
}

@Composable
private fun mapErrorCodeToString(errorCode: ErrorCodes): String {
    return when (errorCode) {
        ErrorCodes.UploadAvatarError -> stringResource(R.string.error_uploading_user_avatar)
        ErrorCodes.NoNetworkError -> stringResource(R.string.error_no_network_message)
        // Add more future errors for a more granular error handling
        else -> stringResource(R.string.error_unknown_title)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private fun hasPickedImage(state: AvatarPickerViewModel.PictureState): Boolean = state is AvatarPickerViewModel.PictureState.Picked

@OptIn(ExperimentalMaterial3Api::class)
private fun isUploadingImage(state: AvatarPickerViewModel.PictureState): Boolean = state is AvatarPickerViewModel.PictureState.Uploading
