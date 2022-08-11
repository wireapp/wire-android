package com.wire.android.ui.common

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.wire.android.R
import com.wire.android.ui.theme.wireTypography

@Composable
fun BlockedLabel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.border(
            width = dimensions().spacing1x,
            shape = RoundedCornerShape(dimensions().spacing4x),
            color = colorsScheme().secondaryButtonDisabledOutline
        )
    ) {
        Text(
            text = stringResource(id = R.string.label_user_blocked),
            color = colorsScheme().labelText,
            style = MaterialTheme.wireTypography.label03.copy(textAlign = TextAlign.Center),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier
                .wrapContentWidth()
                .padding(horizontal = dimensions().spacing4x, vertical = dimensions().spacing2x)
        )
    }
}

@Preview
@Composable
fun BlockedLabelPreview() {
    BlockedLabelPreview()
}
