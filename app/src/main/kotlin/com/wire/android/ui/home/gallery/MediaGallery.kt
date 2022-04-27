package com.wire.android.ui.home.gallery

import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.wire.android.R
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.button.WireSecondaryButton
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

@OptIn(ExperimentalMaterialApi::class, ExperimentalMaterial3Api::class)
@Composable
fun MediaGalleryScreen(mediaGalleryViewModel: MediaGalleryViewModel = hiltViewModel()) {

    val uiState = mediaGalleryViewModel.mediaGalleryViewState

    val mediaGalleryScreenState = rememberMediaGalleryScreenState()
    val scope = rememberCoroutineScope()

    with(uiState) {
        MenuModalSheetLayout(
            sheetState = mediaGalleryScreenState.modalBottomSheetState,
            menuItems = EditGalleryMenuItems(onDeleteMessage = {}),
            content = {
                Scaffold(
                    topBar = {MediaGalleryScreenTopAppBar(screenTitle, {} ,{})},
                    content = {}
                )
            }
        )
    }
}

@Composable
fun MediaGalleryScreenTopAppBar(
    title: String,
    onCloseClick: () -> Unit,
    onOptionsClick: () -> Unit,
) {
    WireCenterAlignedTopAppBar(
        onNavigationPressed = onCloseClick,
        title = title,
        navigationIconType = NavigationIconType.Close,
        elevation = 0.dp,
        actions = {
            WireSecondaryButton(
                onClick = onOptionsClick,
                fillMaxWidth = false,
                minHeight = dimensions().userProfileLogoutBtnHeight,
                leadingIcon = {
                    Icon(
                        imageVector = ImageVector.vectorResource(id = R.drawable.ic_more),
                        contentDescription = stringResource(R.string.content_description_menu_button),
                    )
                }
            )
        }
    )
}

fun EditGalleryMenuItems(
    onDeleteMessage: () -> Unit
): List<@Composable () -> Unit> {
    return buildList {
        add {
            CompositionLocalProvider(LocalContentColor provides MaterialTheme.colorScheme.error) {
                MenuBottomSheetItem(
                    icon = {
                        MenuItemIcon(
                            id = R.drawable.ic_delete,
                            contentDescription = stringResource(R.string.content_description_delete_the_message),
                        )
                    },
                    title = stringResource(R.string.label_delete),
                    onItemClick = onDeleteMessage
                )
            }
        }
    }
}
