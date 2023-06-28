
package com.wire.android.feature

import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import com.wire.android.ui.theme.WireTheme
import com.wire.android.util.ui.PreviewMultipleThemes

@Composable
fun ComposableFeatureScreen() {
    Text("My sample text")
}

@Composable
@PreviewMultipleThemes
fun HelloPreview() {
    WireTheme {
        ComposableFeatureScreen()
    }
}
