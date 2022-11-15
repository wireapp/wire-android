package com.wire.android.ui.home.conversationslist.common

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography

@Composable
fun FolderHeader(name: String, modifier: Modifier = Modifier) {
    Text(
        text = name.uppercase(),
        modifier = modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.wireTypography.title03,
        color = MaterialTheme.wireColorScheme.labelText,
    )
}
