package com.wire.android.ui.userprofile.image

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.imagepreview.ImagePreview
import com.wire.android.ui.common.textfield.WirePrimaryButton
import com.wire.android.ui.theme.wireTypography


@OptIn(ExperimentalMaterialApi::class)
@Composable
fun ProfileImageScreen(avatarUrl: String) {
    val profileImageState = rememberProfileImageState({}, {}, {})

    MenuModalSheetLayout(
        sheetState = profileImageState.modalBottomSheetState,
        headerTitle = "Change Image",
        menuItems = listOf(
            {
                MenuBottomSheetItem(
                    title = "Choose from gallery",
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_gallery,
                            contentDescription = ""
                        )
                    },
                    action = {
                        ArrowRightIcon()
                    },
                    onItemClick = { profileImageState.openGallery() }
                )
            },
            {
                MenuBottomSheetItem(
                    title = "Take a picture",
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_take_a_picture,
                            contentDescription = ""
                        )
                    },
                    action = {
                        ArrowRightIcon()
                    },
                    onItemClick = { profileImageState.openCamera() }
                )
            }
        )
    ) {
        Scaffold(topBar = { ProfileImageTopBar() }) {
            Column(Modifier.fillMaxSize()) {
                Box(Modifier.weight(1f)) {
                    Box(Modifier.align(Alignment.Center)) {
                        ImagePreview(
                            avatarUrl = "",
                            contentDescription = stringResource(R.string.content_description_avatar_preview)
                        )
                    }
                }
                WirePrimaryButton(
                    modifier = Modifier
                        .padding(dimensions().spacing16x),
                    text = "Change Image...",
                    onClick = { profileImageState.showModalBottomSheet() }
                )
            }
        }
    }
}

@Composable
private fun ProfileImageTopBar() {
    CenterAlignedTopAppBar(
        colors = TopAppBarDefaults.centerAlignedTopAppBarColors(
            containerColor = MaterialTheme.colorScheme.surface,
            titleContentColor = MaterialTheme.colorScheme.onSurface,
            actionIconContentColor = MaterialTheme.colorScheme.onSurface,
            navigationIconContentColor = MaterialTheme.colorScheme.onSurface
        ),
        navigationIcon = {
            IconButton(onClick = { }) {
                Icon(
                    imageVector = Icons.Filled.Close,
                    contentDescription = stringResource(R.string.user_profile_close_description),
                )
            }
        },
        title = {
            Text(
                text = "Profile image",
                style = MaterialTheme.wireTypography.title01,
            )
        },
    )
}
