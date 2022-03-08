package com.wire.android.ui.userprofile.image

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
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.BackNavigationIconButton
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.imagepreview.BitmapState
import com.wire.android.ui.common.imagepreview.BulletHoleImagePreview
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireTypography
import com.wire.android.ui.userprofile.UserProfileViewModel

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
//TODO: the input data for ProfileImageScreen will be decided later on after sync with Yamil
fun AvatarPickerScreen(
    viewModel: UserProfileViewModel
) {
    val state = rememberProfileImageState(viewModel.userProfileState.avatarBitmap)

    AvatarPickerBottomSheet(
        state = state,
        onCloseClick = {
            when (val avatarImageState = state.picturePickerFlow.bitmapState) {
                is BitmapState.BitmapPicked -> viewModel.changeUserAvatar(avatarImageState.bitmap, shouldNavigateBack = true)
                is BitmapState.InitialBitmap -> viewModel.navigateBack()
            }
        }
    )
}

@OptIn(ExperimentalMaterialApi::class)
@Composable
private fun AvatarPickerBottomSheet(
    state: ImagePickerState,
    onCloseClick: () -> Unit
) {
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
                    action = {
                        ArrowRightIcon()
                    },
                    onItemClick = { state.openImageSource(ImageSource.Gallery) }
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
                    onItemClick = { state.openImageSource(ImageSource.Camera) }
                )
            }
        )
    ) {
        Scaffold(topBar = {
            AvatarPickerTopBar(
                hasPicked = (state.picturePickerFlow.bitmapState is BitmapState.BitmapPicked),
                onCloseClick = onCloseClick
            )
        }) {
            Box(Modifier.fillMaxSize()) {
                Column(Modifier.fillMaxSize()) {
                    Box(Modifier.weight(1f)) {
                        Box(Modifier.align(Alignment.Center)) {
                            BulletHoleImagePreview(
                                imageBitmap = state.picturePickerFlow.bitmapState.bitmap,
                                contentDescription = stringResource(R.string.content_description_avatar_preview)
                            )
                        }
                    }
                    Divider()
                    Spacer(Modifier.height(4.dp))
                    WirePrimaryButton(
                        modifier = Modifier.padding(dimensions().spacing16x),
                        text = stringResource(R.string.profile_image_change_image_button_label),
                        onClick = { state.showModalBottomSheet() }
                    )
                }
            }
        }
    }
}

@Composable
private fun AvatarPickerTopBar(
    hasPicked: Boolean,
    onCloseClick: () -> Unit
) {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            if (!hasPicked) {
                IconButton(onClick = onCloseClick) {
                    Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = stringResource(R.string.user_profile_close_description),
                    )
                }
            } else {
                BackNavigationIconButton(
                    onBackButtonClick = {
                        onCloseClick()
                    }
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
