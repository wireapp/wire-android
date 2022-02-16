package com.wire.android.ui.userprofile.image

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.toRect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.Density
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

class TestShape : Shape {

    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        return Outline.Generic(
            drawTest(size)
        )
    }

    private fun drawTest(size: Size): Path {
        val path = Path().apply {
            reset()

            val rectangleRect = size.toRect()

//            val topLeftRect = Rect(left = -size.width, top = -size.height, right = size.width / 2, bottom = size.height / 2)
//            val topRightRect = Rect(left = size.width / 2, top = -size.height, right = size.width, bottom = size.height / 2)
//            val bottomLeftRect = Rect(left = -size.width, top = size.height / 2, right = size.width / 2, bottom = size.height)
//            val bottomRightRect = Rect(left = size.width / 2, top = size.height / 2, right = size.width, bottom = size.height)

            val upperRect = Rect(left = 0f, top = 0f, right = size.width, bottom = size.height / 2)
            val bottomRect = Rect(left = -size.width, top = size.height / 2, right = size.width, bottom = size.height)

//            addRect(bottomRect)
//
////            top left arc
//            lineTo(topLeftRect.left, topLeftRect.top)
//            lineTo(topLeftRect.top, topLeftRect.right)
//            arcTo(rectangleRect, startAngleDegrees = 180f, sweepAngleDegrees = 90f, false)
//
//            //top right arc
//            lineTo(topRightRect.left, topRightRect.top)
//            lineTo(topLeftRect.bottom, topRightRect.top)
//            arcTo(rectangleRect, startAngleDegrees = 0f, sweepAngleDegrees = 90f, false)
//
//            //left bottom arc
//            lineTo(bottomRightRect.right, bottomRightRect.bottom)
//            lineTo(bottomRightRect.left, bottomRightRect.bottom)
//            arcTo(rectangleRect, startAngleDegrees = 0f, sweepAngleDegrees = -90f, false)
//
            //right bottom arc

//            arcTo(rectangleRect, startAngleDegrees = 180f, sweepAngleDegrees = 0f, false)
//            lineTo(x = 0f, y = upperRect.bottom)
            moveTo(x = 0f, y = upperRect.bottom)
            lineTo(x = 0f, y = 0f)
            lineTo(x = upperRect.right, y = 0f)
            lineTo(x = upperRect.right, y = upperRect.bottom)
            arcTo(rectangleRect, 0f, -180f, true)

            lineTo(x = 0f, y = bottomRect.bottom)
            lineTo(x = bottomRect.right, y = bottomRect.bottom)
            lineTo(x = bottomRect.right, y = bottomRect.top)
            arcTo(rectangleRect, 0f, 180f, true)
/*
            //middle left  of the rect
            lineTo( x= 0f, y = bottomRect.left)
            //bottom of the rect
            lineTo(x = 0f, y = bottomRect.bottom)
            //bottom right of the rect
            lineTo(x = bottomRect.right, y = bottomRect.bottom)
            //middle right of the rect
            lineTo(x = bottomRect.right,y = bottomRect.top)*/


//            lineTo()

//            lineTo(x = 0f, y = size.height / 2)
//            lineTo(x = 0f, y = 0f)


//            lineTo(topRightRect.right, rectangleRect.top)
//            lineTo(topRightRect.top, topRightRect.bottom)
//            lineTo(topLeftRect.left, topLeftRect.bottom)

//            close()
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
