package com.wire.android.ui.conversation.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.WireColor
import com.wire.android.ui.theme.label3


@Composable
fun MembershipQualifier(label: String) {
    Text(
        text = label,
        color = WireColor.DarkBlue,
        style = MaterialTheme.typography.label3.copy(textAlign = TextAlign.Center),
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
