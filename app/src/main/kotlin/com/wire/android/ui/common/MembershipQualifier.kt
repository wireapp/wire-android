package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireColor


@Composable
fun MembershipQualifier(label: String) {
    Text(
        text = label,
        color = WireColor.DarkBlue,
        style = TextStyle(
            textAlign = TextAlign.Center,
            fontWeight = FontWeight.W500
        ),
        modifier = Modifier
            .wrapContentWidth()
            .background(
                color = WireColor.Alpha10LightBlue,
                shape = RoundedCornerShape(8.dp)
            )
            .padding(
                start = 4.dp,
                top = 2.dp,
                bottom = 2.dp,
                end = 4.dp
            )
    )
}
