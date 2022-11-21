package com.wire.android.ui.common

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.wire.android.R
import com.wire.android.ui.theme.wireColorScheme

/**
 * Outlined box with a text inside.
 * Used for things like "Deleted" users,
 * and "Deleted message" or "Edited message"
 */
@Composable
fun StatusBox(
    statusText: String,
    modifier: Modifier = Modifier
) {
    CompositionLocalProvider(
        LocalTextStyle provides MaterialTheme.typography.labelSmall
    ) {
        Box(
            modifier = modifier
                .wrapContentSize()
                .border(
                    BorderStroke(
                        width = 1.dp,
                        color = MaterialTheme.wireColorScheme.divider
                    ),
                    shape = RoundedCornerShape(size = dimensions().spacing4x)
                )
                .padding(
                    horizontal = dimensions().spacing4x,
                    vertical = dimensions().spacing2x
                )
        ) {
            Text(
                text = statusText,
                style = LocalTextStyle.current.copy(color = MaterialTheme.wireColorScheme.labelText)
            )
        }
    }
}

@Composable
fun DeletedLabel(modifier: Modifier = Modifier) {
    StatusBox(
        statusText = stringResource(id = R.string.label_user_deleted),
        modifier = modifier
    )
}

@Preview
@Composable
fun DeletedLabelPreview() {
    DeletedLabel()
}
