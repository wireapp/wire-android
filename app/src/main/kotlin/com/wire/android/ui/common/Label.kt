package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireColor
import com.wire.android.ui.theme.label3


@Composable
fun Label(
    label: String,
    backgroundShape: Shape = RoundedCornerShape(8.dp),
    modifier: Modifier
) {
    Text(
        text = label,
        color = WireColor.DarkBlue,
        style = MaterialTheme.typography.label3.copy(textAlign = TextAlign.Center),
        modifier = Modifier
            .wrapContentWidth()
            .background(
                color = WireColor.Alpha10LightBlue,
                shape = backgroundShape
            )
            .then(modifier)
    )
}
