package com.wire.android.ui.common.button

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.wire.android.ui.common.dimensions
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun WireItemLabel(
    text: String = "",
    minHeight: Dp = dimensions().badgeSmallMinSize.height,
    minWidth: Dp = dimensions().badgeSmallMinSize.height,
    shape: Shape = RoundedCornerShape(dimensions().spacing6x),
    modifier: Modifier = Modifier
) = Box(
    modifier = modifier
        .defaultMinSize(minHeight = minHeight, minWidth = minWidth)
        .border(width = 1.dp, color = MaterialTheme.wireColorScheme.divider, shape = shape)
        .padding(PaddingValues(horizontal = dimensions().spacing6x))
        .sizeIn(minHeight = minHeight, minWidth = minWidth)
        .wrapContentWidth(),
    contentAlignment = Alignment.Center,
) {
    Text(
        text = text,
        style = MaterialTheme.wireTypography.body03,
        textAlign = TextAlign.Center,
    )
}

@Preview(name = "Wire item label", showBackground = true)
@Composable
private fun WireSecondaryButtonDisabledPreview() {
    Column(modifier = Modifier.fillMaxWidth(), horizontalAlignment = Alignment.CenterHorizontally) {
        WireItemLabel(text = "pending")
    }
}
