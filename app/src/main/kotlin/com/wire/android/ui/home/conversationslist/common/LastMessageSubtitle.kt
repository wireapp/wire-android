package com.wire.android.ui.home.conversationslist.common

import androidx.compose.material.Text
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.style.TextOverflow
import com.wire.android.ui.theme.wireColorScheme
import com.wire.android.ui.theme.wireTypography
import com.wire.android.util.ui.UIText

@Composable
fun LastMessageSubtitle(text: UIText) {
    Text(
        text = text.asString(LocalContext.current.resources),
        style = MaterialTheme.wireTypography.subline01.copy(
            color = MaterialTheme.wireColorScheme.secondaryText
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun LastMessageSubtitleWithAuthor(author: UIText, text: UIText, separator: String) {
    Text(
        text = "${author.asString(LocalContext.current.resources)}$separator${text.asString(LocalContext.current.resources)}",
        style = MaterialTheme.wireTypography.subline01.copy(
            color = MaterialTheme.wireColorScheme.secondaryText
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
fun LastMultipleMessages(messages: List<UIText>, separator: String) {
    Text(
        text = messages.map { it.asString() }.joinToString(separator = separator),
        style = MaterialTheme.wireTypography.subline01.copy(
            color = MaterialTheme.wireColorScheme.secondaryText
        ),
        maxLines = 1,
        overflow = TextOverflow.Ellipsis
    )
}
