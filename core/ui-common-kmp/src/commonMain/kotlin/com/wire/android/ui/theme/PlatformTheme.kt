package com.wire.android.ui.theme

import androidx.compose.runtime.Composable

@Composable
expect fun platformOrientation(): Orientation

@Composable
expect fun platformSmallestWidthDp(): Int

@Composable
expect fun UpdateSystemBarIconsAppearance(useDarkIcons: Boolean)
