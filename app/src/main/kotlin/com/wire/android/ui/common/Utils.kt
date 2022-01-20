package com.wire.android.ui.common

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material.ButtonColors
import androidx.compose.material.ButtonDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun WhiteBackgroundWrapper(content: @Composable () -> Unit, modifier: Modifier) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .wrapContentHeight()
            .background(color = MaterialTheme.colors.surface)
    ) {
        content()
    }
}

@Composable
fun AnimatedButtonColors(enabled: Boolean): ButtonColors {
    val animatedBackgroundColor = animateColorAsState(ButtonDefaults.buttonColors().backgroundColor(enabled).value)
    val animatedContentColor = animateColorAsState(ButtonDefaults.buttonColors().contentColor(enabled).value)
    return ButtonDefaults.buttonColors(
        backgroundColor = animatedBackgroundColor.value,
        disabledBackgroundColor = animatedBackgroundColor.value,
        contentColor = animatedContentColor.value,
        disabledContentColor = animatedContentColor.value)
}
