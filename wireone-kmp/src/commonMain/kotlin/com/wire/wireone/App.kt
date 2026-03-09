package com.wire.wireone

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBars
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import com.wire.android.ui.common.divider.WireDivider
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.common.spacers.HorizontalSpace
import com.wire.android.ui.common.spacers.VerticalSpace
import com.wire.android.ui.theme.WireTheme
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun App() {
    WireTheme {
        Surface(color = MaterialTheme.wireColorScheme.background) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(WindowInsets.statusBars.asPaddingValues())
                    .padding(dimensions().spacing24x)
            ) {
                Text(
                    text = "WireOne iOS Preview",
                    style = MaterialTheme.wireTypography.title01,
                    color = MaterialTheme.wireColorScheme.onBackground
                )
                VerticalSpace.x16()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing28x)
                            .clip(CircleShape)
                            .background(MaterialTheme.wireColorScheme.primary)
                    )
                    HorizontalSpace.x12()
                    Text(
                        text = "Shared Wire theme rendered from Kotlin Multiplatform",
                        style = MaterialTheme.wireTypography.body01,
                        color = MaterialTheme.wireColorScheme.onBackground
                    )
                }
                VerticalSpace.x16()
                WireDivider()
                VerticalSpace.x16()
                Text(
                    text = "Semantic color tokens",
                    style = MaterialTheme.wireTypography.body02,
                    color = MaterialTheme.wireColorScheme.onBackground
                )
                VerticalSpace.x8()
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing14x)
                            .background(MaterialTheme.wireColorScheme.positive)
                    )
                    HorizontalSpace.x8()
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing14x)
                            .background(MaterialTheme.wireColorScheme.warning)
                    )
                    HorizontalSpace.x8()
                    Box(
                        modifier = Modifier
                            .size(dimensions().spacing14x)
                            .background(MaterialTheme.wireColorScheme.error)
                    )
                }
            }
        }
    }
}
