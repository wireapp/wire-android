package com.wire.android.ui.home.conversations.model.messagetypes.image

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.Dp
import com.wire.android.ui.common.dimensions

data class ImageMessageParams(private val realImgWidth: Int, private val realImgHeight: Int) {
    // Image size normalizations to keep the ratio of the inline message image
    val normalizedWidth: Dp
        @Composable
        get() = dimensions().messageImageMaxWidth

    val normalizedHeight: Dp
        @Composable
        get() = Dp(normalizedWidth.value * realImgHeight.toFloat() / realImgWidth)
}
