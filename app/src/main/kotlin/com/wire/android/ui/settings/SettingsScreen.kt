package com.wire.android.ui.settings

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// the logic here is just temporary and will be updated to be added to the VM once we build the settings screen
@Composable
fun SettingsScreen() {
    Column(
        modifier = Modifier.fillMaxSize().wrapContentSize(Alignment.Center)
    ) {
        Text(
            text = "Settings is under construction",
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onBackground,
            modifier = Modifier.align(Alignment.CenterHorizontally).padding(16.dp),
            textAlign = TextAlign.Center,
            fontSize = 18.sp
        )
    }
}

@Preview(showBackground = false)
@Composable
fun SettingsScreenPreview() {
    SettingsScreen()
}
