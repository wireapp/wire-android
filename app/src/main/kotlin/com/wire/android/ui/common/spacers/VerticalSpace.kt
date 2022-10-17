package com.wire.android.ui.common.spacers

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.dimensions

object VerticalSpace {

    @Composable
    fun x8() {
        Spacer(Modifier.height(dimensions().spacing8x))
    }

    @Composable
    fun x16() {
        Spacer(Modifier.height(dimensions().spacing16x))
    }

    @Composable
    fun x24() {
        Spacer(Modifier.height(dimensions().spacing16x))
    }
    
    @Composable
    fun x32() {
        Spacer(Modifier.height(dimensions().spacing32x))
    }

}
