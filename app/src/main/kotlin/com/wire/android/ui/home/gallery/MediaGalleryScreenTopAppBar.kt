package com.wire.android.ui.home.gallery

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.common.button.WireIconButton
import com.wire.android.ui.common.topappbar.NavigationIconType
import com.wire.android.ui.common.topappbar.WireCenterAlignedTopAppBar

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
            WireIconButton(
                onButtonClicked = onOptionsClick,
                iconResource = R.drawable.ic_more,
                contentDescription = R.string.content_description_more_options
            )
        }
    )
}
