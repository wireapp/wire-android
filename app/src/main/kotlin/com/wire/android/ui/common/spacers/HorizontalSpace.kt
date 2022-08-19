package com.wire.android.ui.common.spacers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions

object HorizontalSpace {

    @Composable
    fun x8() {
        Spacer(Modifier.width(dimensions().spacing8x))
    }

    @Composable
    fun x16() {
        Spacer(Modifier.width(dimensions().spacing16x))
    }

    @Composable
    fun x32() {
        Spacer(Modifier.width(dimensions().spacing32x))
    }

}
