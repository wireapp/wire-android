package com.wire.android.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.ButtonColors
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun SurfaceBackgroundWrapper(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = MaterialTheme.colorScheme.surface)
    ) {
        content()
    }
}

@Composable
fun AnimatedButtonColors(enabled: Boolean): ButtonColors {
    val animatedBackgroundColor = animateColorAsState(ButtonDefaults.buttonColors().containerColor(enabled).value)
    val animatedContentColor = animateColorAsState(ButtonDefaults.buttonColors().contentColor(enabled).value)
    return ButtonDefaults.buttonColors(
        containerColor = animatedBackgroundColor.value,
        disabledContainerColor = animatedBackgroundColor.value,
        contentColor = animatedContentColor.value,
        disabledContentColor = animatedContentColor.value)
}
