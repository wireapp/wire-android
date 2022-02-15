package com.wire.android.ui.userprofile.image

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.CornerBasedShape
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Scaffold
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.constraintlayout.compose.ConstraintLayout
import com.wire.android.R
import com.wire.android.ui.common.ArrowRightIcon
import com.wire.android.ui.common.bottomsheet.MenuBottomSheetItem
import com.wire.android.ui.common.bottomsheet.MenuItemIcon
import com.wire.android.ui.common.bottomsheet.MenuModalSheetLayout
import com.wire.android.ui.common.dimensions
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
            Box(modifier = Modifier.fillMaxSize()) {
                Box(Modifier.align(Alignment.Center)) {
                    ProfileImagePreview("")
                }
                Surface(
                    shadowElevation = 8.dp,
                    modifier = Modifier.align(Alignment.BottomEnd)
                ) {
                    WirePrimaryButton(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.background)
                            .padding(dimensions().spacing16x),
                        text = "Change Image...",
                        onClick = { profileImageState.showModalBottomSheet() }
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfileImagePreview(avatarUrl: String) {
    CircleShape
    Box(
        Modifier
            .fillMaxWidth()
            .height(360.dp)
    ) {
        ConstraintLayout(Modifier.matchParentSize()) {
            val (avatarImage, semiTransparentBackground) = createRefs()

            Box(
                Modifier
                    .fillMaxSize()
                    .constrainAs(avatarImage) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
            ) {
                Image(
                    painter = painterResource(id = R.drawable.mock_message_image),
                    contentScale = ContentScale.Crop,
                    contentDescription = "",
                    modifier = Modifier.fillMaxSize()
                )
            }

            CircleShape

            Box(
                Modifier
                    .fillMaxSize()
                    .graphicsLayer(shape = TestShape(), clip = true)
                    .background(Color(color = 0xFFFF0000))
                    .constrainAs(semiTransparentBackground) {
                        top.linkTo(parent.top)
                        start.linkTo(parent.start)
                        end.linkTo(parent.end)
                        bottom.linkTo(parent.bottom)
                    }
            )
        }
    }
}

class TestShape : CornerBasedShape(
    topStart = CornerSize(50),
    topEnd = CornerSize(50),
    bottomEnd = CornerSize(50),
    bottomStart = CornerSize(50)
) {

    override fun copy(
        topStart: CornerSize,
        topEnd: CornerSize,
        bottomEnd: CornerSize,
        bottomStart: CornerSize
    ) = RoundedCornerShape(
        topStart = topStart,
        topEnd = topEnd,
        bottomEnd = bottomEnd,
        bottomStart = bottomStart
    )

    override fun createOutline(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection
    ): Outline {
        return Outline.Generic(
            drawTest(size, topStart, topEnd, bottomEnd, bottomStart, layoutDirection)
        )
    }

    private fun drawTest(
        size: Size,
        topStart: Float,
        topEnd: Float,
        bottomEnd: Float,
        bottomStart: Float,
        layoutDirection: LayoutDirection,
    ): Path {
        Log.d("TEST", "drawTest")
        val path = Path().apply {
            reset()

            val rectangleRect = size.toRect()

            val roundRect = RoundRect(
                size.toRect(),
                topLeft = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) topStart else topEnd),
                topRight = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) topEnd else topStart),
                bottomRight = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) bottomEnd else bottomStart),
                bottomLeft = CornerRadius(if (layoutDirection == LayoutDirection.Ltr) bottomStart else bottomEnd)
            )

            val topLeftRect = Rect(left = -size.width, top = -size.height, right = size.width / 2, bottom = size.height / 2)
            val topRightRect = Rect(left = size.width / 2, top = -size.height, right = size.width, bottom = size.height / 2)
            val bottomLeftRect = Rect(left = -size.width, top = size.height / 2, right = size.width / 2, bottom = size.height)
            val bottomRightRect = Rect(left = size.width / 2, top = size.height / 2, right = size.width, bottom = size.height)

            arcTo(topLeftRect, startAngleDegrees = 90f, sweepAngleDegrees = -90f, false)
            arcTo(topRightRect, startAngleDegrees = 180f, sweepAngleDegrees = -90f, false)
            arcTo(bottomLeftRect, startAngleDegrees = 270f, sweepAngleDegrees = -90f, false)
            arcTo(bottomRightRect, startAngleDegrees = 0f, sweepAngleDegrees = -90f, false)

            close()
        }



        return path
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
