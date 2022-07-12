package com.wire.android.ui.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.wire.android.ui.theme.wireColorScheme

@Composable
fun WireLabeledSwitch(
    switchState: Boolean,
    onCheckedChange: ((Boolean) -> Unit)?,
    text: String,
    modifier: Modifier = Modifier
        .fillMaxWidth()
        .background(MaterialTheme.colorScheme.surface)
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {

        Text(
            text = text,
            fontWeight = FontWeight.Normal,
            color = MaterialTheme.wireColorScheme.onSecondaryButtonEnabled,
            modifier = Modifier
                .padding(16.dp)
                .weight(1f),
            textAlign = TextAlign.Left,
            fontSize = 16.sp
        )

        WireSwitch(
            modifier = Modifier.padding(end = 16.dp),
            checked = switchState,
            onCheckedChange = onCheckedChange
        )
    }

}
