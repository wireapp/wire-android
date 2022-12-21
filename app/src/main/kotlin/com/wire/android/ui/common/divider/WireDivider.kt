package com.wire.android.ui.common.divider

import androidx.compose.material3.Divider
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.wire.android.ui.common.colorsScheme

@Composable
fun WireDivider() {
    Divider(color = colorsScheme().divider, modifier = Modifier)
}
