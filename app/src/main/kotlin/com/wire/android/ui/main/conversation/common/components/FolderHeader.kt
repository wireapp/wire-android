package com.wire.android.ui.main.conversation.common.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.wire.android.ui.theme.title3


@Composable
fun FolderHeader(name: String) {
    Text(
        text = name,
        modifier = Modifier.padding(start = 16.dp, top = 8.dp, bottom = 8.dp),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        style = MaterialTheme.typography.title3
    )
}
