package com.wire.android.ui.userprofile.image

import android.graphics.Bitmap
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.BackNavigationIconButton
import com.wire.android.ui.common.CircularProgressIndicator
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.button.WireButtonState
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.imagepreview.ImagePreview
import com.wire.android.ui.common.imagepreview.ImagePreviewState
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography


@OptIn(ExperimentalMaterialApi::class)
@Composable
//TODO: the input data for ProfileImageScreen will be decided later on after sync with Yamil
fun ProfileImageScreen(onNavigateBack: () -> Unit, viewModel: ProfileImageViewModel = hiltViewModel()) {
    val state = viewModel.state

    // we want to navigate back once we upload the status correctly
    // TODO?: maybe refactor this if any has better idea
    if (state.uploadStatus == UploadStatus.Success) {
        LaunchedEffect(true) {
            onNavigateBack()
        }
    }

    ProfileImageContent(
        avatarBitmap = state.avatarBitmap,
        hasPicked = state.hasPickedAvatar,
        isLoading = state.isLoading,
        onProfileImagePicked = { viewModel.onAvatarPicked(it) },
        onConfirmAvatar = { viewModel.onConfirmAvatar() },
        onBackPressed = onNavigateBack
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun ProfileImageContent(
    avatarBitmap: Bitmap,
    hasPicked: Boolean,
    isLoading: Boolean,
    onProfileImagePicked: (Bitmap) -> Unit,
    onConfirmAvatar: () -> Unit,
    onBackPressed: () -> Unit
) {
    val profileImageState = rememberProfileImageState({
        onProfileImagePicked(it)
    })

    MenuModalSheetLayout(
        sheetState = profileImageState.modalBottomSheetState,
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
                    action = {
                        ArrowRightIcon()
                    },
                    onItemClick = { profileImageState.openImageSource(ImageSource.Gallery) }
                )
            },
            {
                MenuBottomSheetItem(
                    title = stringResource(R.string.profile_image_take_a_picture_menu_item),
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_take_a_picture,
                            contentDescription = stringResource(R.string.content_description_take_a_picture)
                        )
                    },
                    action = {
                        ArrowRightIcon()
                    },
                    onItemClick = { profileImageState.openImageSource(ImageSource.Camera) }
                )
            }
        )
    ) {
        Scaffold(topBar = {
            ProfileImageTopBar(
                hasPicked = hasPicked,
                onConfirmAvatar = onConfirmAvatar,
                onCloseClick = onBackPressed
            )
        }) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        Box(Modifier.align(Alignment.Center)) {
                            ImagePreview(
                                imagePreviewState = ImagePreviewState.HasData(avatarBitmap),
                                contentDescription = stringResource(R.string.content_description_avatar_preview)
                            )
                        }
                    }
                    Divider()
                    Spacer(Modifier.height(4.dp))
                    WirePrimaryButton(
                        modifier = Modifier.padding(dimensions().spacing16x),
                        text = if (hasPicked) stringResource(R.string.profile_image_change_image_button_label) else "Choose Image",
                        state = if (isLoading) WireButtonState.Disabled else WireButtonState.Default,
                        onClick = { profileImageState.showModalBottomSheet() }
                    )
                }
                if (isLoading) {
                    CircularProgressIndicator(
                        progressColor = MaterialTheme.wireColorScheme.background,
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileImageTopBar(
    hasPicked: Boolean,
    onConfirmAvatar: () -> Unit,
    onCloseClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon =
        {
            if (!hasPicked) {
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.user_profile_close_description),
                    )
                }
            } else {
                BackNavigationIconButton(
                    onBackButtonClick = onConfirmAvatar
                )
            }
        },
        title = {
            Text(
                text = stringResource(R.string.profile_image_top_bar_label),
                style = MaterialTheme.wireTypography.title01,
            )
        },
    )
}
