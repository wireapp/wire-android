package com.wire.android.ui.common

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.runtime.State

/**
 * Animation to rotate a component to Right, you can use this in conjunction with a [Modifier] graphicsLayer(rotationZ)
 */
@Composable
fun animateAsStateRotationToRight(isOpen: Boolean): State<Float> {
    return animateFloatAsState(
        if (isOpen) -90f else 0f,
        animationSpec = tween(durationMillis = 500)
    )
}
